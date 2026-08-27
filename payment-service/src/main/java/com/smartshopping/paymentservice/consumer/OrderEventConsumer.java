package com.smartshopping.paymentservice.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

import com.smartshopping.paymentservice.event.OrderCreatedEvent;

@Service
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    @RetryableTopic(
            attempts = "3",
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "order-created",
            groupId = "payment-group"
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
                            + "Event ID: {}, "
                            + "Order ID: {}, "
                            + "Amount: {}",
                    correlationId,
                    event.getEventId(),
                    event.getOrderId(),
                    event.getTotalPrice());

            log.info(
                    "Order received. Waiting for payment request.");

        } finally {

            MDC.remove(CORRELATION_ID);
        }
    }

    @DltHandler
    public void handleDlt(
            OrderCreatedEvent event) {

        log.error(
                "Payment DLT received. "
                        + "Failed Order ID: {}, "
                        + "Event ID: {}",
                event.getOrderId(),
                event.getEventId());
    }
}