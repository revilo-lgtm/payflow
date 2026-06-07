package com.payflow.api.advice;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

	private String correlationIdOrUnknown() {
		String correlationId = CorrelationIdHolder.get();
		return correlationId != null ? correlationId : "unknown";
	}

}
