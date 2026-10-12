# Accounting Conventions & Rules

This document details the accounting principles and system rules for the Textile Accounts Management System.

## 1. Core Principles
- **Double-entry book-keeping:** Every journal entry consists of 2 or more lines.
- **Strict balance constraint:** Total Debits = Total Credits for every transaction.
- **Normal Balances:**
  - Assets and Expenses increase with a **Debit**.
  - Liabilities, Equity, and Income increase with a **Credit**.
- **Dynamic Ledger Balances:** Account balances are never stored as static columns. They are always computed dynamically from `journal_line`.

## 2. Control Accounts (Receivables & Payables)
- **Customer Ledger (Accounts Receivable):**
  - Debit = Invoice issued (customer owes money).
  - Credit = Payment received (reduces what customer owes).
  - Positive balance (Dr) = Receivable from customer.
  - Negative balance (Cr) = Customer advance.
- **Supplier Ledger (Accounts Payable):**
  - Credit = Purchase bill received (we owe money).
  - Debit = Payment made to supplier.
  - Positive balance (Cr) = Payable to supplier.
  - Negative balance (Dr) = Advance paid to supplier.
- Party ledger lines are tagged with `customer_id` / `supplier_id` on the control account lines.
- The sum of all individual customer balances must equal the Accounts Receivable control account balance at all times.

## 3. Financial Year & Timezone
- Indian Financial Year runs from 1 April to 31 March (e.g., `2026-27`).
- All business dates are stored as `LocalDate`.
- System time is derived from an injected `Clock` bean set to the `Asia/Kolkata` time zone.

## 4. Document Lifecycle & Immutability
- **DRAFT:** Editable, deletable, no journal entry, no final number sequence assigned.
- **POSTED:** Immutable. Generates a balanced journal entry and gapless document number inside a single database transaction. Cannot be edited or deleted.
- **REVERSED:** An exact mirror reversing journal entry is posted. Original document remains marked as REVERSED ("Cancelled") for complete audit safety.
