package com.smartshopping.notificationservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

import com.smartshopping.notificationservice.entity.ProcessedEvent;
import com.smartshopping.notificationservice.event.OrderCreatedEvent;
import com.smartshopping.notificationservice.repository.ProcessedEventRepository;

@Service
public class OrderEventConsumer {
	
	private final ProcessedEventRepository processedEventRepository;

	public OrderEventConsumer(
	        ProcessedEventRepository processedEventRepository) {
	    this.processedEventRepository = processedEventRepository;
	}
	@RetryableTopic(
	        attempts = "3",
	        dltTopicSuffix = ".DLT"
	)
	@KafkaListener(
	        topics = "order-created",
	        groupId = "notification-group"
	)
	public void consumeOrderCreatedEvent(OrderCreatedEvent event) {

	    System.out.println("===== ORDER CREATED EVENT =====");
	    System.out.println("Event ID: " + event.getEventId());
	    System.out.println("Order ID: " + event.getOrderId());

	    // Check duplicate event
	    if (processedEventRepository.existsById(event.getEventId())) {

	        System.out.println(
	                "Duplicate event ignored: "
	                + event.getEventId());

	        return;
	    }

	    // Process event
	    System.out.println("Processing notification...");

	    // Mark event as processed
	    ProcessedEvent processedEvent =
	            new ProcessedEvent(
	                    event.getEventId(),
	                    java.time.LocalDateTime.now().toString()
	            );

	    processedEventRepository.save(processedEvent);

	    System.out.println("Event processed successfully");
	}
}