package com.smartshopping.inventoryservice.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.event.InventoryFailedEvent;

@Service
public class InventoryEventProducer {

    private final KafkaTemplate<String, InventoryFailedEvent> kafkaTemplate;

    public InventoryEventProducer(
            KafkaTemplate<String, InventoryFailedEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendInventoryFailedEvent(
            InventoryFailedEvent event) {

        kafkaTemplate.send(
                "inventory-failed",
                event.getOrderId().toString(),
                event);
    }
}