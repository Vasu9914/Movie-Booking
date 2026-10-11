-- ============================================================
-- Schema v1.0 — Payment Service (payment_db)
-- Flyway migration V1: Initial schema
-- ============================================================

CREATE TABLE IF NOT EXISTS payments (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference       VARCHAR(36)   NOT NULL,
    booking_reference       VARCHAR(36)   NOT NULL,
    idempotency_key         VARCHAR(100)  NOT NULL,
    amount                  DECIMAL(10,2) NOT NULL,
    currency                CHAR(3)       NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    provider_transaction_id VARCHAR(150),
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,

    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_booking_reference ON payments(booking_reference);
CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE IF NOT EXISTS refunds (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id              BIGINT        NOT NULL,
    refund_reference        VARCHAR(36)   NOT NULL,
    idempotency_key         VARCHAR(100)  NOT NULL,
    amount                  DECIMAL(10,2) NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    provider_refund_id      VARCHAR(150),
    created_at              DATETIME(6)   NOT NULL,

    CONSTRAINT uk_refunds_reference UNIQUE (refund_reference),
    CONSTRAINT uk_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_refunds_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
