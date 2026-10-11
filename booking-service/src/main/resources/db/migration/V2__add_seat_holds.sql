-- ============================================================
-- Schema v2.0 — Booking Service (booking_db)
-- Flyway migration V2: Add seat_holds table for hold tracking
-- ============================================================

-- -----------------------------------------------------------
-- Table: seat_holds
-- Tracks seat holds with a unique hold_reference
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS seat_holds (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    hold_reference      VARCHAR(36)   NOT NULL,
    user_id             BIGINT,
    show_id             BIGINT        NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    expires_at          DATETIME(6)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)   NOT NULL,

    CONSTRAINT uk_seat_holds_reference UNIQUE (hold_reference),
    CONSTRAINT chk_seat_holds_total_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for seat_holds
CREATE INDEX idx_seat_holds_reference ON seat_holds(hold_reference);
CREATE INDEX idx_seat_holds_status ON seat_holds(status);
CREATE INDEX idx_seat_holds_expires ON seat_holds(expires_at);

-- -----------------------------------------------------------
-- Table: seat_hold_seats
-- Links seat_holds to show_seats
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS seat_hold_seats (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    hold_id         BIGINT        NOT NULL,
    show_seat_id    BIGINT        NOT NULL,

    CONSTRAINT fk_seat_hold_seats_hold FOREIGN KEY (hold_id) REFERENCES seat_holds(id),
    CONSTRAINT fk_seat_hold_seats_show_seat FOREIGN KEY (show_seat_id) REFERENCES show_seats(id),
    CONSTRAINT uk_seat_hold_seats_unique UNIQUE (hold_id, show_seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for seat_hold_seats
CREATE INDEX idx_seat_hold_seats_show_seat_id ON seat_hold_seats(show_seat_id);
