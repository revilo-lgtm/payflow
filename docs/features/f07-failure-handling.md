# F7 — Failure Handling & Retries *(Optional)*

| Field | Value |
|---|---|
| **Feature ID** | F7 |
| **Release** | R7 |
| **Status** | Backlog |
| **Depends on** | [F5](./f05-psp-webhooks.md) |
| **Unlocks** | — |
| **Est. effort** | ~1 weekend |

---

## Goal

Handle **non-happy paths**: PSP timeouts, stuck `PROCESSING` payments, failed webhook processing, retry with backoff, dead-letter queue.

Maps to Xu chapter: handling failed payments, retry queue, DLQ.

---

## User stories

### F7-1 — Stuck payment detection

**As** ops  
**I want** payments stuck in `PROCESSING` flagged  
**So that** they don't silently linger

**Acceptance criteria**

- Scheduled job finds payments where `status=PROCESSING` AND `updated_at < now() - threshold`
- Marks as `NEEDS_ATTENTION` or emits alert record

### F7-2 — PSP call retry policy

**As the** payment executor  
**I want** exponential backoff on transient PSP errors  
**So that** temporary outages recover automatically

**Acceptance criteria**

- Retry 3x with backoff: 1s, 2s, 4s (configurable)
- Idempotency preserved across retries (same PSP idempotency key)

### F7-3 — Terminal failure after max retries

**As a** buyer  
**I want** clear failure when PSP unreachable  
**So that** I can retry checkout

**Acceptance criteria**

- After max retries → payment `FAILED` with reason `PSP_UNAVAILABLE`
- History records failure reason

### F7-4 — Webhook processing DLQ

**As** ops  
**I want** failed webhook processing stored  
**So that** events can be replayed

**Acceptance criteria**

- Table `webhook_dlq`: payload, error, attempts, next_retry_at
- After N failures → status `POISON` (manual review)

### F7-5 — Admin replay (dev only)

**As a** developer  
**I want** to replay DLQ items  
**So that** I can verify recovery

**Acceptance criteria**

- `POST /api/v1/internal/webhooks/dlq/{id}/replay` (profile=dev)

---

## Business rules

| Rule | Detail |
|---|---|
| BR-F7-1 | Retries must not double-charge — PSP + internal idempotency |
| BR-F7-2 | DLQ replay is idempotent on `event_id` |
| BR-F7-3 | Alerting = log + DB flag (no PagerDuty in learning project) |

---

## Data model (V8 migration)

### `webhook_dlq`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `event_id` | VARCHAR(128) | |
| `payload` | JSONB | |
| `error_message` | TEXT | |
| `attempts` | INT | |
| `next_retry_at` | TIMESTAMPTZ | |
| `status` | VARCHAR(16) | `PENDING`, `POISON`, `RESOLVED` |

---

## Test scenarios

| # | Scenario | Expected |
|:---:|---|---|
| T7-1 | PSP timeout then success on retry | Eventually AUTHORIZED |
| T7-2 | PSP down after max retries | FAILED |
| T7-3 | Webhook handler throws | Entry in DLQ |
| T7-4 | Replay DLQ event | Processed once |

---

## Definition of done

- [ ] Stuck payment job runs in tests
- [ ] Retry + DLQ demo documented
- [ ] PO note: retry vs give up tradeoffs

---

## Out of scope

- Kafka / SQS (in-memory or DB-backed queue is fine)
- Circuit breaker across services (stretch)

---

## PO note template

**Problem:** External dependencies fail; payments stall in ambiguous states.

**Decision:** Bounded retries + DLQ + stuck-payment sweeper.

**User impact:** Fewer indefinite "Processing…" states; recoverable failures.

**Metrics:** Retry success rate, DLQ depth, stuck payment count.
