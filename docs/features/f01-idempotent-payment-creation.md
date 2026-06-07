# F1 — Idempotent Payment Creation

| Field | Value |
|---|---|
| **Feature ID** | F1 |
| **Release** | R1 |
| **Status** | Ready to build |
| **Depends on** | [F0](./f00-platform-foundation.md) |
| **Unlocks** | F2, F3 |
| **Est. effort** | ~1 weekend |

---

## Goal

Answer: **"What happens if the user taps Pay twice?"** — network retries must never create duplicate payment records.

---

## User stories

### F1-1 — Create payment

**As a** checkout client  
**I want to** submit a payment with amount, currency, merchant, and customer  
**So that** a payment record is created and I receive a payment ID for tracking

**Acceptance criteria**

- Given a valid request with a new `Idempotency-Key`
- When `POST /api/v1/payments` is called
- Then response is `201 Created` with `paymentId`, `status`, `amountCents`, `currency`
- And a payment row exists in the database
- And a correlation ID appears in logs

### F1-2 — Safe retry (same intent)

**As a** checkout client  
**I want to** retry the same payment request after a timeout  
**So that** I am not charged twice

**Acceptance criteria**

- Given a successful payment was created with key `K`
- When the same request (same key, same body) is sent again
- Then response status and body match the first response exactly
- And only one payment record exists for key `K`

### F1-3 — Reject key reuse with different intent

**As the** payment system  
**I want to** reject idempotency key reuse with a different payload  
**So that** clients cannot accidentally corrupt prior operations

**Acceptance criteria**

- Given key `K` was used for amount 4999
- When the same key is used with amount 9999
- Then response is `409 Conflict` with error code `IDEMPOTENCY_KEY_MISMATCH`
- And the original payment is unchanged

### F1-4 — Require idempotency key

**As the** payment system  
**I want to** reject requests without an idempotency key  
**So that** clients cannot accidentally create unprotected duplicate charges

**Acceptance criteria**

- Missing `Idempotency-Key` header → `422 Unprocessable Entity`

### F1-5 — Validate money fields

**As the** payment system  
**I want to** reject invalid amounts and currencies  
**So that** corrupt data never enters the ledger path (F4)

**Acceptance criteria**

- `amountCents` must be > 0
- `currency` must be 3-letter ISO 4217 (e.g. USD, EUR)
- Negative or zero amount → `400 Bad Request`

---

## Business rules

| Rule | Detail |
|---|---|
| BR-F1-1 | All `POST /payments` require `Idempotency-Key` (UUID v4 recommended) |
| BR-F1-2 | Amounts stored as **BIGINT cents** — never floats |
| BR-F1-3 | Initial status: `CREATED` |
| BR-F1-4 | Idempotency TTL default 24h (configurable) |
| BR-F1-5 | Request hash = SHA-256 of canonical JSON body |
| BR-F1-6 | Payment + idempotency row inserted in **one DB transaction** |

---

## API contract

### `POST /api/v1/payments`

**Headers:** `Idempotency-Key` (required), `X-Correlation-Id` (optional)

**Request**

```json
{
  "amountCents": 4999,
  "currency": "USD",
  "merchantId": "merchant-001",
  "customerId": "customer-001",
  "metadata": { "orderId": "order-abc-123" }
}
```

**Response `201 Created`**

```json
{
  "paymentId": "pay_7f3a2b1c",
  "status": "CREATED",
  "amountCents": 4999,
  "currency": "USD",
  "merchantId": "merchant-001",
  "customerId": "customer-001",
  "createdAt": "2026-06-09T10:00:00Z"
}
```

**Errors**

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Invalid body |
| 409 | `IDEMPOTENCY_KEY_MISMATCH` | Same key, different body |
| 422 | `IDEMPOTENCY_KEY_REQUIRED` | Missing header |

---

## Data model

### `payments` (V2 migration)

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR(36) | PK, UUID |
| `amount_cents` | BIGINT | NOT NULL, > 0 |
| `currency` | CHAR(3) | NOT NULL |
| `merchant_id` | VARCHAR(64) | NOT NULL |
| `customer_id` | VARCHAR(64) | NOT NULL |
| `status` | VARCHAR(32) | NOT NULL, default `CREATED` |
| `metadata` | JSONB | |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `updated_at` | TIMESTAMPTZ | NOT NULL |

### `idempotency_keys` (V2 migration)

| Column | Type | Notes |
|---|---|---|
| `idempotency_key` | VARCHAR(128) | PK |
| `request_hash` | VARCHAR(64) | NOT NULL |
| `http_status` | INT | NOT NULL |
| `response_body` | JSONB | NOT NULL |
| `payment_id` | VARCHAR(36) | FK → payments |
| `created_at` | TIMESTAMPTZ | NOT NULL |
| `expires_at` | TIMESTAMPTZ | |

---

## Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Payment API
    participant DB as PostgreSQL

    C->>API: POST /payments (Idempotency-Key: K)
    API->>DB: SELECT idempotency_keys WHERE key=K
    alt Key exists, same hash
        API-->>C: Replay cached response
    else Key exists, different hash
        API-->>C: 409 Conflict
    else New key
        API->>DB: BEGIN; INSERT payment; INSERT idempotency_key; COMMIT
        API-->>C: 201 Created
    end
```

---

## Test scenarios

| # | Scenario | Expected |
|:---:|---|---|
| T1 | First create with key K | 201, 1 payment row |
| T2 | Retry exact same request | 201, identical body, 1 payment row |
| T3 | Same key, different amount | 409 |
| T4 | Missing idempotency key | 422 |
| T5 | amountCents = 0 | 400 |
| T6 | Concurrent duplicate requests same key | Exactly one payment created |

---

## Demo script

```bash
KEY=$(uuidgen)
BASE=http://localhost:8080/api/v1/payments
BODY='{"amountCents":4999,"currency":"USD","merchantId":"m1","customerId":"c1"}'

curl -s -X POST "$BASE" -H "Content-Type: application/json" -H "Idempotency-Key: $KEY" -d "$BODY"
curl -s -X POST "$BASE" -H "Content-Type: application/json" -H "Idempotency-Key: $KEY" -d "$BODY"
curl -s -X POST "$BASE" -H "Content-Type: application/json" -H "Idempotency-Key: $KEY" \
  -d '{"amountCents":9999,"currency":"USD","merchantId":"m1","customerId":"c1"}'
```

---

## Definition of done

- [ ] All test scenarios pass (automated)
- [ ] curl demo script works
- [ ] OpenAPI updated with payment create endpoint
- [ ] PO note written

---

## Out of scope

- State transitions beyond `CREATED`
- PSP, webhooks, ledger, wallet
- Authentication

---

## PO note template

**Problem:** Network retries on checkout can duplicate payment records.

**Decision:** Client-supplied idempotency keys; atomic insert of payment + cached response.

**User impact:** Consistent result on retry; no double charge.

**Metrics:** Idempotency replay rate, 409 rate, p99 create latency.

**Validate with engineering:** Concurrent request handling; key TTL policy.
