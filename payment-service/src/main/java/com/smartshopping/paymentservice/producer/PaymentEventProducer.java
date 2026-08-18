package com.smartshopping.paymentservice.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartshopping.paymentservice.event.PaymentProcessedEvent;

@Service
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentProcessedEvent(
            PaymentProcessedEvent event) {

        kafkaTemplate.send(
                "payment-processed",
                event.getOrderId().toString(),
                event
        );

        System.out.println(
                "Payment event published for Order ID: "
                + event.getOrderId());
    }
}