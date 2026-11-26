CREATE TABLE bookings (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    booking_ref     VARCHAR(20)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    ticket_tier_id  BIGINT        NOT NULL,
    event_id        BIGINT        NOT NULL,
    quantity        INT           NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED',
    cancelled_at    DATETIME(6),
    checked_in_at   DATETIME(6),
    created_at      DATETIME(6)   NOT NULL,
    created_by      VARCHAR(100)  NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    updated_by      VARCHAR(100)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookings_ref         UNIQUE (booking_ref),
    CONSTRAINT fk_bookings_user        FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_bookings_ticket_tier FOREIGN KEY (ticket_tier_id) REFERENCES ticket_tiers (id),
    CONSTRAINT fk_bookings_event       FOREIGN KEY (event_id)       REFERENCES events (id),
    CONSTRAINT ck_bookings_quantity    CHECK (quantity > 0),
    CONSTRAINT ck_bookings_unit_price  CHECK (unit_price >= 0),
    CONSTRAINT ck_bookings_total       CHECK (total_amount >= 0),
    CONSTRAINT ck_bookings_status      CHECK (status IN ('CONFIRMED', 'CANCELLED', 'CHECKED_IN'))
);

CREATE INDEX idx_bookings_user_created      ON bookings (user_id, created_at);
CREATE INDEX idx_bookings_event_status      ON bookings (event_id, status);
CREATE INDEX idx_bookings_user_event_status ON bookings (user_id, event_id, status);