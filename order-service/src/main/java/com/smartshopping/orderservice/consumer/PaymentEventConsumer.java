package com.smartshopping.orderservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.PaymentProcessedEvent;
import com.smartshopping.orderservice.repository.OrderRepository;

@Service
public class PaymentEventConsumer {
	private final OrderRepository orderRepository;

	public PaymentEventConsumer(OrderRepository orderRepository) {
	    this.orderRepository = orderRepository;
	}
	@KafkaListener(
	        topics = "payment-processed",
	        groupId = "order-payment-group"
	)
	public void consumePaymentProcessedEvent(
	        PaymentProcessedEvent event) {

	    System.out.println("🔥 PAYMENT EVENT RECEIVED");

	    System.out.println("===== ORDER PAYMENT EVENT =====");
	    System.out.println("Event ID: " + event.getEventId());
	    System.out.println("Payment ID: " + event.getPaymentId());
	    System.out.println("Order ID: " + event.getOrderId());
	    System.out.println("Amount: " + event.getAmount());
	    System.out.println("Status: " + event.getStatus());
	    System.out.println("===============================");
	    if ("SUCCESS".equals(event.getStatus())) {

	        Order order = orderRepository.findById(event.getOrderId())
	                .orElseThrow(() ->
	                        new RuntimeException(
	                                "Order not found: " + event.getOrderId()));

	        order.setStatus("PAID");

	        orderRepository.save(order);

	        System.out.println(
	                "Order " + event.getOrderId()
	                + " status updated to PAID");
	    }
	}
}