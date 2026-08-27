package com.smartshopping.inventoryservice.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.event.OrderPaymentFailedEvent;
import com.smartshopping.inventoryservice.service.InventoryService;

@Service
public class OrderPaymentFailedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderPaymentFailedConsumer.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final InventoryService inventoryService;

    public OrderPaymentFailedConsumer(
            InventoryService inventoryService) {

        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = "order-payment-failed",
            containerFactory =
                    "orderPaymentFailedKafkaListenerContainerFactory"
    )
    public void consumeOrderPaymentFailedEvent(
            OrderPaymentFailedEvent event,
            ConsumerRecord<String, OrderPaymentFailedEvent> record) {

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
                    "Order payment failed event received. "
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

            inventoryService.releaseStock(
                    event.getProductId(),
                    event.getQuantity());

            log.info(
                    "Stock released successfully. "
                            + "Product ID: {}, Quantity: {}",
                    event.getProductId(),
                    event.getQuantity());

        } finally {

            MDC.remove(CORRELATION_ID);
        }
    }
}