package com.smartshopping.paymentservice.event;

public class PaymentProcessedEvent {

    private String eventId;
    private Long paymentId;
    private Long orderId;
    private Double amount;
    private String status;

    public PaymentProcessedEvent() {
    }

    public PaymentProcessedEvent(String eventId, Long paymentId,
                                 Long orderId, Double amount,
                                 String status) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}