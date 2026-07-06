CREATE TABLE skill_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    skill_key VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    active_version_id BIGINT,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_skill_definition_key UNIQUE (skill_key)
);

CREATE TABLE skill_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    skill_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    content VARCHAR(8000) NOT NULL,
    test_script VARCHAR(4000) NOT NULL,
    test_result_json VARCHAR(8000),
    approval_reason VARCHAR(1000),
    created_by VARCHAR(160) NOT NULL,
    reviewed_by VARCHAR(160),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_skill_version_definition FOREIGN KEY (skill_id) REFERENCES skill_definition (id),
    CONSTRAINT uk_skill_version_number UNIQUE (skill_id, version_number)
);

CREATE TABLE approval_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_type VARCHAR(60) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    decision_comment VARCHAR(1000),
    requested_by VARCHAR(160) NOT NULL,
    reviewed_by VARCHAR(160),
    created_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_skill_definition_updated ON skill_definition (updated_at);
CREATE INDEX idx_skill_version_skill_status ON skill_version (skill_id, status);
CREATE INDEX idx_approval_status_created ON approval_request (status, created_at);
CREATE INDEX idx_approval_target_status ON approval_request (target_type, target_id, status);
