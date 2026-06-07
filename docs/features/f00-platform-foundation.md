# F0 — Platform Foundation

| Field | Value |
|---|---|
| **Feature ID** | F0 |
| **Release** | R0 |
| **Status** | Ready to build |
| **Depends on** | — |
| **Unlocks** | F1 |
| **Est. effort** | ~½ weekend |

---

## Goal

Provide a runnable application shell — build, database, migrations, API conventions, and test harness — so every payment feature adds domain logic without re-solving infrastructure.

---

## User stories

### F0-1 — Project scaffold

**As a** developer  
**I want** a Spring Boot project that builds and starts  
**So that** I can add payment features incrementally

**Acceptance criteria**

- Given the repo is cloned
- When `mvn clean verify` runs
- Then the build succeeds with no tests failing (or only skipped integration until DB up)
- And `mvn spring-boot:run` starts the application on port 8080

### F0-2 — Health & readiness

**As an** operator  
**I want** health endpoints  
**So that** I know the app and database are alive

**Acceptance criteria**

- `GET /actuator/health` returns `{"status":"UP"}` when DB connected
- Health returns `DOWN` when PostgreSQL unavailable

### F0-3 — Database & migrations

**As a** developer  
**I want** Flyway migrations on startup  
**So that** schema is reproducible

**Acceptance criteria**

- `docker compose up -d postgres` starts PostgreSQL 16
- App applies migrations from `src/main/resources/db/migration/`
- Baseline migration `V1__baseline.sql` exists (can be empty comment only)

### F0-4 — API conventions

**As an** API consumer  
**I want** consistent errors and documentation  
**So that** integration is predictable

**Acceptance criteria**

- Validation errors return `{ "error": "...", "code": "...", "correlationId": "..." }`
- Correlation ID generated if `X-Correlation-Id` absent; echoed in response header
- OpenAPI UI available at `/swagger-ui.html`

### F0-5 — Test harness

**As a** developer  
**I want** Testcontainers for integration tests  
**So that** tests run against real PostgreSQL

**Acceptance criteria**

- One `@SpringBootTest` smoke test loads context with Testcontainers PostgreSQL
- Test passes locally without manual DB setup

---

## Business rules

| Rule | Detail |
|---|---|
| BR-F0-1 | Java 21, Spring Boot 3.x |
| BR-F0-2 | Package root: `com.payflow` |
| BR-F0-3 | API prefix: `/api/v1` for domain endpoints |
| BR-F0-4 | Secrets via env vars / `application-local.yml` — never committed |
| BR-F0-5 | Structured JSON logging in non-local profiles |

---

## Technical deliverables

### Repository layout

```
payflow/
├── pom.xml
├── docker-compose.yml
├── src/main/java/com/payflow/
│   ├── PayflowApplication.java
│   ├── config/
│   ├── api/          # controllers (empty until F1)
│   └── domain/       # services (empty until F1)
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
└── src/test/java/
```

### docker-compose.yml (minimal)

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: payflow
      POSTGRES_USER: payflow
      POSTGRES_PASSWORD: payflow
    ports:
      - "5432:5432"
```

### Dependencies (Maven)

- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-validation
- spring-boot-starter-data-jpa
- flyway-core + postgresql driver
- springdoc-openapi-starter-webmvc-ui
- test: spring-boot-starter-test, testcontainers postgresql

---

## API contract (F0 only)

| Method | Path | Response |
|---|---|---|
| GET | `/actuator/health` | Spring Actuator health |
| GET | `/swagger-ui.html` | OpenAPI UI |

No payment endpoints in F0.

---

## Data model (F0)

No domain tables. Optional Flyway `V1__baseline.sql`:

```sql
-- PayFlow baseline — domain tables added in F1+
SELECT 1;
```

---

## Test scenarios

| # | Scenario | Expected |
|:---:|---|---|
| T0-1 | Context loads with Testcontainers | Test green |
| T0-2 | Health endpoint | 200 UP |
| T0-3 | Flyway migrates on startup | No migration errors in logs |

---

## Definition of done

- [ ] README: clone → compose → run documented
- [ ] `mvn verify` passes
- [ ] Health check UP with postgres running
- [ ] OpenAPI page loads

---

## Out of scope

- Payment domain tables or APIs
- Authentication / authorization
- Kafka, Redis, multi-module split

---

## PO note template

**Problem:** Need a stable base before adding payment correctness features.

**Decision:** Single-module Spring Boot + Postgres + Flyway for simplicity.

**User impact:** N/A (internal dev enabler).

**Metrics:** Build time, test pass rate.
