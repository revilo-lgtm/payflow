package com.payflow.api.dto;

import java.time.Instant;

import com.payflow.domain.model.Payment;
import com.payflow.domain.model.PaymentStatus;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentId;
    private PaymentStatus status;
    private long amountCents;
    private String currency;
    private String merchantId;
    private String customerId;
    private Instant createdAt;

    public static PaymentResponse from(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.paymentId = payment.getId();
        response.status = payment.getStatus();
        response.amountCents = payment.getAmountCents();
        response.currency = payment.getCurrency();
        response.merchantId = payment.getMerchantId();
        response.customerId = payment.getCustomerId();
        response.createdAt = payment.getCreatedAt();
        return response;
    }
}
