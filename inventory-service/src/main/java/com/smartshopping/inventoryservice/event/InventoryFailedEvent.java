package com.smartshopping.inventoryservice.event;

public class InventoryFailedEvent {

    private Long orderId;
    private Long productId;
    private int quantity;
    private String reason;

    public InventoryFailedEvent() {
    }

    public InventoryFailedEvent(
            Long orderId,
            Long productId,
            int quantity,
            String reason) {

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}