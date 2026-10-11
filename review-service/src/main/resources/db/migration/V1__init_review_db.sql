-- ============================================================
-- Schema v1.0 — Review Service (review_db)
-- Flyway migration V1: Initial schema
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    movie_id    BIGINT       NOT NULL,
    rating      INT          NOT NULL,
    comment     VARCHAR(2000),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    CONSTRAINT uk_reviews_user_movie UNIQUE (user_id, movie_id),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reviews_movie_id ON reviews(movie_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
