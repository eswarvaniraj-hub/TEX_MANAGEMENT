# Textile Accounts Management System

A production-grade, single-owner accounts management system tailored for textile manufacturing operations in Tamil Nadu, India. Built with strict double-entry general ledger accounting principles, immutable audit logging, Indian GST compliance, and zero external payment dependencies.

---

## Technology Stack

- **Backend:** Java 21 LTS, Spring Boot 4.1.1.RELEASE
  - Spring Data JPA & Hibernate 7 (`spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`)
  - Spring Security (deny-by-default architecture)
  - Flyway Database Migrations
  - Spring Session JDBC (`spring.session.jdbc.initialize-schema=never`, schema managed via Flyway V1)
  - ArchUnit (architectural and module boundary verification)
  - Testcontainers (PostgreSQL 16 container for Docker/CI automated runs; direct Windows PostgreSQL fallback when Docker is absent)
- **Frontend:** TypeScript, React 18.3.1
  - Mantine UI 7.17.1 (`@mantine/core`, `@mantine/hooks`)
  - Tabler Icons (`@tabler/icons-react`)
  - TanStack React Query 5.62.7
  - Vite 6.0.7 & Vitest 2.1.8
  - ESLint 9 & Prettier
- **Database:** PostgreSQL (uses your existing Windows PostgreSQL installation directly)
- **Architecture:** Monolith with strict package modularity; immutable posted ledger records; server-side monetary calculations using `NUMERIC` / `BigDecimal`.

---

## Architecture Principles & Guardrails

1. **Double-Entry General Ledger:** The sole source of accounting truth. Balances are computed dynamically from `journal_line`, never stored as running balances.
2. **Immutable Lifecycle:** Documents follow `DRAFT -> POSTED -> REVERSED`. Once posted, records cannot be edited or deleted. Corrections require posting a full reversal document.
3. **Monetary Precision:** All currency amounts are strictly `NUMERIC` in PostgreSQL and `BigDecimal` in Java. In JSON APIs, monetary values are serialized as string representations to avoid floating-point errors.
4. **Isolated Ledger Package:** Only the `ledger` module may write to `journal_entry` and `journal_line` tables (enforced by ArchUnit tests and PostgreSQL triggers).
5. **No Payment Gateway / External Payment APIs:** All receipts and payments are internal accounting entries against bank/cash ledger accounts. No external payment processors or third-party webhooks are permitted.
6. **Zero Secrets in Code/Git:** Database credentials and secrets are managed strictly through environment variables. Real credentials are never committed.

---

## Local Development Setup (Windows Native PostgreSQL)

No Docker Desktop is required for local development. The application connects directly to your locally installed Windows PostgreSQL service.

### Prerequisites

- **Java Development Kit (JDK):** Version 21 (Temurin, OpenJDK 21, or Android Studio JBR)
- **Node.js:** Version 20+ (LTS) or Node 22 with `npm`
- **PostgreSQL:** Locally installed on Windows (PostgreSQL 16, 17, or 18)

---

### Step 1: Verify PostgreSQL Windows Service is Running

Open Windows PowerShell and check that the PostgreSQL service is active:

```powershell
Get-Service -Name "*postgres*"
```

If the service status is `Stopped`, start it with:

```powershell
Start-Service -Name postgresql-x64-18
# Or open services.msc and start "postgresql-x64-xx"
```

To verify the port PostgreSQL is listening on (default is `5432`):

```powershell
Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -in 5432, 5433 }
```

---

### Step 2: Create the Application Database (If Not Already Created)

The application requires a dedicated database (default: `textile_accounts`).

#### Option A: Using `psql` (PowerShell)

```powershell
# Connect to your local PostgreSQL server (you will be prompted for your password):
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h 127.0.0.1 -p 5432 -c "CREATE DATABASE textile_accounts;"
```

#### Option B: Using pgAdmin

1. Open **pgAdmin** and connect to your local server.
2. In the left browser tree, right-click **Databases** -> **Create** -> **Database...**
3. In the **Database** field, enter `textile_accounts`.
4. Click **Save**.

*(Note: Flyway will automatically create all tables, indexes, and initial master data on first application startup).*

---

### Step 3: Configure Local Environment Variables

1. Copy `.env.example` to `.env` in the project root (if not already created):

```powershell
Copy-Item .env.example .env
```

2. Open `.env` and set your local PostgreSQL credentials:

```env
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_DB=textile_accounts
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_actual_postgres_password
```

> **Note:** `.env` is listed in `.gitignore` and will never be committed to Git.

---

### Step 4: Start the Spring Boot Backend

Open a Windows PowerShell terminal in the `backend/` directory:

```powershell
cd C:\Users\Eswar\Desktop\Tex_Arun\backend

# 1. Load credentials from .env into the current PowerShell session:
Get-Content ..\.env | Where-Object { $_ -match '^\s*([^#\s]+)\s*=\s*(.*)$' } | ForEach-Object {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), "Process")
}

# 2. Ensure JAVA_HOME points to your JDK 21 installation (if not set in system environment):
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# 3. Launch Spring Boot in dev profile:
.\mvnw.cmd spring-boot:run
```

The backend starts on port `8080`.
- Flyway automatically validates and applies `V1__initial_foundation.sql`.
- Hibernate schema validation verifies all entities against the database (`ddl-auto=validate`).
- Public health check: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)

---

### Step 5: Verify the Backend Health Endpoint

In another PowerShell window, verify that the backend is running and connected to PostgreSQL:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health"
```

Expected response:
```json
{
  "status": "UP"
}
```

---

### Step 6: Start the React Frontend

Open a new Windows PowerShell terminal in the `frontend/` directory:

```powershell
cd C:\Users\Eswar\Desktop\Tex_Arun\frontend

# Install dependencies (first time only)
npm install

# Start Vite dev server
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.
Vite automatically proxies `/api/*` requests to `http://localhost:8080`, allowing the frontend to query `/api/v1/health` seamlessly.

---

## Running Tests

### Backend Tests

To run the ArchUnit architecture tests (no database or Docker required):

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\mvnw.cmd test -Dtest=LayerArchitectureTest
```

To run the complete test suite against your local Windows PostgreSQL instance:

```powershell
cd backend
Get-Content ..\.env | Where-Object { $_ -match '^\s*([^#\s]+)\s*=\s*(.*)$' } | ForEach-Object {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), "Process")
}
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\mvnw.cmd test
```

> **Docker / Testcontainers Note:** When running in GitHub Actions CI (or on machines with Docker available), `AbstractIntegrationTest` automatically spins up a shared PostgreSQL 16 Testcontainers container. When Docker is not installed locally, the test suite falls back cleanly to your locally running PostgreSQL database using the `POSTGRES_*` environment variables.

### Frontend Quality Checks

In `frontend/`:

```powershell
# Run Vitest unit tests
npm run test:run

# Run ESLint checks
npm run lint

# Verify Prettier formatting
npm run format

# Production bundle build
npm run build
```

---

## Optional: Docker Compose

The file `deploy/docker-compose.dev.yml` remains available in the repository for environments where containerized development is preferred or for testing against isolated PostgreSQL containers.
