package com.smartshopping.paymentservice.controller;

import org.springframework.web.bind.annotation.*;

import com.smartshopping.paymentservice.entity.Payment;
import com.smartshopping.paymentservice.exception.PaymentNotFoundException;
import com.smartshopping.paymentservice.repository.PaymentRepository;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {

        payment.setStatus("SUCCESS");

        return paymentRepository.save(payment);
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException("Payment not found"));
    }
}