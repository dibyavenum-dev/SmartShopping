package com.smartshopping.inventoryservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.event.OrderCreatedEvent;

@Service
public class OrderEventConsumer {

    @KafkaListener(
            topics = "order-created",
            groupId = "inventory-group"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {

        System.out.println("===== INVENTORY EVENT =====");
        System.out.println("Order ID: " + event.getOrderId());
        System.out.println("Product ID: " + event.getProductId());
        System.out.println("Quantity: " + event.getQuantity());
        System.out.println("==========================");
    }
}