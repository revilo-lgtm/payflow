# Slice 1 — Idempotent Payment Creation

> **Superseded by:** [features/f01-idempotent-payment-creation.md](../features/f01-idempotent-payment-creation.md)  
> This file is kept for backwards compatibility — use the F1 spec for the latest detail.

---
## User stories

### US-1.1 — Create payment

**As a** checkout client  
**I want to** submit a payment with amount, currency, merchant, and customer  
**So that** a payment record is created and I receive a payment ID for tracking

**Acceptance criteria**

- Given a valid request with a new `Idempotency-Key`
- When `POST /api/v1/payments` is called
- Then response is `201 Created` with `paymentId`, `status`, `amountCents`, `currency`
- And a payment row exists in the database
- And a correlation ID appears in logs

### US-1.2 — Safe retry (same intent)

**As a** checkout client  
**I want to** retry the same payment request after a timeout  
**So that** I am not charged twice

**Acceptance criteria**

- Given a successful payment was created with key `K`
- When the same request (same key, same body) is sent again
- Then response status and body match the first response exactly
- And only one payment record exists for key `K`

### US-1.3 — Reject key reuse with different intent

**As the** payment system  
**I want to** reject idempotency key reuse with a different payload  
**So that** clients cannot accidentally corrupt prior operations

**Acceptance criteria**

- Given key `K` was used for amount 4999
- When the same key is used with amount 9999
- Then response is `409 Conflict` with a clear error code
- And the original payment is unchanged

---

## Business rules

| Rule | Detail |
|---|---|
| BR-1 | All `POST /payments` require `Idempotency-Key` header (UUID recommended) |
| BR-2 | Amounts stored as **integer cents** — never floats |
| BR-3 | Initial status is `CREATED` or `PENDING` |
| BR-4 | Idempotency records retained for at least 24 hours (configurable) |
| BR-5 | Request body hash stored with idempotency key to detect payload mismatch |

---

## API contract

### `POST /api/v1/payments`

**Headers**

| Header | Required | Description |
|---|---|---|
| `Idempotency-Key` | Yes | Client-generated UUID |
| `X-Correlation-Id` | No | Generated server-side if absent |

**Request body**

```json
{
  "amountCents": 4999,
  "currency": "USD",
  "merchantId": "merchant-001",
  "customerId": "customer-001",
  "metadata": {
    "orderId": "order-abc-123"
  }
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

| Status | When |
|---|---|
| `400` | Validation failure (missing fields, invalid currency) |
| `409` | Same idempotency key, different request body |
| `422` | Missing `Idempotency-Key` header |

---

## Data model (minimal)

### `payments`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID / VARCHAR | Primary key |
| `amount_cents` | BIGINT | Not null |
| `currency` | CHAR(3) | ISO 4217 |
| `merchant_id` | VARCHAR | |
| `customer_id` | VARCHAR | |
| `status` | VARCHAR | `CREATED` initially |
| `metadata` | JSONB | Optional |
| `created_at` | TIMESTAMPTZ | |

### `idempotency_keys`

| Column | Type | Notes |
|---|---|---|
| `key` | VARCHAR | Primary key |
| `request_hash` | VARCHAR | Hash of canonical request body |
| `response_status` | INT | HTTP status to replay |
| `response_body` | JSONB | Cached response |
| `payment_id` | VARCHAR | FK to payments |
| `created_at` | TIMESTAMPTZ | |
| `expires_at` | TIMESTAMPTZ | Optional TTL |

---

## Sequence (Slice 1)

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Payment API
    participant DB as PostgreSQL

    C->>API: POST /payments (Idempotency-Key: K)
    API->>DB: Lookup idempotency_keys where key=K
    alt Key exists, same hash
        DB-->>API: Cached response
        API-->>C: Replay cached response
    else Key exists, different hash
        API-->>C: 409 Conflict
    else New key
        API->>DB: BEGIN; insert payment; insert idempotency_key; COMMIT
        API-->>C: 201 Created
    end
```

---

## Test scenarios

| # | Scenario | Expected |
|:---:|---|---|
| T1 | First create with key K | 201, payment in DB |
| T2 | Retry exact same request | 201, same body, 1 payment row |
| T3 | Same key, different amount | 409 |
| T4 | Missing idempotency key | 422 |
| T5 | Negative amount | 400 |

---

## Demo script (curl)

```bash
KEY=$(uuidgen)

# Create
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $KEY" \
  -d '{"amountCents":4999,"currency":"USD","merchantId":"m1","customerId":"c1"}'

# Retry (should be identical)
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $KEY" \
  -d '{"amountCents":4999,"currency":"USD","merchantId":"m1","customerId":"c1"}'

# Conflict
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $KEY" \
  -d '{"amountCents":9999,"currency":"USD","merchantId":"m1","customerId":"c1"}'
```

---

## PO note template (fill after build)

**Problem:** Network retries on checkout can create duplicate payment records.

**Decision:** Require client-supplied idempotency keys; store response atomically with payment creation.

**User impact:** Buyer sees consistent result on retry; no double charge.

**Metrics to watch:** Duplicate-key replay rate, 409 rate, create latency.

**Open questions for eng/legal/ops:** TTL for idempotency keys in production; behaviour when key expires mid-retry.

---

## Out of scope for Slice 1

- PSP integration
- State transitions beyond `CREATED`
- Ledger / wallet
- Webhooks
- Authentication
