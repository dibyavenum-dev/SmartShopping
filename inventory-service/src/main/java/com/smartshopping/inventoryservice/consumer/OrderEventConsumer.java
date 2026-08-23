package com.smartshopping.inventoryservice.consumer;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.event.OrderCreatedEvent;
import com.smartshopping.inventoryservice.service.InventoryService;

@Service
public class OrderEventConsumer {
	private final InventoryService inventoryService;

	public OrderEventConsumer(
	        InventoryService inventoryService) {

	    this.inventoryService = inventoryService;
	}
	@RetryableTopic(
	        attempts = "3",
	        dltTopicSuffix = ".DLT"
	)
	@KafkaListener(
	        topics = "order-created",
	        groupId = "inventory-group"
	)
	public void consumeOrderCreatedEvent(
	        OrderCreatedEvent event) {

    	inventoryService.reserveStock(
    	        event.getProductId(),
    	        event.getQuantity());

    	System.out.println(
    	        "Stock reserved successfully for Product ID: "
    	                + event.getProductId());
    }
	
	@DltHandler
	public void handleDlt(OrderCreatedEvent event) {

	    System.err.println("===== INVENTORY DLT =====");
	    System.err.println(
	            "Failed Order ID: " + event.getOrderId());
	    System.err.println(
	            "Product ID: " + event.getProductId());
	    System.err.println(
	            "Quantity: " + event.getQuantity());
	    System.err.println("========================");
	}
}