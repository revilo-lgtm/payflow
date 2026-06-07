# F8 — Checkout Client / Miniapp Demo *(Optional)*

| Field | Value |
|---|---|
| **Feature ID** | F8 |
| **Release** | R8 |
| **Status** | Backlog |
| **Depends on** | [F2](./f02-payment-lifecycle.md) (F3+ for full flow) |
| **Unlocks** | — |
| **Est. effort** | ~½ weekend |

---

## Goal

Connect PayFlow to your **platform TPO** background: a thin client (HTML page or script) that mimics a **miniapp in a shell** — create payment, poll status, map states to user copy, reuse idempotency key on retry.

Portfolio piece: shows channel UX on top of payment API.

---

## User stories

### F8-1 — Checkout demo page

**As a** demo user  
**I want** a simple checkout UI  
**So that** I can click Pay and see status updates

**Acceptance criteria**

- Static page in `demo/checkout/index.html` OR small React/Vanilla JS app
- Fields: amount, merchantId (prefilled), Pay button
- Calls PayFlow API (CORS enabled for local dev)

### F8-2 — Poll until terminal state

**As a** buyer  
**I want** the UI to update while payment processes  
**So that** I don't stare at a frozen screen

**Acceptance criteria**

- After create, poll `GET /payments/{id}` every 2s until terminal state
- Stop polling on `SETTLED`, `FAILED`, `VOIDED`, `REFUNDED`

### F8-3 — Client idempotency on retry

**As a** buyer  
**I want** Pay retry to reuse the same idempotency key  
**So that** double-click doesn't double-charge

**Acceptance criteria**

- Key generated once per checkout session (sessionStorage)
- Retry button reuses key + same payload

### F8-4 — State → UI copy

**As a** product owner  
**I want** user-facing messages per backend state  
**So that** UX matches F2 mapping table

**Acceptance criteria**

- UI implements mapping from [F2 user-visible status table](./f02-payment-lifecycle.md#user-visible-status-mapping)
- Failed state shows "Try again" (new session / new key)

### F8-5 — Shell / miniapp narrative

**As a** portfolio reader  
**I want** a 1-page architecture note  
**So that** I understand how this maps to microfrontend platform work

**Acceptance criteria**

- Doc: `docs/features/f08-shell-miniapp-narrative.md` explains SDK contract, polling, idempotency key ownership

---

## Shell / miniapp narrative (draft)

```
┌─────────────────────────────────────┐
│  Shell (host app)                   │
│  - auth context, layout, routing    │
│  - loads payment miniapp            │
│  ┌───────────────────────────────┐  │
│  │ Payment miniapp               │  │
│  │ - @payflow/sdk createPayment()│  │
│  │ - pollPaymentStatus(id)       │  │
│  │ - mapStatusToUI(state)        │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
           │
           ▼
    PayFlow API (this project)
```

**SDK responsibilities (conceptual):**

- Generate `Idempotency-Key` per checkout intent
- Exponential backoff on poll (optional)
- Normalize API errors to `{ retryable, message, code }`

---

## Technical notes

| Topic | Choice |
|---|---|
| CORS | `@CrossOrigin` or WebMvc config for `localhost:5173` / file demo |
| Auth | None for demo (document as out of scope) |
| Serve demo | Spring static resources or separate `npm run dev` |

---

## Test scenarios

| # | Scenario | Expected |
|:---:|---|---|
| T8-1 | Click Pay once | Status progresses to terminal |
| T8-2 | Double-click Pay quickly | One payment (idempotency) |
| T8-3 | Simulate failure | Error message + retry affordance |

---

## Definition of done

- [ ] Demo runnable from README
- [ ] Screen recording or GIF optional for portfolio
- [ ] Shell narrative doc complete

---

## Out of scope

- Real shell integration at NAB
- Production-grade SDK packaging

---

## PO note template

**Problem:** Payment UX lives in channel apps; backend states are opaque to users.

**Decision:** Miniapp polls payment API; shell provides auth only; SDK owns idempotency.

**User impact:** Clear progress and error recovery on checkout.

**Metrics:** Poll count to terminal, checkout completion rate (hypothetical).
