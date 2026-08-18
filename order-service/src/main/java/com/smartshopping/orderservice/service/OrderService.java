package com.smartshopping.orderservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.smartshopping.orderservice.dto.ProductResponse;
import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.OrderCreatedEvent;
import com.smartshopping.orderservice.producer.OrderEventProducer;
import com.smartshopping.orderservice.repository.OrderRepository;
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

        ProductResponse product = restTemplate.getForObject(
                "http://PRODUCT-SERVICE/products/" + order.getProductId(),
                ProductResponse.class
        );

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        double totalPrice =
                product.getPrice() * order.getQuantity();

        order.setTotalPrice(totalPrice);
        order.setStatus("CREATED");

        // 1. Save order
        Order savedOrder = orderRepository.save(order);

        // 2. Create Kafka event
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalPrice()
        );
        
        // 3. Generate unique event ID
        event.setEventId(UUID.randomUUID().toString());
        
        // 3. Publish event
        orderEventProducer.sendOrderCreatedEvent(event);

        return savedOrder;
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
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
}