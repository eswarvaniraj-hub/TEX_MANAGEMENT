-- =============================================================================
-- V3: Seed Provisional Chart of Accounts, Units, Payment Accounts & Sequences
-- PROVISIONAL — SUBJECT TO CHARTERED ACCOUNTANT REVIEW
-- =============================================================================

-- 1. Standard Textile Units of Measure
INSERT INTO unit (code, name, decimal_places, active) VALUES
('kg', 'Kilogram', 3, TRUE),
('m', 'Metre', 3, TRUE),
('roll', 'Roll', 0, TRUE),
('pcs', 'Pieces', 0, TRUE);

-- 2. Provisional Chart of Accounts
-- 13 System Roles + Standard Ledger Accounts
INSERT INTO account (code, name, account_type, system_role, report_group, is_postable, is_system, active) VALUES
-- Assets
('1010', 'Cash in Hand', 'ASSET', NULL, 'CURRENT_ASSETS', TRUE, FALSE, TRUE),
('1020', 'Main Bank Account', 'ASSET', NULL, 'CURRENT_ASSETS', TRUE, FALSE, TRUE),
('1100', 'Accounts Receivable', 'ASSET', 'AR_CONTROL', 'CURRENT_ASSETS', TRUE, TRUE, TRUE),
('1210', 'GST Input CGST', 'ASSET', 'GST_INPUT_CGST', 'CURRENT_ASSETS', TRUE, TRUE, TRUE),
('1220', 'GST Input SGST', 'ASSET', 'GST_INPUT_SGST', 'CURRENT_ASSETS', TRUE, TRUE, TRUE),
('1230', 'GST Input IGST', 'ASSET', 'GST_INPUT_IGST', 'CURRENT_ASSETS', TRUE, TRUE, TRUE),
('1500', 'Machinery & Fixed Assets', 'ASSET', NULL, 'FIXED_ASSETS', TRUE, FALSE, TRUE),

-- Liabilities
('2100', 'Accounts Payable', 'LIABILITY', 'AP_CONTROL', 'CURRENT_LIABILITIES', TRUE, TRUE, TRUE),
('2210', 'GST Output CGST', 'LIABILITY', 'GST_OUTPUT_CGST', 'CURRENT_LIABILITIES', TRUE, TRUE, TRUE),
('2220', 'GST Output SGST', 'LIABILITY', 'GST_OUTPUT_SGST', 'CURRENT_LIABILITIES', TRUE, TRUE, TRUE),
('2230', 'GST Output IGST', 'LIABILITY', 'GST_OUTPUT_IGST', 'CURRENT_LIABILITIES', TRUE, TRUE, TRUE),
('2300', 'Bank & Commercial Loans', 'LIABILITY', NULL, 'LIABILITIES', TRUE, FALSE, TRUE),

-- Equity
('3100', 'Owner Capital', 'EQUITY', NULL, 'EQUITY', TRUE, FALSE, TRUE),
('3200', 'Owner Drawings', 'EQUITY', NULL, 'EQUITY', TRUE, FALSE, TRUE),
('3900', 'Opening Balance Equity', 'EQUITY', 'OPENING_EQUITY', 'EQUITY', TRUE, TRUE, TRUE),

-- Income
('4100', 'Sales - Fabric', 'INCOME', 'SALES', 'REVENUE', TRUE, TRUE, TRUE),

-- Expenses - Materials
('5100', 'Purchases - Yarn', 'EXPENSE', 'PURCHASES_YARN', 'MATERIALS', TRUE, TRUE, TRUE),
('5110', 'Purchases - Other Materials', 'EXPENSE', 'PURCHASES_OTHER', 'MATERIALS', TRUE, TRUE, TRUE),

-- Expenses - Direct Costs
('5300', 'Wages - Weaving & Production', 'EXPENSE', NULL, 'DIRECT', TRUE, FALSE, TRUE),

-- Expenses - Operating
('5200', 'Electricity Charges', 'EXPENSE', NULL, 'OPERATING', TRUE, FALSE, TRUE),
('5210', 'Transport & Freight', 'EXPENSE', NULL, 'OPERATING', TRUE, FALSE, TRUE),
('5220', 'Machine Maintenance & Repairs', 'EXPENSE', NULL, 'OPERATING', TRUE, FALSE, TRUE),
('5230', 'Factory Rent', 'EXPENSE', NULL, 'OPERATING', TRUE, FALSE, TRUE),
('5240', 'Administrative & Office Expenses', 'EXPENSE', NULL, 'OPERATING', TRUE, FALSE, TRUE),
('5900', 'Round Off', 'EXPENSE', 'ROUND_OFF', 'OPERATING', TRUE, TRUE, TRUE);

-- 3. Initial Payment Accounts
INSERT INTO payment_account (name, kind, account_id, bank_name, account_last4, active) VALUES
('Cash Counter', 'CASH', (SELECT id FROM account WHERE code = '1010'), NULL, NULL, TRUE),
('SBI Current Account', 'BANK', (SELECT id FROM account WHERE code = '1020'), 'State Bank of India', '4321', TRUE);

-- 4. Initial Expense Categories
INSERT INTO expense_category (name, account_id, active) VALUES
('Electricity & Power', (SELECT id FROM account WHERE code = '5200'), TRUE),
('Transport & Delivery', (SELECT id FROM account WHERE code = '5210'), TRUE),
('Machine Maintenance', (SELECT id FROM account WHERE code = '5220'), TRUE),
('Factory Rent', (SELECT id FROM account WHERE code = '5230'), TRUE),
('Administrative Expenses', (SELECT id FROM account WHERE code = '5240'), TRUE),
('Weaving Wages', (SELECT id FROM account WHERE code = '5300'), TRUE);

-- 5. Business Profile (Single row id = 1, Tamil Nadu state_code = 33)
INSERT INTO business_profile (
    id, legal_name, trade_name, address, state_code, gstin, pan, phone, email,
    invoice_footer, bank_details_text, gst_registered, round_invoice_total, lock_date
) OVERRIDING SYSTEM VALUE VALUES (
    1,
    'TEX ARUN TEXTILES',
    'Tex Arun',
    '108, Weavers Colony, Kumaran Road, Tiruppur, Tamil Nadu - 641601',
    '33',
    '33ABCDE1234F1Z5',
    'ABCDE1234F',
    '9876543210',
    'owner@texarun.com',
    'Thank you for your business. All disputes subject to Tiruppur jurisdiction.',
    'Bank: State Bank of India | A/c No: XXXXXXXX4321 | IFSC: SBIN0001234 | Branch: Tiruppur Main',
    TRUE,
    TRUE,
    NULL
) ON CONFLICT (id) DO NOTHING;

-- 6. Document Sequences for all existing financial years
INSERT INTO document_sequence (doc_type, financial_year_id, prefix, next_number)
SELECT d.doc_type, fy.id, d.prefix, 1
FROM financial_year fy
CROSS JOIN (
    VALUES
        ('INV', 'INV'),
        ('PUR', 'PUR'),
        ('REC', 'REC'),
        ('PAY', 'PAY'),
        ('EXP', 'EXP'),
        ('TRF', 'TRF'),
        ('OTH', 'OTH'),
        ('JV',  'JV')
) AS d(doc_type, prefix)
ON CONFLICT (doc_type, financial_year_id) DO NOTHING;
