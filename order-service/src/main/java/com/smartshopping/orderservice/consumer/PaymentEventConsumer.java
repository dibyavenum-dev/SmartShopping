package com.smartshopping.orderservice.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.OrderPaymentFailedEvent;
import com.smartshopping.orderservice.event.PaymentProcessedEvent;
import com.smartshopping.orderservice.producer.OrderFailureEventProducer;
import com.smartshopping.orderservice.repository.OrderRepository;

@Service
public class PaymentEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentEventConsumer.class);

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final OrderRepository orderRepository;
    private final OrderFailureEventProducer orderFailureEventProducer;

    public PaymentEventConsumer(
            OrderRepository orderRepository,
            OrderFailureEventProducer orderFailureEventProducer) {

        this.orderRepository = orderRepository;
        this.orderFailureEventProducer = orderFailureEventProducer;
    }

    @KafkaListener(
            topics = "payment-processed",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consumePaymentProcessedEvent(
            PaymentProcessedEvent event,
            ConsumerRecord<String, PaymentProcessedEvent> record) {

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
                    "Payment event received. "
                            + "Correlation ID: {}, "
                            + "Event ID: {}, "
                            + "Payment ID: {}, "
                            + "Order ID: {}, "
                            + "Amount: {}, "
                            + "Status: {}",
                    correlationId,
                    event.getEventId(),
                    event.getPaymentId(),
                    event.getOrderId(),
                    event.getAmount(),
                    event.getStatus());

            // =====================================================
            // PAYMENT SUCCESS
            // =====================================================

            if ("SUCCESS".equals(event.getStatus())) {

                Order order =
                        orderRepository.findById(
                                event.getOrderId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: "
                                                + event.getOrderId()));

                if ("FAILED".equals(order.getStatus())) {

                    log.info(
                            "Order {} is already FAILED. "
                                    + "Payment status update skipped.",
                            event.getOrderId());

                } else if ("PAYMENT_FAILED".equals(
                        order.getStatus())) {

                    log.info(
                            "Order {} is already PAYMENT_FAILED. "
                                    + "Payment success update skipped.",
                            event.getOrderId());

                } else if ("CANCELLED".equals(
                        order.getStatus())) {

                    log.info(
                            "Order {} is already CANCELLED. "
                                    + "Payment success update skipped.",
                            event.getOrderId());

                } else {

                    order.setStatus("PAID");

                    orderRepository.save(order);

                    log.info(
                            "Order {} status updated to PAID",
                            event.getOrderId());
                }

            // =====================================================
            // PAYMENT FAILED
            // =====================================================

            } else if ("FAILED".equals(event.getStatus())) {

                Order order =
                        orderRepository.findById(
                                event.getOrderId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order not found: "
                                                + event.getOrderId()));

                if ("FAILED".equals(order.getStatus())) {

                    log.info(
                            "Order {} is already FAILED. "
                                    + "Payment failure update skipped.",
                            event.getOrderId());

                } else if ("CANCELLED".equals(
                        order.getStatus())) {

                    log.info(
                            "Order {} is already CANCELLED. "
                                    + "Payment failure update skipped.",
                            event.getOrderId());

                } else if ("PAID".equals(
                        order.getStatus())) {

                    log.info(
                            "Order {} is already PAID. "
                                    + "Payment failure update skipped.",
                            event.getOrderId());

                } else {

                    order.setStatus("PAYMENT_FAILED");

                    Order savedOrder =
                            orderRepository.save(order);

                    OrderPaymentFailedEvent failedEvent =
                            new OrderPaymentFailedEvent(
                                    savedOrder.getId(),
                                    savedOrder.getProductId(),
                                    savedOrder.getQuantity(),
                                    "Payment failed");

                    orderFailureEventProducer
                            .sendOrderPaymentFailedEvent(
                                    failedEvent);

                    log.info(
                            "Order {} status updated to PAYMENT_FAILED",
                            event.getOrderId());
                }
            }

        } finally {

            MDC.remove(CORRELATION_ID);
        }
    }
}