package com.smartshopping.paymentservice.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.smartshopping.paymentservice.entity.Payment;
import com.smartshopping.paymentservice.event.PaymentProcessedEvent;
import com.smartshopping.paymentservice.exception.PaymentNotFoundException;
import com.smartshopping.paymentservice.producer.PaymentEventProducer;
import com.smartshopping.paymentservice.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

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

        String correlationId =
                MDC.get(CORRELATION_ID);

        log.info(
                "Payment created successfully. "
                        + "Payment ID: {}, "
                        + "Order ID: {}, "
                        + "Amount: {}, "
                        + "Status: {}, "
                        + "Correlation ID: {}",
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getAmount(),
                savedPayment.getStatus(),
                correlationId);

        PaymentProcessedEvent event =
                new PaymentProcessedEvent(
                        UUID.randomUUID().toString(),
                        savedPayment.getId(),
                        savedPayment.getOrderId(),
                        savedPayment.getAmount(),
                        savedPayment.getStatus());

        paymentEventProducer.sendPaymentProcessedEvent(
                event,
                correlationId);

        log.info(
                "Payment processed event published. "
                        + "Payment ID: {}, Order ID: {}",
                savedPayment.getId(),
                savedPayment.getOrderId());

        return savedPayment;
    }

    public Payment getPaymentById(Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found: " + id));
    }
}