-- ============================================================
-- Schema v1.0 — Movie Service (movie_db)
-- Flyway migration V1: Initial schema
-- ============================================================

-- -----------------------------------------------------------
-- Table: movies
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS movies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)  NOT NULL,
    description     TEXT,
    language        VARCHAR(50)   NOT NULL,
    duration_minutes INT          NOT NULL,
    release_date    DATE,
    status          VARCHAR(20)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,

    CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for movies
CREATE INDEX idx_movies_status ON movies(status);
CREATE INDEX idx_movies_language ON movies(language);
CREATE INDEX idx_movies_title ON movies(title);

-- -----------------------------------------------------------
-- Table: theatres
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS theatres (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150)  NOT NULL,
    city            VARCHAR(100)  NOT NULL,
    address         VARCHAR(255)  NOT NULL,
    status          VARCHAR(20)   NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for theatres
CREATE INDEX idx_theatres_city ON theatres(city);
CREATE INDEX idx_theatres_status ON theatres(status);

-- -----------------------------------------------------------
-- Table: screens
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS screens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    theatre_id      BIGINT        NOT NULL,
    name            VARCHAR(50)   NOT NULL,
    total_seats     INT           NOT NULL,
    status          VARCHAR(20)   NOT NULL,

    CONSTRAINT fk_screens_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id),
    CONSTRAINT uk_screens_theatre_name UNIQUE (theatre_id, name),
    CONSTRAINT chk_screens_total_seats CHECK (total_seats > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for screens
CREATE INDEX idx_screens_theatre_id ON screens(theatre_id);
CREATE INDEX idx_screens_status ON screens(status);

-- -----------------------------------------------------------
-- Table: screen_seats
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS screen_seats (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id       BIGINT        NOT NULL,
    seat_number     VARCHAR(10)   NOT NULL,
    row_label       VARCHAR(5)    NOT NULL,
    column_number   INT           NOT NULL,
    seat_type       VARCHAR(20)   NOT NULL,

    CONSTRAINT fk_screen_seats_screen FOREIGN KEY (screen_id) REFERENCES screens(id),
    CONSTRAINT uk_screen_seats_seat_number UNIQUE (screen_id, seat_number),
    CONSTRAINT uk_screen_seats_position UNIQUE (screen_id, row_label, column_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for screen_seats
CREATE INDEX idx_screen_seats_screen_id ON screen_seats(screen_id);

-- -----------------------------------------------------------
-- Table: shows
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS shows (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id        BIGINT        NOT NULL,
    screen_id       BIGINT        NOT NULL,
    start_time      DATETIME(6)   NOT NULL,
    end_time        DATETIME(6)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL,

    CONSTRAINT fk_shows_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_shows_screen FOREIGN KEY (screen_id) REFERENCES screens(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for shows
CREATE INDEX idx_shows_movie_id ON shows(movie_id);
CREATE INDEX idx_shows_screen_id ON shows(screen_id);
CREATE INDEX idx_shows_status ON shows(status);
CREATE INDEX idx_shows_screen_start ON shows(screen_id, start_time);
