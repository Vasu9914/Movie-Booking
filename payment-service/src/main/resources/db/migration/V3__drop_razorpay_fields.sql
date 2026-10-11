-- ============================================================
-- Schema v3.0 — Payment Service (payment_db)
-- Flyway migration V3: Drop Razorpay fields (no longer used)
-- ============================================================

ALTER TABLE payments DROP COLUMN IF EXISTS razorpay_order_id;
ALTER TABLE payments DROP COLUMN IF EXISTS razorpay_payment_id;
ALTER TABLE payments DROP COLUMN IF EXISTS razorpay_signature;
