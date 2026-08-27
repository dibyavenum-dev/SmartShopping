package com.smartshopping.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

import com.smartshopping.notificationservice.entity.ProcessedEvent;
import com.smartshopping.notificationservice.event.OrderCreatedEvent;
import com.smartshopping.notificationservice.repository.ProcessedEventRepository;

@Service
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;

    public OrderEventConsumer(
            ProcessedEventRepository processedEventRepository) {

        this.processedEventRepository =
                processedEventRepository;
    }

    @RetryableTopic(
            attempts = "3",
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "order-created",
            groupId = "notification-group"
    )
    public void consumeOrderCreatedEvent(
            OrderCreatedEvent event) {

        log.info(
                "Order created event received. "
                        + "Event ID: {}, Order ID: {}",
                event.getEventId(),
                event.getOrderId());

        // Check duplicate event
        if (processedEventRepository
                .existsById(event.getEventId())) {

            log.info(
                    "Duplicate event ignored: {}",
                    event.getEventId());

            return;
        }

        // Process event
        log.info(
                "Processing notification for Order ID: {}",
                event.getOrderId());

        // Mark event as processed
        ProcessedEvent processedEvent =
                new ProcessedEvent(
                        event.getEventId(),
                        java.time.LocalDateTime
                                .now()
                                .toString());

        processedEventRepository.save(
                processedEvent);

        log.info(
                "Event processed successfully. "
                        + "Event ID: {}",
                event.getEventId());
    }
}