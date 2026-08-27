package com.smartshopping.orderservice.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.event.OrderCreatedEvent;

@Service
public class OrderEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventProducer.class);

    private static final String TOPIC =
            "order-created";

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(
            OrderCreatedEvent event) {

        String correlationId =
                MDC.get(CORRELATION_ID);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        TOPIC,
                        event.getOrderId().toString(),
                        event);

        if (correlationId != null) {

            record.headers().add(
                    CORRELATION_ID,
                    correlationId.getBytes());
        }

        kafkaTemplate.send(record)
                .whenComplete((result, exception) -> {

                    if (exception != null) {

                        log.error(
                                "Failed to send order event for Order ID: {}",
                                event.getOrderId(),
                                exception);

                    } else {

                        log.info(
                                "Kafka event sent successfully. "
                                + "Correlation ID: {}, "
                                + "Order ID: {}, "
                                + "Order Key: {}, "
                                + "Topic: {}, "
                                + "Partition: {}, "
                                + "Offset: {}",
                                correlationId,
                                event.getOrderId(),
                                event.getOrderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void testSameKey() {

        String key = "ORDER-100";

        for (int i = 1; i <= 3; i++) {

            OrderCreatedEvent event =
                    new OrderCreatedEvent();

            event.setOrderId((long) i);
            event.setProductId(1L);
            event.setQuantity(i);
            event.setTotalPrice(3999.0 * i);

            int eventNumber = i;

            kafkaTemplate.send(
                    TOPIC,
                    key,
                    event)
                    .whenComplete((result, exception) -> {

                        if (exception != null) {

                            log.error(
                                    "Failed to send test event. "
                                            + "Event Number: {}, Key: {}",
                                    eventNumber,
                                    key,
                                    exception);

                        } else {

                            log.info(
                                    "Test event sent. "
                                            + "Event Number: {}, "
                                            + "Key: {}, "
                                            + "Partition: {}, "
                                            + "Offset: {}",
                                    eventNumber,
                                    key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        }
    }
}