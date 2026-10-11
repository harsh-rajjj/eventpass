CREATE TABLE organizers (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    name          VARCHAR(150)  NOT NULL,
    description   VARCHAR(2000),
    website       VARCHAR(255),
    contact_email VARCHAR(255)  NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    CONSTRAINT uk_organizers_name UNIQUE (name),
    CONSTRAINT ck_organizers_status CHECK (status IN ('PENDING', 'APPROVED', 'SUSPENDED'))
);

CREATE TABLE events (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    organizer_id      BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    category          VARCHAR(30)  NOT NULL,
    venue_name        VARCHAR(200) NOT NULL,
    city              VARCHAR(100) NOT NULL,
    address           VARCHAR(255),
    starts_at         DATETIME(6)  NOT NULL,   -- UTC, microsecond precision
    ends_at           DATETIME(6)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    banner_image_path VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES organizers (id),
    CONSTRAINT ck_events_status   CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CONSTRAINT ck_events_category CHECK (category IN ('MUSIC', 'TECH', 'SPORTS', 'COMEDY', 'WORKSHOP', 'OTHER')),
    CONSTRAINT ck_events_dates    CHECK (ends_at > starts_at)
);
CREATE INDEX idx_events_status_starts_at ON events (status, starts_at);   -- public listing
CREATE INDEX idx_events_city_starts_at   ON events (city, starts_at);     -- search by city

CREATE TABLE ticket_tiers (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    event_id   BIGINT        NOT NULL,
    name       VARCHAR(100)  NOT NULL,
    price      DECIMAL(10,2) NOT NULL,         -- money is never FLOAT/DOUBLE
    capacity   INT           NOT NULL,
    sold_count INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_tiers_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_tiers_event_name UNIQUE (event_id, name),
    CONSTRAINT ck_ticket_tiers_price    CHECK (price >= 0),
    CONSTRAINT ck_ticket_tiers_capacity CHECK (capacity > 0),
    CONSTRAINT ck_ticket_tiers_sold     CHECK (sold_count >= 0 AND sold_count <= capacity)  -- DB-level anti-overselling safety net
);
