package com.payflow.api.exception;

public class IdempotencyKeyMismatchException extends RuntimeException {

	public IdempotencyKeyMismatchException() {
		super("Idempotency key reused with different request body");
	}
}
