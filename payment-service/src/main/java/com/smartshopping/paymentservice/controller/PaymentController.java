package com.smartshopping.paymentservice.controller;

import org.springframework.web.bind.annotation.*;

import com.smartshopping.paymentservice.dto.PaymentRequest;
import com.smartshopping.paymentservice.entity.Payment;
import com.smartshopping.paymentservice.exception.PaymentNotFoundException;
import com.smartshopping.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Payment createPayment(
            @Valid @RequestBody PaymentRequest request) {

        return paymentService.createPayment(
                request.getOrderId(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getStatus());
    }

    @GetMapping("/{id}")
    public Payment getPayment(
            @PathVariable Long id) {

        return paymentService.getPaymentById(id);
    }
}