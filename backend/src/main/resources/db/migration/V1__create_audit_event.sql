CREATE TABLE audit_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    description VARCHAR(1024) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

