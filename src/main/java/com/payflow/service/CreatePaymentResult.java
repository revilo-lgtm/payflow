package com.payflow.service;

import org.springframework.http.HttpStatus;

import com.payflow.api.dto.PaymentResponse;

public record CreatePaymentResult(PaymentResponse body, HttpStatus status) {
    
}
