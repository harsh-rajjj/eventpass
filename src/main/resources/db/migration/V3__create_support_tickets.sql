-- V3__create_support_tickets.sql
CREATE TABLE support_tickets (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    message    TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    closed_at  DATETIME(6),
    created_at DATETIME(6)  NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_support_tickets_status CHECK (status IN ('NEW', 'CLOSED'))
);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);
