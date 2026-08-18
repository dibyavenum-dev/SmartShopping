package com.smartshopping.paymentservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.paymentservice.entity.Payment;
import com.smartshopping.paymentservice.event.OrderCreatedEvent;
import com.smartshopping.paymentservice.event.PaymentProcessedEvent;
import com.smartshopping.paymentservice.producer.PaymentEventProducer;
import com.smartshopping.paymentservice.repository.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;

@Service
public class OrderEventConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    public OrderEventConsumer(
            PaymentRepository paymentRepository,
            PaymentEventProducer paymentEventProducer) {

        this.paymentRepository = paymentRepository;
        this.paymentEventProducer = paymentEventProducer;
    }
    @RetryableTopic(
            attempts = "3",
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "order-created",
            groupId = "payment-group"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {

        System.out.println("===== PAYMENT SERVICE =====");

        System.out.println("Event ID: " + event.getEventId());
        System.out.println("Order ID: " + event.getOrderId());
        System.out.println("Amount: " + event.getTotalPrice());

        // Check duplicate payment
        Optional<Payment> existingPayment =
                paymentRepository.findByOrderId(event.getOrderId());

        if (existingPayment.isPresent()) {

            System.out.println(
                    "Payment already exists for Order ID: "
                            + event.getOrderId());

            return;
        }

        // Create payment
        Payment payment = new Payment();

        payment.setOrderId(event.getOrderId());
        payment.setAmount(event.getTotalPrice());
        payment.setPaymentMethod("UPI");
        payment.setStatus("SUCCESS");

        Payment savedPayment = paymentRepository.save(payment);

        System.out.println(
                "Payment created successfully: "
                        + savedPayment.getId());

        // Create payment event
        PaymentProcessedEvent paymentEvent =
                new PaymentProcessedEvent(
                        UUID.randomUUID().toString(),
                        savedPayment.getId(),
                        savedPayment.getOrderId(),
                        savedPayment.getAmount(),
                        savedPayment.getStatus()
                );

        paymentEventProducer.sendPaymentProcessedEvent(paymentEvent);

        System.out.println("===========================");
    }
    
    @DltHandler
    public void handleDlt(OrderCreatedEvent event) {

        System.err.println("===== PAYMENT DLT =====");

        System.err.println(
                "Failed Order ID: " + event.getOrderId());

        System.err.println(
                "Event ID: " + event.getEventId());

        System.err.println("=======================");
    }
}