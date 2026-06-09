package com.payflow.api.advice;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payflow.api.exception.IdempotencyKeyMismatchException;
import com.payflow.api.exception.IdempotencyKeyRequiredException;
import com.payflow.support.CorrelationIdHolder;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(
						"error", "Validation failed",
						"code", "VALIDATION_ERROR",
						"correlationId", correlationIdOrUnknown()));
	}

	@ExceptionHandler(IdempotencyKeyRequiredException.class)
	public ResponseEntity<Map<String, String>> handleIdempotencyKeyRequired(IdempotencyKeyRequiredException ex) {
		return ResponseEntity
				.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(Map.of(
						"error", "Idempotency-Key header is required",
						"code", "IDEMPOTENCY_KEY_REQUIRED",
						"correlationId", correlationIdOrUnknown()));
	}

	@ExceptionHandler(IdempotencyKeyMismatchException.class)
	public ResponseEntity<Map<String, String>> handleIdempotencyKeyMismatch(IdempotencyKeyMismatchException ex) {
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(Map.of(
						"error", "Idempotency key was already used with a different request body",
						"code", "IDEMPOTENCY_KEY_MISMATCH",
						"correlationId", correlationIdOrUnknown()));
	}

	private String correlationIdOrUnknown() {
		String correlationId = CorrelationIdHolder.get();
		return correlationId != null ? correlationId : "unknown";
	}

}
