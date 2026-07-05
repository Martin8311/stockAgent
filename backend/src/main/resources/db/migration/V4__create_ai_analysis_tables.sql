CREATE TABLE ai_analysis_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    exchange VARCHAR(40) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    model_id VARCHAR(80) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    question VARCHAR(600) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    summary VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_ai_analysis_task_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE ai_token_usage_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_task_id BIGINT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model_id VARCHAR(80) NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    total_tokens INT NOT NULL,
    usage_source VARCHAR(32) NOT NULL,
    estimated_cost DECIMAL(19, 8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    billable BOOLEAN NOT NULL,
    test_mode BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ai_token_usage_task FOREIGN KEY (analysis_task_id) REFERENCES ai_analysis_task (id),
    CONSTRAINT fk_ai_token_usage_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_ai_analysis_task_user_created ON ai_analysis_task (user_id, created_at);
CREATE INDEX idx_ai_token_usage_user_created ON ai_token_usage_record (user_id, created_at);
