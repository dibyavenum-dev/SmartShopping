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
	        containerFactory = "paymentKafkaListenerContainerFactory"
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
	                                "Order not found: "
	                                        + event.getOrderId()));

	        if ("FAILED".equals(order.getStatus())) {

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " is already FAILED. "
	                    + "Payment status update skipped.");

	        } else if ("PAYMENT_FAILED".equals(order.getStatus())) {

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " is already PAYMENT_FAILED. "
	                    + "Payment success update skipped.");

	        } else if ("CANCELLED".equals(order.getStatus())) {

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " is already CANCELLED. "
	                    + "Payment success update skipped.");

	        } else {

	            order.setStatus("PAID");

	            orderRepository.save(order);

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " status updated to PAID");
	        }

	    } else if ("FAILED".equals(event.getStatus())) {

	        Order order = orderRepository.findById(event.getOrderId())
	                .orElseThrow(() ->
	                        new RuntimeException(
	                                "Order not found: "
	                                        + event.getOrderId()));

	        if ("FAILED".equals(order.getStatus())) {

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " is already FAILED. "
	                    + "Payment failure update skipped.");

	        } else if ("CANCELLED".equals(order.getStatus())) {

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " is already CANCELLED. "
	                    + "Payment failure update skipped.");

	        } else if ("PAID".equals(order.getStatus())) {

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " is already PAID. "
	                    + "Payment failure update skipped.");

	        } else {

	            order.setStatus("PAYMENT_FAILED");

	            orderRepository.save(order);

	            System.out.println(
	                    "Order " + event.getOrderId()
	                    + " status updated to PAYMENT_FAILED");
	        }
	    }
	}
}