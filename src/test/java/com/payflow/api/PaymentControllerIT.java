package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.payflow.domain.repository.PaymentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentControllerIT {

	private static final String URL = "/api/v1/payments";
	private static final String VALID_BODY =
			"{\"amountCents\":4999,\"currency\":\"USD\",\"merchantId\":\"m1\",\"customerId\":\"c1\"}";

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private PaymentRepository paymentRepository;

	@Test
	void createsPaymentOnFirstRequest() {
		long paymentsBefore = paymentRepository.count();

		ResponseEntity<String> response = post("key-create-1", VALID_BODY);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).contains("\"status\":\"CREATED\"");
		assertThat(response.getBody()).contains("\"amountCents\":4999");
		assertThat(response.getBody()).contains("\"currency\":\"USD\"");
		assertThat(paymentRepository.count()).isEqualTo(paymentsBefore + 1);
	}

	@Test
	void replaysIdenticalRequestWithSameIdempotencyKey() {
		post("key-replay-1", VALID_BODY);
		long paymentsAfterFirst = paymentRepository.count();

		ResponseEntity<String> first = post("key-replay-1", VALID_BODY);
		ResponseEntity<String> second = post("key-replay-1", VALID_BODY);

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(second.getBody()).isEqualTo(first.getBody());
		assertThat(paymentRepository.count()).isEqualTo(paymentsAfterFirst);
	}

	@Test
	void rejectsSameKeyWithDifferentRequestBody() {
		post("key-mismatch-1", VALID_BODY);
		long paymentsAfterFirst = paymentRepository.count();

		ResponseEntity<String> response = post(
				"key-mismatch-1",
				"{\"amountCents\":9999,\"currency\":\"USD\",\"merchantId\":\"m1\",\"customerId\":\"c1\"}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).contains("IDEMPOTENCY_KEY_MISMATCH");
		assertThat(paymentRepository.count()).isEqualTo(paymentsAfterFirst);
	}

	@Test
	void rejectsMissingIdempotencyKey() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = restTemplate.postForEntity(
				URL,
				new HttpEntity<>(VALID_BODY, headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(response.getBody()).contains("IDEMPOTENCY_KEY_REQUIRED");
	}

	@Test
	void rejectsZeroAmount() {
		ResponseEntity<String> response = post(
				"key-zero-amount-1",
				"{\"amountCents\":0,\"currency\":\"USD\",\"merchantId\":\"m1\",\"customerId\":\"c1\"}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("VALIDATION_ERROR");
	}

	private ResponseEntity<String> post(String idempotencyKey, String body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Idempotency-Key", idempotencyKey);
		return restTemplate.postForEntity(URL, new HttpEntity<>(body, headers), String.class);
	}

}
