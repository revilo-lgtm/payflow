package com.payflow.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payflow.api.dto.CreatePaymentRequest;
import com.payflow.api.dto.PaymentResponse;
import com.payflow.service.CreatePaymentResult;
import com.payflow.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
       @Valid @RequestBody CreatePaymentRequest request
    ) {
        CreatePaymentResult result = paymentService.createPayment(idempotencyKey, request);
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
