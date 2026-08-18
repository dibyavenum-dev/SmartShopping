package com.smartshopping.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshopping.orderservice.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long>{

}
