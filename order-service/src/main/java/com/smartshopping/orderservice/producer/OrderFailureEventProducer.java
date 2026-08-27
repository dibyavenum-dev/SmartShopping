package com.smartshopping.orderservice.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.event.OrderPaymentFailedEvent;

@Service
public class OrderFailureEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderFailureEventProducer.class);

    private static final String TOPIC =
            "order-payment-failed";

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderFailureEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderPaymentFailedEvent(
            OrderPaymentFailedEvent event) {

        String correlationId =
                MDC.get(CORRELATION_ID);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        TOPIC,
                        event.getOrderId().toString(),
                        event);

        if (correlationId != null &&
                !correlationId.isBlank()) {

            record.headers().add(
                    CORRELATION_ID,
                    correlationId.getBytes());
        }

        kafkaTemplate.send(record)
                .whenComplete((result, exception) -> {

                    if (exception != null) {

                        log.error(
                                "Failed to send order payment failed event. "
                                        + "Order ID: {}",
                                event.getOrderId(),
                                exception);

                    } else {

                        log.info(
                                "Order payment failed event sent successfully. "
                                        + "Correlation ID: {}, "
                                        + "Order ID: {}, "
                                        + "Product ID: {}, "
                                        + "Quantity: {}, "
                                        + "Reason: {}, "
                                        + "Topic: {}, "
                                        + "Partition: {}, "
                                        + "Offset: {}",
                                correlationId,
                                event.getOrderId(),
                                event.getProductId(),
                                event.getQuantity(),
                                event.getReason(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}