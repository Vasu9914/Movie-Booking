-- ============================================================
-- Schema v1.0 — Booking Service (booking_db)
-- Flyway migration V1: Initial schema
-- ============================================================

-- -----------------------------------------------------------
-- Table: show_seats
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS show_seats (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id         BIGINT        NOT NULL,
    screen_seat_id  BIGINT        NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    reserved_until  DATETIME(6),
    booking_id      BIGINT,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_show_seats_unique UNIQUE (show_id, screen_seat_id),
    CONSTRAINT chk_show_seats_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for show_seats
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_screen_seat_id ON show_seats(screen_seat_id);
CREATE INDEX idx_show_seats_booking_id ON show_seats(booking_id);
CREATE INDEX idx_show_seats_status ON show_seats(status);
CREATE INDEX idx_show_seats_status_reserved ON show_seats(status, reserved_until);
CREATE INDEX idx_show_seats_show_status ON show_seats(show_id, status);

-- -----------------------------------------------------------
-- Table: bookings
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference   VARCHAR(36)   NOT NULL,
    user_id             BIGINT        NOT NULL,
    show_id             BIGINT        NOT NULL,
    status              VARCHAR(30)   NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    currency            CHAR(3)       NOT NULL,
    expires_at          DATETIME(6),
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT chk_bookings_total_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for bookings
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_show_id ON bookings(show_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_status_expires ON bookings(status, expires_at);

-- -----------------------------------------------------------
-- Table: booking_seats
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking_seats (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT        NOT NULL,
    show_seat_id    BIGINT        NOT NULL,
    price           DECIMAL(10,2) NOT NULL,

    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_booking_seats_show_seat FOREIGN KEY (show_seat_id) REFERENCES show_seats(id),
    CONSTRAINT uk_booking_seats_unique UNIQUE (booking_id, show_seat_id),
    CONSTRAINT chk_booking_seats_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for booking_seats
CREATE INDEX idx_booking_seats_show_seat_id ON booking_seats(show_seat_id);
