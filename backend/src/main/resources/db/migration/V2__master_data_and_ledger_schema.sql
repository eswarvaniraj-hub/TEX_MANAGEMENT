-- =============================================================================
-- V2: Master Data, Chart of Accounts, Ledger, Invoices, Payments, Expenses & Banking Schema
-- =============================================================================

-- 1. Units of Measure
CREATE TABLE unit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    decimal_places INT NOT NULL DEFAULT 0 CONSTRAINT chk_unit_decimals CHECK (decimal_places >= 0 AND decimal_places <= 3),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_unit_code_lower ON unit (LOWER(code));

-- 2. Tax Rates
CREATE TABLE tax_rate (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rate_percent NUMERIC(5,2) NOT NULL CONSTRAINT chk_tax_rate_percent CHECK (rate_percent >= 0 AND rate_percent <= 100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_tax_rate_name_lower ON tax_rate (LOWER(name));

-- 3. Chart of Accounts
CREATE TABLE account (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    account_type VARCHAR(20) NOT NULL CONSTRAINT chk_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE')),
    system_role VARCHAR(50) NULL CONSTRAINT chk_account_system_role CHECK (system_role IN (
        'AR_CONTROL', 'AP_CONTROL', 'SALES', 'PURCHASES_YARN', 'PURCHASES_OTHER',
        'OPENING_EQUITY', 'ROUND_OFF', 'GST_INPUT_CGST', 'GST_INPUT_SGST', 'GST_INPUT_IGST',
        'GST_OUTPUT_CGST', 'GST_OUTPUT_SGST', 'GST_OUTPUT_IGST'
    )),
    report_group VARCHAR(50) NULL,
    parent_id BIGINT NULL REFERENCES account(id) ON DELETE RESTRICT,
    is_postable BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_account_code_lower ON account (LOWER(code));
CREATE UNIQUE INDEX idx_account_system_role ON account (system_role) WHERE system_role IS NOT NULL;

-- 4. Payment Accounts (Cash and Bank)
CREATE TABLE payment_account (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    kind VARCHAR(20) NOT NULL CONSTRAINT chk_payment_account_kind CHECK (kind IN ('CASH', 'BANK')),
    account_id BIGINT NOT NULL UNIQUE REFERENCES account(id) ON DELETE RESTRICT,
    bank_name VARCHAR(100) NULL,
    account_last4 VARCHAR(4) NULL CONSTRAINT chk_account_last4 CHECK (account_last4 IS NULL OR length(account_last4) = 4),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_payment_account_name_lower ON payment_account (LOWER(name));

-- 5. Expense Categories
CREATE TABLE expense_category (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_expense_category_name_lower ON expense_category (LOWER(name));

-- 6. Products
CREATE TABLE product (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    product_type VARCHAR(20) NOT NULL CONSTRAINT chk_product_type CHECK (product_type IN ('YARN', 'FABRIC', 'MATERIAL', 'SERVICE', 'OTHER')),
    unit_id BIGINT NOT NULL REFERENCES unit(id) ON DELETE RESTRICT,
    hsn_code VARCHAR(8) NULL CONSTRAINT chk_product_hsn CHECK (hsn_code IS NULL OR (hsn_code ~ '^[0-9]{4,8}$')),
    default_tax_rate_id BIGINT NULL REFERENCES tax_rate(id) ON DELETE RESTRICT,
    default_sale_rate NUMERIC(14,4) NOT NULL DEFAULT 0 CONSTRAINT chk_sale_rate CHECK (default_sale_rate >= 0),
    default_purchase_rate NUMERIC(14,4) NOT NULL DEFAULT 0 CONSTRAINT chk_purchase_rate CHECK (default_purchase_rate >= 0),
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_product_code_lower ON product (LOWER(code));

-- 7. Customers
CREATE TABLE customer (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    address TEXT NULL,
    city VARCHAR(100) NULL,
    state_code VARCHAR(2) NOT NULL REFERENCES gst_state(code) ON DELETE RESTRICT,
    gstin VARCHAR(15) NULL CONSTRAINT chk_cust_gstin CHECK (gstin IS NULL OR (gstin ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$')),
    pan VARCHAR(10) NULL CONSTRAINT chk_cust_pan CHECK (pan IS NULL OR (pan ~ '^[A-Z]{5}[0-9]{4}[A-Z]{1}$')),
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_customer_code_lower ON customer (LOWER(code));

-- 8. Suppliers
CREATE TABLE supplier (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    supplier_type VARCHAR(20) NOT NULL DEFAULT 'YARN' CONSTRAINT chk_supplier_type CHECK (supplier_type IN ('YARN', 'OTHER')),
    contact_person VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    address TEXT NULL,
    city VARCHAR(100) NULL,
    state_code VARCHAR(2) NOT NULL REFERENCES gst_state(code) ON DELETE RESTRICT,
    gstin VARCHAR(15) NULL CONSTRAINT chk_supp_gstin CHECK (gstin IS NULL OR (gstin ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$')),
    pan VARCHAR(10) NULL CONSTRAINT chk_supp_pan CHECK (pan IS NULL OR (pan ~ '^[A-Z]{5}[0-9]{4}[A-Z]{1}$')),
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_supplier_code_lower ON supplier (LOWER(code));

-- 9. Gapless Document Number Sequences
CREATE TABLE document_sequence (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doc_type VARCHAR(20) NOT NULL CONSTRAINT chk_seq_doc_type CHECK (doc_type IN ('INV', 'PUR', 'REC', 'PAY', 'EXP', 'TRF', 'OTH', 'JV')),
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    prefix VARCHAR(10) NOT NULL,
    next_number BIGINT NOT NULL DEFAULT 1 CONSTRAINT chk_seq_next_number CHECK (next_number >= 1),
    CONSTRAINT uq_seq_doc_type_fy UNIQUE (doc_type, financial_year_id)
);

-- 10. Journal Entries (General Ledger Header)
CREATE TABLE journal_entry (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entry_no VARCHAR(20) NOT NULL UNIQUE,
    entry_date DATE NOT NULL,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    source_type VARCHAR(30) NOT NULL CONSTRAINT chk_je_source_type CHECK (source_type IN (
        'SALES_INVOICE', 'PURCHASE_INVOICE', 'PAYMENT', 'EXPENSE', 'TRANSFER', 'OTHER_TXN', 'OPENING', 'MANUAL'
    )),
    source_id BIGINT NULL,
    description VARCHAR(255) NOT NULL,
    reverses_entry_id BIGINT NULL UNIQUE REFERENCES journal_entry(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL
);
CREATE INDEX idx_je_entry_date ON journal_entry (entry_date);
CREATE INDEX idx_je_source ON journal_entry (source_type, source_id);

-- 11. Journal Lines (General Ledger Line Items)
CREATE TABLE journal_line (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    line_no INT NOT NULL,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    debit NUMERIC(14,2) NOT NULL DEFAULT 0 CONSTRAINT chk_jl_debit CHECK (debit >= 0),
    credit NUMERIC(14,2) NOT NULL DEFAULT 0 CONSTRAINT chk_jl_credit CHECK (credit >= 0),
    customer_id BIGINT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    supplier_id BIGINT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
    narration VARCHAR(255) NULL,
    CONSTRAINT uq_jl_entry_line UNIQUE (journal_entry_id, line_no),
    CONSTRAINT chk_jl_one_nonzero CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)),
    CONSTRAINT chk_jl_not_both_parties CHECK (NOT (customer_id IS NOT NULL AND supplier_id IS NOT NULL))
);
CREATE INDEX idx_jl_account ON journal_line (account_id);
CREATE INDEX idx_jl_customer ON journal_line (customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_jl_supplier ON journal_line (supplier_id) WHERE supplier_id IS NOT NULL;

-- 12. Sales Invoices
CREATE TABLE sales_invoice (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_no VARCHAR(20) NULL,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    invoice_date DATE NOT NULL,
    due_date DATE NULL,
    place_of_supply_state_code VARCHAR(2) NOT NULL REFERENCES gst_state(code) ON DELETE RESTRICT,
    is_interstate BOOLEAN NOT NULL DEFAULT FALSE,
    customer_name_snapshot VARCHAR(150) NOT NULL,
    customer_gstin_snapshot VARCHAR(15) NULL,
    customer_address_snapshot TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CONSTRAINT chk_si_status CHECK (status IN ('DRAFT', 'POSTED', 'REVERSED')),
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    taxable_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    cgst_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    sgst_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    igst_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    round_off NUMERIC(14,2) NOT NULL DEFAULT 0 CONSTRAINT chk_si_roundoff CHECK (round_off >= -0.50 AND round_off <= 0.50),
    grand_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    notes TEXT NULL,
    journal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversed_at TIMESTAMPTZ NULL,
    reversal_reason VARCHAR(255) NULL,
    posted_at TIMESTAMPTZ NULL,
    posted_by VARCHAR(50) NULL,
    idempotency_key UUID NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_si_no_fy UNIQUE (invoice_no, financial_year_id)
);
CREATE INDEX idx_si_customer ON sales_invoice (customer_id);
CREATE INDEX idx_si_date ON sales_invoice (invoice_date);
CREATE INDEX idx_si_status ON sales_invoice (status);

CREATE TABLE sales_invoice_line (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES sales_invoice(id) ON DELETE CASCADE,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    description VARCHAR(255) NULL,
    hsn_code VARCHAR(8) NULL,
    quantity NUMERIC(14,3) NOT NULL CONSTRAINT chk_sil_qty CHECK (quantity > 0),
    unit_id BIGINT NOT NULL REFERENCES unit(id) ON DELETE RESTRICT,
    rate NUMERIC(14,4) NOT NULL CONSTRAINT chk_sil_rate CHECK (rate >= 0),
    line_amount NUMERIC(14,2) NOT NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    taxable_value NUMERIC(14,2) NOT NULL,
    tax_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    cgst NUMERIC(14,2) NOT NULL DEFAULT 0,
    sgst NUMERIC(14,2) NOT NULL DEFAULT 0,
    igst NUMERIC(14,2) NOT NULL DEFAULT 0,
    line_total NUMERIC(14,2) NOT NULL,
    CONSTRAINT uq_sil_invoice_line UNIQUE (invoice_id, line_no)
);

-- 13. Purchase Invoices
CREATE TABLE purchase_invoice (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_no VARCHAR(20) NULL,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
    supplier_invoice_no VARCHAR(50) NOT NULL,
    supplier_invoice_no_normalized VARCHAR(50) NOT NULL,
    supplier_invoice_date DATE NOT NULL,
    purchase_date DATE NOT NULL,
    supplier_name_snapshot VARCHAR(150) NOT NULL,
    supplier_gstin_snapshot VARCHAR(15) NULL,
    supplier_address_snapshot TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CONSTRAINT chk_pi_status CHECK (status IN ('DRAFT', 'POSTED', 'REVERSED')),
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    taxable_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    cgst_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    sgst_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    igst_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    round_off NUMERIC(14,2) NOT NULL DEFAULT 0 CONSTRAINT chk_pi_roundoff CHECK (round_off >= -0.50 AND round_off <= 0.50),
    grand_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    notes TEXT NULL,
    journal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversed_at TIMESTAMPTZ NULL,
    reversal_reason VARCHAR(255) NULL,
    posted_at TIMESTAMPTZ NULL,
    posted_by VARCHAR(50) NULL,
    idempotency_key UUID NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_pi_voucher_fy UNIQUE (voucher_no, financial_year_id)
);
CREATE UNIQUE INDEX idx_pi_supplier_dup ON purchase_invoice (supplier_id, supplier_invoice_no_normalized) WHERE status IN ('DRAFT', 'POSTED');
CREATE INDEX idx_pi_supplier ON purchase_invoice (supplier_id);
CREATE INDEX idx_pi_date ON purchase_invoice (purchase_date);
CREATE INDEX idx_pi_status ON purchase_invoice (status);

CREATE TABLE purchase_invoice_line (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES purchase_invoice(id) ON DELETE CASCADE,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    description VARCHAR(255) NULL,
    hsn_code VARCHAR(8) NULL,
    quantity NUMERIC(14,3) NOT NULL CONSTRAINT chk_pil_qty CHECK (quantity > 0),
    unit_id BIGINT NOT NULL REFERENCES unit(id) ON DELETE RESTRICT,
    rate NUMERIC(14,4) NOT NULL CONSTRAINT chk_pil_rate CHECK (rate >= 0),
    line_amount NUMERIC(14,2) NOT NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    taxable_value NUMERIC(14,2) NOT NULL,
    tax_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    cgst NUMERIC(14,2) NOT NULL DEFAULT 0,
    sgst NUMERIC(14,2) NOT NULL DEFAULT 0,
    igst NUMERIC(14,2) NOT NULL DEFAULT 0,
    line_total NUMERIC(14,2) NOT NULL,
    CONSTRAINT uq_pil_invoice_line UNIQUE (invoice_id, line_no)
);

-- 14. Opening Balance Items
CREATE TABLE opening_balance_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    supplier_id BIGINT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
    doc_date DATE NOT NULL,
    reference VARCHAR(50) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CONSTRAINT chk_obi_amount CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CONSTRAINT chk_obi_status CHECK (status IN ('POSTED', 'REVERSED')),
    journal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_obi_party CHECK (
        (customer_id IS NOT NULL AND supplier_id IS NULL) OR
        (supplier_id IS NOT NULL AND customer_id IS NULL)
    )
);

-- 15. Payments & Receipts
CREATE TABLE payment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_no VARCHAR(20) NOT NULL UNIQUE,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    direction VARCHAR(20) NOT NULL CONSTRAINT chk_pmt_direction CHECK (direction IN ('RECEIPT', 'PAYMENT')),
    customer_id BIGINT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    supplier_id BIGINT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
    payment_date DATE NOT NULL,
    payment_account_id BIGINT NOT NULL REFERENCES payment_account(id) ON DELETE RESTRICT,
    amount NUMERIC(14,2) NOT NULL CONSTRAINT chk_pmt_amount CHECK (amount > 0),
    method VARCHAR(20) NOT NULL CONSTRAINT chk_pmt_method CHECK (method IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'OTHER')),
    reference_no VARCHAR(50) NULL,
    notes TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CONSTRAINT chk_pmt_status CHECK (status IN ('POSTED', 'REVERSED')),
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversed_at TIMESTAMPTZ NULL,
    reversal_reason VARCHAR(255) NULL,
    corrected_from_id BIGINT NULL REFERENCES payment(id) ON DELETE RESTRICT,
    idempotency_key UUID NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pmt_party CHECK (
        (direction = 'RECEIPT' AND customer_id IS NOT NULL AND supplier_id IS NULL) OR
        (direction = 'PAYMENT' AND supplier_id IS NOT NULL AND customer_id IS NULL)
    )
);
CREATE INDEX idx_pmt_party ON payment (direction, customer_id, supplier_id);
CREATE INDEX idx_pmt_date ON payment (payment_date);

-- 16. Payment Allocations
CREATE TABLE payment_allocation (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payment(id) ON DELETE RESTRICT,
    sales_invoice_id BIGINT NULL REFERENCES sales_invoice(id) ON DELETE RESTRICT,
    purchase_invoice_id BIGINT NULL REFERENCES purchase_invoice(id) ON DELETE RESTRICT,
    opening_balance_item_id BIGINT NULL REFERENCES opening_balance_item(id) ON DELETE RESTRICT,
    amount NUMERIC(14,2) NOT NULL CONSTRAINT chk_alloc_amount CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_alloc_target CHECK (
        (sales_invoice_id IS NOT NULL AND purchase_invoice_id IS NULL AND opening_balance_item_id IS NULL) OR
        (purchase_invoice_id IS NOT NULL AND sales_invoice_id IS NULL AND opening_balance_item_id IS NULL) OR
        (opening_balance_item_id IS NOT NULL AND sales_invoice_id IS NULL AND purchase_invoice_id IS NULL)
    )
);
CREATE INDEX idx_alloc_sales_inv ON payment_allocation (sales_invoice_id);
CREATE INDEX idx_alloc_purchase_inv ON payment_allocation (purchase_invoice_id);
CREATE INDEX idx_alloc_obi ON payment_allocation (opening_balance_item_id);

-- 17. Expenses
CREATE TABLE expense (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expense_no VARCHAR(20) NOT NULL UNIQUE,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    expense_date DATE NOT NULL,
    category_id BIGINT NOT NULL REFERENCES expense_category(id) ON DELETE RESTRICT,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CONSTRAINT chk_exp_amount CHECK (amount > 0),
    payment_account_id BIGINT NOT NULL REFERENCES payment_account(id) ON DELETE RESTRICT,
    reference_no VARCHAR(50) NULL,
    notes TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CONSTRAINT chk_exp_status CHECK (status IN ('POSTED', 'REVERSED')),
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversed_at TIMESTAMPTZ NULL,
    reversal_reason VARCHAR(255) NULL,
    corrected_from_id BIGINT NULL REFERENCES expense(id) ON DELETE RESTRICT,
    idempotency_key UUID NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_expense_date ON expense (expense_date);
CREATE INDEX idx_expense_category ON expense (category_id);

-- 18. Cash / Bank Transfers
CREATE TABLE fund_transfer (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transfer_no VARCHAR(20) NOT NULL UNIQUE,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    transfer_date DATE NOT NULL,
    from_payment_account_id BIGINT NOT NULL REFERENCES payment_account(id) ON DELETE RESTRICT,
    to_payment_account_id BIGINT NOT NULL REFERENCES payment_account(id) ON DELETE RESTRICT,
    amount NUMERIC(14,2) NOT NULL CONSTRAINT chk_trf_amount CHECK (amount > 0),
    reference VARCHAR(50) NULL,
    notes TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CONSTRAINT chk_trf_status CHECK (status IN ('POSTED', 'REVERSED')),
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversed_at TIMESTAMPTZ NULL,
    reversal_reason VARCHAR(255) NULL,
    corrected_from_id BIGINT NULL REFERENCES fund_transfer(id) ON DELETE RESTRICT,
    idempotency_key UUID NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_trf_diff_accounts CHECK (from_payment_account_id <> to_payment_account_id)
);

-- 19. Other Non-Operating Transactions
CREATE TABLE other_transaction (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    txn_no VARCHAR(20) NOT NULL UNIQUE,
    financial_year_id BIGINT NOT NULL REFERENCES financial_year(id) ON DELETE RESTRICT,
    kind VARCHAR(30) NOT NULL CONSTRAINT chk_oth_kind CHECK (kind IN ('OWNER_CAPITAL', 'OWNER_DRAWINGS', 'LOAN_RECEIVED', 'LOAN_REPAYMENT', 'ASSET_PURCHASE')),
    txn_date DATE NOT NULL,
    payment_account_id BIGINT NOT NULL REFERENCES payment_account(id) ON DELETE RESTRICT,
    counter_account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
    amount NUMERIC(14,2) NOT NULL CONSTRAINT chk_oth_amount CHECK (amount > 0),
    reference_no VARCHAR(50) NULL,
    notes TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CONSTRAINT chk_oth_status CHECK (status IN ('POSTED', 'REVERSED')),
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversal_entry_id BIGINT NULL REFERENCES journal_entry(id) ON DELETE RESTRICT,
    reversed_at TIMESTAMPTZ NULL,
    reversal_reason VARCHAR(255) NULL,
    idempotency_key UUID NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- =============================================================================
-- Database Level Integrity Safeguards
-- =============================================================================

-- Immutability Trigger for Journal Entries and Lines
CREATE OR REPLACE FUNCTION fn_prevent_journal_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Posted general ledger records are immutable and cannot be modified or deleted.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_entry_immutable
BEFORE UPDATE OR DELETE ON journal_entry
FOR EACH ROW
EXECUTE FUNCTION fn_prevent_journal_modification();

CREATE TRIGGER trg_journal_line_immutable
BEFORE UPDATE OR DELETE ON journal_line
FOR EACH ROW
EXECUTE FUNCTION fn_prevent_journal_modification();

-- Deferred Double-Entry Balance Check Constraint Function
CREATE OR REPLACE FUNCTION fn_check_journal_entry_balance()
RETURNS TRIGGER AS $$
DECLARE
    entry_rec RECORD;
    v_count INT;
    v_debit NUMERIC(14,2);
    v_credit NUMERIC(14,2);
BEGIN
    FOR entry_rec IN (
        SELECT DISTINCT journal_entry_id FROM new_table
    ) LOOP
        SELECT count(*), COALESCE(sum(debit), 0), COALESCE(sum(credit), 0)
        INTO v_count, v_debit, v_credit
        FROM journal_line
        WHERE journal_entry_id = entry_rec.journal_entry_id;

        IF v_count < 2 THEN
            RAISE EXCEPTION 'Journal entry % must have at least 2 lines (found %)', entry_rec.journal_entry_id, v_count;
        END IF;

        IF v_debit <> v_credit THEN
            RAISE EXCEPTION 'Journal entry % is unbalanced: Total Debits (%) != Total Credits (%)',
                entry_rec.journal_entry_id, v_debit, v_credit;
        END IF;
    END LOOP;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_line_balance_check
AFTER INSERT ON journal_line
REFERENCING NEW TABLE AS new_table
FOR EACH STATEMENT
EXECUTE FUNCTION fn_check_journal_entry_balance();
