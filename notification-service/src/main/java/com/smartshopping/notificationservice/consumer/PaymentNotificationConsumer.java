package com.smartshopping.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.notificationservice.event.PaymentProcessedEvent;

@Service
public class PaymentNotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentNotificationConsumer.class);

    @KafkaListener(
            topics = "payment-processed",
            groupId = "notification-payment-group"
    )
    public void consumePaymentProcessedEvent(
            PaymentProcessedEvent event) {

        log.info(
                "Payment processed event received. "
                        + "Event ID: {}, "
                        + "Payment ID: {}, "
                        + "Order ID: {}, "
                        + "Amount: {}, "
                        + "Status: {}",
                event.getEventId(),
                event.getPaymentId(),
                event.getOrderId(),
                event.getAmount(),
                event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {

            log.info(
                    "Payment successful notification for Order ID: {}",
                    event.getOrderId());
        }
    }
}