-- =============================================================================
-- V1: Initial Foundation Schema
-- Tables: app_user, business_profile, gst_state, financial_year, audit_event,
--         SPRING_SESSION, SPRING_SESSION_ATTRIBUTES
-- =============================================================================

-- 1. App Users (Owner account)
CREATE TABLE app_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CONSTRAINT chk_app_user_role CHECK (role IN ('OWNER')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ NULL,
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_app_user_username_lower ON app_user (LOWER(username));

-- 2. Business Profile (Single row configuration)
CREATE TABLE business_profile (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    legal_name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    state_code VARCHAR(2) NOT NULL CONSTRAINT chk_business_profile_state_len CHECK (length(state_code) = 2),
    gstin VARCHAR(15) NULL,
    pan VARCHAR(10) NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    invoice_footer TEXT NULL,
    bank_details_text TEXT NULL,
    gst_registered BOOLEAN NOT NULL DEFAULT TRUE,
    round_invoice_total BOOLEAN NOT NULL DEFAULT TRUE,
    lock_date DATE NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_business_profile_single_row CHECK (id = 1)
);

-- 3. GST States (Standard 2-digit Indian State / UT codes)
CREATE TABLE gst_state (
    code VARCHAR(2) NOT NULL PRIMARY KEY CONSTRAINT chk_gst_state_code_len CHECK (length(code) = 2),
    name VARCHAR(100) NOT NULL
);

INSERT INTO gst_state (code, name) VALUES
('01', 'Jammu and Kashmir'),
('02', 'Himachal Pradesh'),
('03', 'Punjab'),
('04', 'Chandigarh'),
('05', 'Uttarakhand'),
('06', 'Haryana'),
('07', 'Delhi'),
('08', 'Rajasthan'),
('09', 'Uttar Pradesh'),
('10', 'Bihar'),
('11', 'Sikkim'),
('12', 'Arunachal Pradesh'),
('13', 'Nagaland'),
('14', 'Manipur'),
('15', 'Mizoram'),
('16', 'Tripura'),
('17', 'Meghalaya'),
('18', 'Assam'),
('19', 'West Bengal'),
('20', 'Jharkhand'),
('21', 'Odisha'),
('22', 'Chhattisgarh'),
('23', 'Madhya Pradesh'),
('24', 'Gujarat'),
('25', 'Daman and Diu'),
('26', 'Dadra and Nagar Haveli'),
('27', 'Maharashtra'),
('28', 'Andhra Pradesh (Old)'),
('29', 'Karnataka'),
('30', 'Goa'),
('31', 'Lakshadweep'),
('32', 'Kerala'),
('33', 'Tamil Nadu'),
('34', 'Puducherry'),
('35', 'Andaman and Nicobar Islands'),
('36', 'Telangana'),
('37', 'Andhra Pradesh (New)'),
('38', 'Ladakh');

-- 4. Financial Year (April - March)
CREATE TABLE financial_year (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label VARCHAR(10) NOT NULL UNIQUE,
    start_date DATE NOT NULL UNIQUE,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_financial_year_dates CHECK (end_date > start_date)
);

INSERT INTO financial_year (label, start_date, end_date) VALUES
('2025-26', '2025-04-01', '2026-03-31'),
('2026-27', '2026-04-01', '2027-03-31');

-- 5. Audit Event (Append-only security and activity trail)
CREATE TABLE audit_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_user_id BIGINT NULL,
    actor_username VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NULL,
    summary VARCHAR(255) NOT NULL,
    details JSONB NULL,
    ip_address VARCHAR(45) NULL
);

CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at);
CREATE INDEX idx_audit_event_entity ON audit_event (entity_type, entity_id);

-- 6. Spring Session Tables (PostgreSQL schema)
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
