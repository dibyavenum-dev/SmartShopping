package com.smartshopping.orderservice.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.event.OrderCreatedEvent;

@Service
public class OrderEventProducer {

    private static final String TOPIC = "order-created";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderEventProducer(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {

        kafkaTemplate.send(
                "order-created",
                event.getOrderId().toString(),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                System.out.println("Failed to send order event: "
                        + exception.getMessage());
            } else {

            	System.out.println("===== KAFKA EVENT SENT =====");
            	System.out.println("Order ID: " + event.getOrderId());
            	System.out.println("Order Key: " + event.getOrderId());
            	System.out.println("Topic: "
            	        + result.getRecordMetadata().topic());
            	System.out.println("Partition: "
            	        + result.getRecordMetadata().partition());
            	System.out.println("Offset: "
            	        + result.getRecordMetadata().offset());
            	System.out.println("============================");
            }
        });
    }
    public void testSameKey() {

        String key = "ORDER-100";

        for (int i = 1; i <= 3; i++) {

            OrderCreatedEvent event = new OrderCreatedEvent();

            event.setOrderId((long) i);
            event.setProductId(1L);
            event.setQuantity(i);
            event.setTotalPrice(3999.0 * i);

            int eventNumber = i;

            kafkaTemplate.send(
                    "order-created",
                    key,
                    event
            ).whenComplete((result, exception) -> {

                if (exception != null) {
                    System.out.println(
                            "Failed: " + exception.getMessage());
                } else {

                    System.out.println(
                            "Event " + eventNumber +
                            " | Key: " + key +
                            " | Partition: " +
                            result.getRecordMetadata().partition() +
                            " | Offset: " +
                            result.getRecordMetadata().offset()
                    );
                }
            });
        }
    }
}