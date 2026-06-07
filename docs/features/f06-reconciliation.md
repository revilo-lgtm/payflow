# F6 — Reconciliation

| Field | Value |
|---|---|
| **Feature ID** | F6 |
| **Release** | R6 (**Full depth**) |
| **Status** | Ready to build |
| **Depends on** | [F4](./f04-wallet-ledger.md), [F5](./f05-psp-webhooks.md) |
| **Unlocks** | — |
| **Est. effort** | ~1 weekend |

---

## Goal

Answer: **"How do you know your books are correct at end of day?"** — batch job compares internal records vs PSP settlement file and flags mismatches.

Maps to Xu chapter: reconciliation; ByteByteGo articles on reconciliation.

---

## User stories

### F6-1 — Settlement file ingestion

**As** ops/finance  
**I want** a daily settlement file from the PSP  
**So that** external truth can be compared to internal records

**Acceptance criteria**

- Simulator generates `settlements/YYYY-MM-DD.json` or accepts upload
- Format documented below

### F6-2 — Reconciliation job

**As** ops  
**I want** an automated matching job  
**So that** discrepancies are found without manual spreadsheet work

**Acceptance criteria**

- Job matches on `pspReference` or `paymentId` + amount + currency
- Runnable via `POST /api/v1/internal/reconciliation/run?date=YYYY-MM-DD` (dev) or CLI

### F6-3 — Mismatch classification

**As** ops  
**I want** typed exceptions  
**So that** I know how to fix each issue

**Acceptance criteria**

- Types: `MATCHED`, `MISSING_INTERNAL`, `MISSING_PSP`, `AMOUNT_MISMATCH`, `DUPLICATE`

### F6-4 — Reconciliation report

**As** a product owner  
**I want** a summary report  
**So that** I can track financial integrity metrics

**Acceptance criteria**

- Report JSON: `{ "date", "matched", "exceptions": [...], "summary": { ... } }`
- Export CSV optional

### F6-5 — Chaos scenario support

**As a** learner  
**I want** to demo missing webhook → mismatch  
**So that** I understand why reconciliation matters

**Acceptance criteria**

- Test fixture: payment captured internally but absent from settlement file → `MISSING_PSP` or inverse

---

## Business rules

| Rule | Detail |
|---|---|
| BR-F6-1 | Reconciliation is **read-only** on payments — flags only, no auto-fix in MVP |
| BR-F6-2 | Job is idempotent per `(date, run_id)` |
| BR-F6-3 | Amount comparison in minor units |
| BR-F6-4 | Unmatched rows default status `NEEDS_REVIEW` |

---

## Settlement file format (simulated)

```json
{
  "settlementDate": "2026-06-09",
  "psp": "simulated",
  "rows": [
    {
      "pspReference": "psp_xyz789",
      "paymentId": "pay_7f3a2b1c",
      "amountCents": 4999,
      "currency": "USD",
      "status": "captured",
      "capturedAt": "2026-06-09T10:05:30Z"
    }
  ]
}
```

---

## Data model (V7 migration)

### `reconciliation_runs`

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR(36) | PK |
| `settlement_date` | DATE | |
| `started_at` | TIMESTAMPTZ | |
| `completed_at` | TIMESTAMPTZ | |
| `matched_count` | INT | |
| `exception_count` | INT | |

### `reconciliation_exceptions`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `run_id` | VARCHAR(36) | FK |
| `type` | VARCHAR(32) | |
| `payment_id` | VARCHAR(36) | Nullable |
| `psp_reference` | VARCHAR(64) | Nullable |
| `internal_amount_cents` | BIGINT | Nullable |
| `psp_amount_cents` | BIGINT | Nullable |
| `status` | VARCHAR(16) | `NEEDS_REVIEW` |

---

## Matching algorithm (MVP)

```
For each settlement row:
  Find internal payment by pspReference (or paymentId)
  If not found → MISSING_INTERNAL
  If amount/currency differ → AMOUNT_MISMATCH
  Else → MATCHED

For each internal CAPTURED payment on date without settlement row:
  → MISSING_PSP
```

---

## Test scenarios

| # | Scenario | Expected |
|:---:|---|---|
| T6-1 | Perfect match file | exception_count = 0 |
| T6-2 | Amount off by 1 cent | AMOUNT_MISMATCH |
| T6-3 | Missing webhook payment | MISSING_PSP or MISSING_INTERNAL |
| T6-4 | Re-run same date | Idempotent report or new run_id |

---

## Definition of done

- [ ] Happy path 100% match demo
- [ ] Chaos case surfaces in report
- [ ] PO note: reconciliation as safety net

---

## Out of scope

- Auto-adjustment entries
- Multi-PSP aggregation
- FX reconciliation

---

## PO note template

**Problem:** Webhooks fail; internal and PSP records drift.

**Decision:** Nightly reconciliation job with typed exceptions and manual review queue.

**User impact:** Indirect — financial integrity and merchant trust.

**Metrics:** Match rate, open exceptions, mean time to resolve.

**Validate with ops:** SLA for exception handling; escalation path.
