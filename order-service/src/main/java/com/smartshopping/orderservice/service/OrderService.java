package com.smartshopping.orderservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.smartshopping.orderservice.dto.ProductResponse;
import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.OrderCreatedEvent;
import com.smartshopping.orderservice.exception.OrderCancellationException;
import com.smartshopping.orderservice.exception.OrderNotFoundException;
import com.smartshopping.orderservice.exception.ProductNotFoundException;
import com.smartshopping.orderservice.producer.OrderEventProducer;
import com.smartshopping.orderservice.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderEventProducer orderEventProducer;
    
    public OrderService(OrderRepository orderRepository,
                        RestTemplate restTemplate,
                        OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.orderEventProducer = orderEventProducer;
    }

    public Order createOrder(Order order) {

        ProductResponse product;

        try {

            product = restTemplate.getForObject(
                    "http://PRODUCT-SERVICE/products/"
                            + order.getProductId(),
                    ProductResponse.class
            );

        } catch (HttpClientErrorException.NotFound e) {

            throw new ProductNotFoundException(
                    "Product not found: "
                    + order.getProductId());
        }

        if (product == null) {

            throw new ProductNotFoundException(
                    "Product not found: "
                    + order.getProductId());
        }

        double totalPrice =
                product.getPrice() * order.getQuantity();

        order.setTotalPrice(totalPrice);
        order.setStatus("CREATED");

        // Save order
        Order savedOrder =
                orderRepository.save(order);

        // Create Kafka event
        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        savedOrder.getId(),
                        savedOrder.getProductId(),
                        savedOrder.getQuantity(),
                        savedOrder.getTotalPrice()
                );

        // Generate unique event ID
        event.setEventId(
                UUID.randomUUID().toString());

        // Publish event
        orderEventProducer.sendOrderCreatedEvent(event);

        return savedOrder;
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found: " + id));
    }

    public Order updateOrder(Long id, Order order) {

        Order existingOrder = getOrderById(id);

        existingOrder.setProductId(order.getProductId());
        existingOrder.setQuantity(order.getQuantity());
        existingOrder.setTotalPrice(order.getTotalPrice());
        existingOrder.setStatus(order.getStatus());

        return orderRepository.save(existingOrder);
    }

    public void deleteOrder(Long id) {
        Order existingOrder = getOrderById(id);
        orderRepository.delete(existingOrder);
    }
    
    public Order cancelOrder(Long id) {

        Order order = getOrderById(id);

        if ("PAID".equals(order.getStatus())) {

            throw new OrderCancellationException(
                    "Paid order cannot be cancelled");
        }

        if ("FAILED".equals(order.getStatus())) {

            throw new OrderCancellationException(
                    "Failed order cannot be cancelled");
        }

        if ("CANCELLED".equals(order.getStatus())) {

            throw new OrderCancellationException(
                    "Order is already cancelled");
        }

        order.setStatus("CANCELLED");

        return orderRepository.save(order);
    }
}