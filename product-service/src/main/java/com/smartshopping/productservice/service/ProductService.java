package com.smartshopping.productservice.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smartshopping.productservice.entity.Product;
import com.smartshopping.productservice.exception.ProductNotFoundException;
import com.smartshopping.productservice.repository.ProductRepository;

@Service
public class ProductService {

    private static final Logger log =
            LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {

        Product savedProduct =
                productRepository.save(product);

        log.info(
                "Product created successfully. Product ID: {}",
                savedProduct.getId());

        return savedProduct;
    }

    public List<Product> getAllProducts() {

        return productRepository.findAll();
    }

    public Product getProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found: " + id));
    }

    public Product updateProduct(
            Long id,
            Product product) {

        Product existingProduct =
                getProductById(id);

        existingProduct.setName(
                product.getName());

        existingProduct.setDescription(
                product.getDescription());

        existingProduct.setPrice(
                product.getPrice());

        existingProduct.setQuantity(
                product.getQuantity());

        existingProduct.setCategory(
                product.getCategory());

        Product updatedProduct =
                productRepository.save(existingProduct);

        log.info(
                "Product updated successfully. Product ID: {}",
                id);

        return updatedProduct;
    }

    public void deleteProduct(Long id) {

        Product existingProduct =
                getProductById(id);

        productRepository.delete(existingProduct);

        log.info(
                "Product deleted successfully. Product ID: {}",
                id);
    }
}