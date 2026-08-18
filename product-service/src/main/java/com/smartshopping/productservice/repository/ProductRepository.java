package com.smartshopping.productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshopping.productservice.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{

}
