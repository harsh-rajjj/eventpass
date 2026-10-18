-- V4__create_users.sql
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    organizer_id  BIGINT,
    created_at    DATETIME(6)  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email     UNIQUE (email),
    CONSTRAINT fk_users_organizer FOREIGN KEY (organizer_id) REFERENCES organizers (id),
    CONSTRAINT ck_users_role      CHECK (role IN ('ATTENDEE', 'ORGANIZER', 'ADMIN'))
);