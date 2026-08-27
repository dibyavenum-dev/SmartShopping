package com.smartshopping.inventoryservice.producer;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.event.InventoryFailedEvent;

@Service
public class InventoryEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InventoryEventProducer.class);

    private static final String TOPIC =
            "inventory-failed";

    private static final String CORRELATION_ID =
            "X-Correlation-Id";

    private final KafkaTemplate<String, InventoryFailedEvent> kafkaTemplate;

    public InventoryEventProducer(
            KafkaTemplate<String, InventoryFailedEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendInventoryFailedEvent(
            InventoryFailedEvent event) {

        String correlationId =
                MDC.get(CORRELATION_ID);

        ProducerRecord<String, InventoryFailedEvent> record =
                new ProducerRecord<>(
                        TOPIC,
                        event.getOrderId().toString(),
                        event);

        if (correlationId != null &&
                !correlationId.isBlank()) {

            record.headers().add(
                    CORRELATION_ID,
                    correlationId.getBytes(
                            StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record)
                .whenComplete((result, exception) -> {

                    if (exception != null) {

                        log.error(
                                "Failed to send inventory failed event. "
                                        + "Order ID: {}",
                                event.getOrderId(),
                                exception);

                    } else {

                        log.info(
                                "Inventory failed event sent successfully. "
                                        + "Correlation ID: {}, "
                                        + "Order ID: {}, "
                                        + "Topic: {}, "
                                        + "Partition: {}, "
                                        + "Offset: {}",
                                correlationId,
                                event.getOrderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}