-- ============================================================
-- Schema v1.0 — Notification Service (notification_db)
-- Flyway migration V1: Initial schema
-- ============================================================

-- -----------------------------------------------------------
-- Table: notifications
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id            VARCHAR(100)  NOT NULL,
    user_id             BIGINT        NOT NULL,
    booking_reference   VARCHAR(36),
    channel             VARCHAR(20)   NOT NULL,
    recipient           VARCHAR(254)  NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    attempt_count       INT           NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    sent_at             DATETIME(6),

    CONSTRAINT uk_notifications_event_channel UNIQUE (event_id, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for notifications
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_booking_reference ON notifications(booking_reference);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_status_attempt ON notifications(status, attempt_count);

-- -----------------------------------------------------------
-- Table: processed_events
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS processed_events (
    event_id      VARCHAR(100)  PRIMARY KEY,
    event_type    VARCHAR(100)  NOT NULL,
    service_name  VARCHAR(50)   NOT NULL,
    processed_at  DATETIME(6)   NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for processed_events
CREATE INDEX idx_processed_events_event_type ON processed_events(event_type);
