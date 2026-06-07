# PayFlow

A self-learning **mini payment platform** — built slice-by-slice to practice Technical Product Ownership, solution design, and backend engineering in payments.

Inspired by the [Pragmatic Engineer excerpt](https://newsletter.pragmaticengineer.com/p/designing-a-payment-system) of Alex Xu & Sahn Lam’s *System Design Interview* Vol. 2 Payment System chapter.

---
## App Quick Start
docker compose up -d

mvn spring-boot:run

curl http://localhost:8080/actuator/health

curl http://localhost:8080/api/v1/ping

## Start here

| Doc | Purpose |
|---|---|
| **[PROJECT-FRAMING.md](./PROJECT-FRAMING.md)** | Vision, scope, architecture |
| **[EPIC-BREAKDOWN.md](./docs/EPIC-BREAKDOWN.md)** | Epic overview, releases, dependencies |
| **[Feature specs](./docs/features/)** | F0–F8 detailed backlog (stories, API, schema, tests) |
| **[Reference chapter](../docs/designing-payment-system/designing-a-payment-system.md)** | Full payment system design chapter |
| **[Learning plan](../payments-learning-plan.md)** | Books, repos, 8-week study schedule |

---

## What we're building

```
Slice 1  Idempotent POST /payments
Slice 2  State machine + GET /payments/{id}
Slice 3  Auth / capture / void
Slice 4  Double-entry ledger + wallet
Slice 5  Simulated PSP + webhooks
Slice 6  Reconciliation job
```

**Stack (planned):** Java 21, Spring Boot 3, PostgreSQL, Flyway

**MVP:** Slices 1–3

---

## Status

| Feature | Release | Spec | Status |
|:---|:---:|:---|:---:|
| F0 — Foundation | R0 | [f00](./docs/features/f00-platform-foundation.md) | Not started |
| F1 — Idempotency | R1 | [f01](./docs/features/f01-idempotent-payment-creation.md) | Not started |
| F2 — Lifecycle | R2 | [f02](./docs/features/f02-payment-lifecycle.md) | Not started |
| F3 — Auth/capture/void | R3 (MVP) | [f03](./docs/features/f03-auth-capture-void.md) | Not started |
| F4 — Ledger | R4 | [f04](./docs/features/f04-wallet-ledger.md) | Not started |
| F5 — Webhooks | R5 | [f05](./docs/features/f05-psp-webhooks.md) | Not started |
| F6 — Reconciliation | R6 | [f06](./docs/features/f06-reconciliation.md) | Not started |
| F7–F8 — Optional | R7–R8 | [f07](./docs/features/f07-failure-handling.md) / [f08](./docs/features/f08-checkout-demo.md) | Backlog |

---

## Quick links

- [System Design Sandbox — Payment System](https://www.systemdesignsandbox.com/learn/design-payment-system)
- [Stripe — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)
