package com.smartshopping.orderservice.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.InventoryFailedEvent;
import com.smartshopping.orderservice.repository.OrderRepository;

@Service
public class InventoryFailedEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InventoryFailedEventConsumer.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final OrderRepository orderRepository;

    public InventoryFailedEventConsumer(
            OrderRepository orderRepository) {

        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "inventory-failed",
            containerFactory =
                    "inventoryFailedKafkaListenerContainerFactory"
    )
    public void consumeInventoryFailedEvent(
            ConsumerRecord<String, InventoryFailedEvent> record) {

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

            InventoryFailedEvent event =
                    record.value();

            log.info(
                    "Inventory failed event received. "
                            + "Correlation ID: {}, "
                            + "Order ID: {}, "
                            + "Product ID: {}, "
                            + "Quantity: {}, "
                            + "Reason: {}",
                    correlationId,
                    event.getOrderId(),
                    event.getProductId(),
                    event.getQuantity(),
                    event.getReason());

            Order order =
                    orderRepository.findById(
                            event.getOrderId())
                    .orElse(null);

            if (order == null) {

                log.error(
                        "Order not found: {}",
                        event.getOrderId());

                return;
            }

            if (!"FAILED".equals(order.getStatus())) {

                order.setStatus("FAILED");

                orderRepository.save(order);

                log.info(
                        "Order marked as FAILED. Order ID: {}",
                        event.getOrderId());

                log.info(
                        "Reason: {}",
                        event.getReason());

            } else {

                log.info(
                        "Order {} is already FAILED. "
                                + "Duplicate inventory failure event ignored.",
                        event.getOrderId());
            }

        } finally {

            MDC.remove(CORRELATION_ID);
        }
    }
}