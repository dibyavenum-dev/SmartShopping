package com.smartshopping.orderservice.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.smartshopping.orderservice.dto.ProductResponse;
import com.smartshopping.orderservice.entity.Order;
import com.smartshopping.orderservice.event.OrderCreatedEvent;
import com.smartshopping.orderservice.exception.OrderCancellationException;
import com.smartshopping.orderservice.exception.OrderNotFoundException;
import com.smartshopping.orderservice.exception.ProductNotFoundException;
import com.smartshopping.orderservice.exception.ProductServiceUnavailableException;
import com.smartshopping.orderservice.producer.OrderEventProducer;
import com.smartshopping.orderservice.repository.OrderRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class OrderService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderEventProducer orderEventProducer;

    public OrderService(
            OrderRepository orderRepository,
            RestTemplate restTemplate,
            OrderEventProducer orderEventProducer) {

        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.orderEventProducer = orderEventProducer;
    }

    public Order createOrder(Order order) {

        ProductResponse product;

        try {

            product = getProduct(order.getProductId());

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

        Order savedOrder =
                orderRepository.save(order);

        log.info(
                "Order created successfully. Order ID: {}, Product ID: {}, Quantity: {}",
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity());

        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        savedOrder.getId(),
                        savedOrder.getProductId(),
                        savedOrder.getQuantity(),
                        savedOrder.getTotalPrice());

        event.setEventId(
                UUID.randomUUID().toString());

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

    public Order updateOrder(
            Long id,
            Order order) {

        Order existingOrder =
                getOrderById(id);

        existingOrder.setProductId(
                order.getProductId());

        existingOrder.setQuantity(
                order.getQuantity());

        existingOrder.setTotalPrice(
                order.getTotalPrice());

        existingOrder.setStatus(
                order.getStatus());

        Order updatedOrder =
                orderRepository.save(existingOrder);

        log.info(
                "Order updated successfully. Order ID: {}",
                id);

        return updatedOrder;
    }

    public void deleteOrder(Long id) {

        Order existingOrder =
                getOrderById(id);

        orderRepository.delete(existingOrder);

        log.info(
                "Order deleted successfully. Order ID: {}",
                id);
    }

    public Order cancelOrder(Long id) {

        Order order =
                getOrderById(id);

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

        Order cancelledOrder =
                orderRepository.save(order);

        log.info(
                "Order cancelled successfully. Order ID: {}",
                id);

        return cancelledOrder;
    }

    @CircuitBreaker(
            name = "productService",
            fallbackMethod = "productServiceFallback"
    )
    public ProductResponse getProduct(
            Long productId) {

        try {

            return restTemplate.getForObject(
                    "http://PRODUCT-SERVICE/products/"
                            + productId,
                    ProductResponse.class);

        } catch (
                IllegalStateException |
                ResourceAccessException ex) {

            log.error(
                    "Product Service unavailable for Product ID: {}",
                    productId,
                    ex);

            throw new ProductServiceUnavailableException(
                    "Product Service is currently unavailable. "
                            + "Please try again later.");
        }
    }

    public ProductResponse productServiceFallback(
            Long productId,
            Throwable ex) {

        log.error(
                "Circuit breaker fallback triggered for Product ID: {}",
                productId,
                ex);

        throw new ProductServiceUnavailableException(
                "Product Service is currently unavailable. "
                        + "Please try again later.");
    }
}