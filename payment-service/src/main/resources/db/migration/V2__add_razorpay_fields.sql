-- ============================================================
-- Schema v2.0 — Payment Service (payment_db)
-- Flyway migration V2: Add Razorpay fields
-- ============================================================

ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(150);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(150);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_signature VARCHAR(255);
