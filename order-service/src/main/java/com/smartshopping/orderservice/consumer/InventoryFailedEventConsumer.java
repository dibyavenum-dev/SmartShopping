package com.smartshopping.orderservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.InventoryFailedEvent;
import com.smartshopping.orderservice.repository.OrderRepository;

@Service
public class InventoryFailedEventConsumer {

    private final OrderRepository orderRepository;

    public InventoryFailedEventConsumer(
            OrderRepository orderRepository) {

        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "inventory-failed",
            containerFactory = "inventoryFailedKafkaListenerContainerFactory"
    )
    public void consumeInventoryFailedEvent(
            InventoryFailedEvent event) {

        Order order =
                orderRepository.findById(event.getOrderId())
                        .orElse(null);

        if (order == null) {

            System.err.println(
                    "Order not found: "
                            + event.getOrderId());

            return;
        }

        if (!"FAILED".equals(order.getStatus())) {

            order.setStatus("FAILED");

            orderRepository.save(order);

            System.out.println(
                    "Order marked as FAILED. Order ID: "
                            + event.getOrderId());

            System.out.println(
                    "Reason: " + event.getReason());

        } else {

            System.out.println(
                    "Order " + event.getOrderId()
                            + " is already FAILED. "
                            + "Duplicate inventory failure event ignored.");
        }
    }
}