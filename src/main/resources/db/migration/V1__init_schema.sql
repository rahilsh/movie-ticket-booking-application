-- =====================================================================
-- V1: Initial schema for Movie Ticket Booking Application
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)        NOT NULL,
    email         VARCHAR(255)        NOT NULL UNIQUE,
    password_hash VARCHAR(255)        NOT NULL,
    phone         VARCHAR(20),
    gender        VARCHAR(10)         NOT NULL,
    role          VARCHAR(20)         NOT NULL DEFAULT 'ROLE_USER',
    created_at    TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- -------------------------

CREATE TABLE IF NOT EXISTS theatres (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    address TEXT         NOT NULL,
    city    VARCHAR(100) NOT NULL
);

-- -------------------------

CREATE TABLE IF NOT EXISTS screens (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(50)  NOT NULL,
    total_capacity INT          NOT NULL,
    rows           INT          NOT NULL,
    cols           INT          NOT NULL,
    theatre_id     BIGINT       NOT NULL REFERENCES theatres(id) ON DELETE CASCADE
);

CREATE INDEX idx_screens_theatre_id ON screens(theatre_id);

-- -------------------------

CREATE TABLE IF NOT EXISTS seats (
    id          BIGSERIAL PRIMARY KEY,
    label       VARCHAR(10)  NOT NULL,
    row_number  INT          NOT NULL,
    col_number  INT          NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'REGULAR',
    screen_id   BIGINT       NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    CONSTRAINT uk_seats_screen_label UNIQUE (screen_id, label)
);

CREATE INDEX idx_seats_screen_id ON seats(screen_id);

-- -------------------------

CREATE TABLE IF NOT EXISTS shows (
    id                  BIGSERIAL PRIMARY KEY,
    movie_name          VARCHAR(200) NOT NULL,
    start_time          TIMESTAMP    NOT NULL,
    end_time            TIMESTAMP    NOT NULL,
    base_price_in_paise INT          NOT NULL,
    screen_id           BIGINT       NOT NULL REFERENCES screens(id) ON DELETE CASCADE
);

CREATE INDEX idx_shows_screen_id   ON shows(screen_id);
CREATE INDEX idx_shows_start_time  ON shows(start_time);

-- -------------------------

CREATE TABLE IF NOT EXISTS show_seats (
    id      BIGSERIAL PRIMARY KEY,
    status  VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT      NOT NULL DEFAULT 0,
    show_id BIGINT      NOT NULL REFERENCES shows(id)  ON DELETE CASCADE,
    seat_id BIGINT      NOT NULL REFERENCES seats(id)  ON DELETE CASCADE,
    CONSTRAINT uk_show_seats_show_seat UNIQUE (show_id, seat_id)
);

CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_status  ON show_seats(show_id, status);

-- -------------------------

CREATE TABLE IF NOT EXISTS bookings (
    id                    BIGSERIAL PRIMARY KEY,
    total_amount_in_paise INT          NOT NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'PROCESSING',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    user_id               BIGINT       NOT NULL REFERENCES users(id),
    show_id               BIGINT       NOT NULL REFERENCES shows(id)
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_show_id ON bookings(show_id);

-- -------------------------

CREATE TABLE IF NOT EXISTS booking_show_seats (
    booking_id   BIGINT NOT NULL REFERENCES bookings(id)   ON DELETE CASCADE,
    show_seat_id BIGINT NOT NULL REFERENCES show_seats(id) ON DELETE CASCADE,
    PRIMARY KEY (booking_id, show_seat_id)
);

-- -------------------------

CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(100) NOT NULL UNIQUE,
    amount_in_paise INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'INITIATED',
    failure_reason  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    booking_id      BIGINT       NOT NULL UNIQUE REFERENCES bookings(id)
);

CREATE INDEX idx_payments_booking_id     ON payments(booking_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
