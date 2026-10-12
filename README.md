# Textile Accounts Management System

A production-grade, single-owner accounts management system tailored for textile manufacturing operations in Tamil Nadu, India. Built with strict double-entry general ledger accounting principles, immutable audit logging, Indian GST compliance, and zero external payment dependencies.

---

## Technology Stack

- **Backend:** Java 21 LTS, Spring Boot 4.1.1.RELEASE
  - Spring Data JPA & Hibernate 7 (with `ddl-auto=validate`)
  - Spring Security (deny-by-default architecture)
  - Flyway Database Migrations
  - Spring Session JDBC (schema managed via Flyway)
  - ArchUnit (architectural and module boundary verification)
  - Testcontainers (PostgreSQL 16 shared container for integration tests)
- **Frontend:** TypeScript, React 18.3.1
  - Mantine UI 7.17.1 (`@mantine/core`, `@mantine/hooks`)
  - Tabler Icons (`@tabler/icons-react`)
  - TanStack React Query 5.62.7
  - Vite 6.0.7
  - Vitest 2.1.8 & Testing Library
  - ESLint 9 & Prettier
- **Database:** PostgreSQL 16
- **Architecture:** Monolith with strict package modularity; immutable posted ledger records; server-side monetary calculations using `NUMERIC` / `BigDecimal`.

---

## Architecture Principles & Guardrails

1. **Double-Entry General Ledger:** The sole source of accounting truth. Balances are computed dynamically from `journal_line`, never stored as running balances.
2. **Immutable Lifecycle:** Documents follow `DRAFT -> POSTED -> REVERSED`. Once posted, records cannot be edited or deleted. Corrections require posting a full reversal document.
3. **Monetary Precision:** All currency amounts are strictly `NUMERIC` in PostgreSQL and `BigDecimal` in Java. In JSON APIs, monetary values are serialized as string representations to avoid floating-point errors.
4. **Isolated Ledger Package:** Only the `ledger` module may write to `journal_entry` and `journal_line` tables (enforced by ArchUnit tests and PostgreSQL triggers).
5. **No Payment Gateway / External Payment APIs:** All receipts and payments are internal accounting entries against bank/cash ledger accounts. No external payment processors or third-party webhooks are permitted.
6. **Zero Secrets in Code/Git:** Database credentials and secrets are managed strictly through environment variables.

---

## Local Development Setup

### Prerequisites

- **Java Development Kit (JDK):** Version 21 (Temurin or OpenJDK 21)
- **Node.js:** Version 20+ (LTS) or Node 22 with `npm`
- **Docker & Docker Compose:** For running the local PostgreSQL service

### 1. Configure Environment Variables

Copy the example environment configuration into `.env` at the root of the project:

```bash
cp .env.example .env
```

The `.env` file configures the local PostgreSQL container:
- `POSTGRES_USER`: Database user (default: `textile_user`)
- `POSTGRES_PASSWORD`: Database password (default: `textile_password_change_me`)
- `POSTGRES_DB`: Database name (default: `textile_accounts`)
- `POSTGRES_PORT`: Exposed host port (default: `5432`)

### 2. Start PostgreSQL via Docker Compose

Run the local development database:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d
```

> **Security Note:** The PostgreSQL port is bound to `127.0.0.1:5432` only, preventing external network exposure on development machines.

### 3. How the Backend Reads DB Credentials in Dev

In `application.yml` and `application-dev.yml`, Spring Boot is configured to read database connection details from environment variables with fallback defaults that mirror `.env.example`:

- `spring.datasource.url`: `jdbc:postgresql://127.0.0.1:${POSTGRES_PORT:5432}/${POSTGRES_DB:textile_accounts}`
- `spring.datasource.username`: `${POSTGRES_USER:textile_user}`
- `spring.datasource.password`: `${POSTGRES_PASSWORD:textile_password_change_me}`

If you set custom values in `.env`, export them in your terminal before launching the backend:

```bash
# Windows PowerShell
$env:POSTGRES_USER="textile_user"
$env:POSTGRES_PASSWORD="your_password"

# Linux / macOS
export POSTGRES_USER=textile_user
export POSTGRES_PASSWORD=your_password
```

### 4. Run the Backend

Navigate to `backend/` and use the Maven Wrapper:

```bash
cd backend
./mvnw spring-boot:run
# On Windows PowerShell:
# .\mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:8080`.
- Public Health Check: `http://localhost:8080/api/v1/health` (returns `{"status":"UP"}`)
- Internal Actuator: `http://localhost:8080/actuator/health` (returns `{"status":"UP"}` without details)
- All other endpoints deny unauthenticated access by default.

### 5. Run the Frontend

In a separate terminal, navigate to `frontend/`:

```bash
cd frontend
npm install
npm run dev
```

The frontend Vite dev server will start on `http://localhost:5173`.
Vite proxies all `/api/*` requests directly to `http://localhost:8080`, allowing seamless local development without CORS complications.

---

## Running Tests

### Backend Tests (Flyway, Testcontainers, ArchUnit)

The backend test suite utilizes a single shared PostgreSQL container via Testcontainers. To execute all unit, integration, and architecture tests:

```bash
cd backend
./mvnw clean verify
# On Windows PowerShell:
# .\mvnw.cmd clean verify
```

### Frontend Tests & Quality Checks

```bash
cd frontend
# Run unit tests via Vitest
npm run test:run

# Run ESLint code checks
npm run lint

# Check formatting via Prettier
npm run format

# Validate production TypeScript build
npm run build
```

---

## Continuous Integration (CI)

A GitHub Actions pipeline is configured in `.github/workflows/ci.yml`. It runs on every push and pull request to `main`, validating:
1. Java 21 compilation, Flyway schema validation, Spring context boot, Security tests, and ArchUnit rules against PostgreSQL Testcontainers.
2. Frontend clean install (`npm ci`), ESLint checks, Vitest test suite, and Vite production bundle compilation.
