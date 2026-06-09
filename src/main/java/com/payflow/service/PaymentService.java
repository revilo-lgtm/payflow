package com.payflow.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.api.dto.CreatePaymentRequest;
import com.payflow.api.dto.PaymentResponse;
import com.payflow.api.exception.IdempotencyKeyMismatchException;
import com.payflow.api.exception.IdempotencyKeyRequiredException;
import com.payflow.domain.model.IdempotencyKey;
import com.payflow.domain.model.Payment;
import com.payflow.domain.model.PaymentStatus;
import com.payflow.domain.repository.IdempotencyKeyRepository;
import com.payflow.domain.repository.PaymentRepository;
import com.payflow.support.RequestHasher;

@Service
public class PaymentService {
    

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    private final Duration idempotencyTtl;

    public PaymentService(
        PaymentRepository paymentRepository,
        IdempotencyKeyRepository idempotencyKeyRepository,
        ObjectMapper objectMapper,
        @Value("${payflow.idempotency.ttl-hours:24}") long ttlHours) {

        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
        this.idempotencyTtl = Duration.ofHours(ttlHours);
    }

    @Transactional
    public CreatePaymentResult createPayment(String idempotencyKey, CreatePaymentRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new IdempotencyKeyRequiredException();
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = RequestHasher.hash(request);
        
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(normalizedKey);
        if (existing.isPresent()) {
            return replayOrReject(existing.get(), requestHash);
        }

        try {
            return createNewPayment(normalizedKey, requestHash, request);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate request with same key - recheck stored result
            IdempotencyKey raced = idempotencyKeyRepository.findById(normalizedKey)
            .orElseThrow(() -> ex);
            return replayOrReject(raced, requestHash);
        }
    }

    private CreatePaymentResult replayOrReject(IdempotencyKey record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyKeyMismatchException();
        }
        return new CreatePaymentResult(deserializeResponse(record), HttpStatus.valueOf(record.getHttpStatus()));
    }

    private CreatePaymentResult createNewPayment(
        String idempotencyKey,
        String requestHash,
        CreatePaymentRequest request) {
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID().toString());
    payment.setAmountCents(request.getAmountCents());
    payment.setCurrency(request.getCurrency());
    payment.setMerchantId(request.getMerchantId());
    payment.setCustomerId(request.getCustomerId());
    payment.setStatus(PaymentStatus.CREATED);
    payment.setMetadata(request.getMetadata());
    paymentRepository.save(payment);
    PaymentResponse response = PaymentResponse.from(payment);
    String responseJson = serializeResponse(response);
    IdempotencyKey record = new IdempotencyKey();
    record.setIdempotencyKey(idempotencyKey);
    record.setRequestHash(requestHash);
    record.setHttpStatus(HttpStatus.CREATED.value());
    record.setResponseBody(responseJson);
    record.setPaymentId(payment.getId());
    record.setExpiresAt(Instant.now().plus(idempotencyTtl));
    idempotencyKeyRepository.save(record);
    return new CreatePaymentResult(response, HttpStatus.CREATED);
}
    
    private PaymentResponse deserializeResponse(IdempotencyKey record) {
        try {
            return objectMapper.readValue(record.getResponseBody(), PaymentResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotency response is invalid JSON", ex);
        }
    }

    private String serializeResponse(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize payment response", ex);
        }
    }
}


