package com.smartshopping.inventoryservice.service;

import org.springframework.stereotype.Service;

import com.smartshopping.inventoryservice.entity.Inventory;
import com.smartshopping.inventoryservice.exception.InsufficientStockException;
import com.smartshopping.inventoryservice.exception.InventoryNotFoundException;
import com.smartshopping.inventoryservice.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(
            InventoryRepository inventoryRepository) {

        this.inventoryRepository = inventoryRepository;
    }

    public void reserveStock(
            Long productId,
            int quantity) {

        Inventory inventory =
                inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                new InventoryNotFoundException(
                        "Inventory not found for product: "
                                + productId));

        if (inventory.getAvailableQuantity() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient stock for product: "
                            + productId);
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity);

        inventoryRepository.save(inventory);
    }
}