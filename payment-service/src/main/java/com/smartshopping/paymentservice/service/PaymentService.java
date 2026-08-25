package com.smartshopping.paymentservice.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smartshopping.paymentservice.entity.Payment;
import com.smartshopping.paymentservice.event.PaymentProcessedEvent;
import com.smartshopping.paymentservice.exception.PaymentNotFoundException;
import com.smartshopping.paymentservice.producer.PaymentEventProducer;
import com.smartshopping.paymentservice.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    
    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventProducer paymentEventProducer) {

        this.paymentRepository = paymentRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    public Payment createPayment(
            Long orderId,
            Double amount,
            String paymentMethod,
            String status) {

        Payment payment = new Payment();

        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);

        payment.setStatus(
                status == null ? "SUCCESS" : status);

        Payment savedPayment =
                paymentRepository.save(payment);

        PaymentProcessedEvent event =
                new PaymentProcessedEvent(
                        UUID.randomUUID().toString(),
                        savedPayment.getId(),
                        savedPayment.getOrderId(),
                        savedPayment.getAmount(),
                        savedPayment.getStatus());

        paymentEventProducer.sendPaymentProcessedEvent(event);

        return savedPayment;
    }
    
    public Payment getPaymentById(Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found: " + id));
    }
}