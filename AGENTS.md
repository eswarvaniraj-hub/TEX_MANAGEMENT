# AGENTS.md — Rules and Guidelines for AI Development

## 1. Operating Protocol
1. **Work ONE phase at a time**: At the start of a phase, restate its goal and completion criteria, list the files to be created or changed, and WAIT for user approval.
2. **Phase Completion**: At the end of a phase, run all tests, show a results summary with real console output, and show the completion-criteria checklist with evidence. Do NOT start the next phase without explicit user approval.
3. **No Silent Redesigns**: This repository and `docs/IMPLEMENTATION_PLAN.md` define the system design. If a bug, contradiction, or superior alternative is identified, stop, explain in 10 lines or fewer, recommend one option, and wait.
4. **Plain Language**: Explain everything in simple terms. Explain each accounting term the first time it is introduced.

## 2. Hard Engineering Rules
1. **Money & Precision**:
   - Money is strictly `NUMERIC` in PostgreSQL and `BigDecimal` in Java. Never float or double.
   - In JSON APIs, monetary values and quantities are serialized as strings to preserve precision.
   - Calculations and GST rounding are strictly server-side. The frontend never performs financial arithmetic.
2. **Database & Migrations**:
   - Flyway is the sole schema authority. Never edit an applied migration; always add a new `V` migration file.
   - `spring.jpa.hibernate.ddl-auto=validate` and `spring.jpa.open-in-view=false`.
   - `spring.session.jdbc.initialize-schema=never` (schema defined in Flyway V1).
3. **Architecture & Layering**:
   - Layering: `Controller -> Service -> Repository`.
   - Controllers must never access repositories directly. Services may call other services.
   - Only the `ledger` module may write to `journal_entry` and `journal_line` (enforced by ArchUnit tests and DB triggers).
   - DTOs are Java records, separate from JPA entities. No business logic in controllers.
   - `@Transactional` only on service methods.
4. **Time & Environment**:
   - Time comes strictly from an injected `Clock` bean set to `Asia/Kolkata`.
   - Business dates are `LocalDate`.
5. **Security & Secrets**:
   - Zero secrets in code, logs, frontend, or git. `.env` is git-ignored; `.env.example` holds safe placeholders only.
   - Deny by default: unauthenticated requests to protected `/api/**` return 401 via `HttpStatusEntryPoint`.
   - Passwords use `DelegatingPasswordEncoder` with `BCryptPasswordEncoder(12)` default.
   - No external payment gateways or third-party payment APIs of any kind. All receipts and payments are internal accounting entries.
