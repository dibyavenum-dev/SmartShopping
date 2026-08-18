package com.smartshopping.notificationservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.notificationservice.event.PaymentProcessedEvent;

@Service
public class PaymentNotificationConsumer {

    @KafkaListener(
            topics = "payment-processed",
            groupId = "notification-payment-group"
    )
    public void consumePaymentProcessedEvent(
            PaymentProcessedEvent event) {

        System.out.println("===== NOTIFICATION SERVICE =====");

        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Payment ID: " + event.getPaymentId());
        System.out.println("Order ID: " + event.getOrderId());
        System.out.println("Amount: " + event.getAmount());
        System.out.println("Status: " + event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {

            System.out.println(
                    "🔔 Notification: Payment successful for Order "
                    + event.getOrderId());
        }

        System.out.println("================================");
    }
}