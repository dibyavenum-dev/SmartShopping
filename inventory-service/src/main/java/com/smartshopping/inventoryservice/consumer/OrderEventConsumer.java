package com.smartshopping.inventoryservice.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.event.InventoryFailedEvent;
import com.smartshopping.inventoryservice.event.OrderCreatedEvent;
import com.smartshopping.inventoryservice.exception.InsufficientStockException;
import com.smartshopping.inventoryservice.producer.InventoryEventProducer;
import com.smartshopping.inventoryservice.service.InventoryService;

@Service
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    public OrderEventConsumer(
            InventoryService inventoryService,
            InventoryEventProducer inventoryEventProducer) {

        this.inventoryService = inventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(
            topics = "order-created",
            groupId = "inventory-group"
    )
    public void consumeOrderCreatedEvent(
            OrderCreatedEvent event,
            ConsumerRecord<String, OrderCreatedEvent> record) {

        String correlationId = null;

        if (record.headers().lastHeader(CORRELATION_ID) != null) {

            correlationId = new String(
                    record.headers()
                            .lastHeader(CORRELATION_ID)
                            .value(),
                    StandardCharsets.UTF_8);
        }

        try {

            MDC.put(
                    CORRELATION_ID,
                    correlationId != null
                            ? correlationId
                            : "UNKNOWN");

            log.info(
                    "Order created event received. "
                            + "Correlation ID: {}, "
                            + "Order ID: {}, "
                            + "Product ID: {}, "
                            + "Quantity: {}",
                    correlationId,
                    event.getOrderId(),
                    event.getProductId(),
                    event.getQuantity());

            try {

                inventoryService.reserveStock(
                        event.getProductId(),
                        event.getQuantity());

                log.info(
                        "Stock reserved successfully. "
                                + "Product ID: {}, Quantity: {}",
                        event.getProductId(),
                        event.getQuantity());

            } catch (InsufficientStockException e) {

                log.warn(
                        "Inventory reservation failed. "
                                + "Order ID: {}, Product ID: {}, "
                                + "Quantity: {}, Reason: {}",
                        event.getOrderId(),
                        event.getProductId(),
                        event.getQuantity(),
                        e.getMessage());

                InventoryFailedEvent failureEvent =
                        new InventoryFailedEvent(
                                event.getOrderId(),
                                event.getProductId(),
                                event.getQuantity(),
                                e.getMessage());

                inventoryEventProducer
                        .sendInventoryFailedEvent(
                                failureEvent);

                log.info(
                        "Inventory failed event published. "
                                + "Order ID: {}",
                        event.getOrderId());
            }

        } finally {

            MDC.remove(CORRELATION_ID);
        }
    }

    @DltHandler
    public void handleDlt(
            OrderCreatedEvent event) {

        log.error(
                "Inventory DLT received. "
                        + "Order ID: {}, Product ID: {}, Quantity: {}",
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity());
    }
}