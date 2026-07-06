CREATE TABLE sandbox_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    script VARCHAR(4000) NOT NULL,
    output_json VARCHAR(8000),
    error_message VARCHAR(2000),
    approval_reason VARCHAR(1000),
    timeout_ms INT NOT NULL,
    execution_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_sandbox_task_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_sandbox_task_user_created ON sandbox_task (user_id, created_at);
CREATE INDEX idx_sandbox_task_status_created ON sandbox_task (status, created_at);
