CREATE TABLE investment_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    exchange VARCHAR(40) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    latest_price DECIMAL(19, 6),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_investment_asset_symbol_exchange_currency UNIQUE (symbol, exchange, currency)
);

CREATE TABLE portfolio_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    quantity DECIMAL(19, 6) NOT NULL,
    price DECIMAL(19, 6) NOT NULL,
    fee DECIMAL(19, 6) NOT NULL,
    traded_at TIMESTAMP NOT NULL,
    note VARCHAR(240),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_portfolio_transaction_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_portfolio_transaction_asset FOREIGN KEY (asset_id) REFERENCES investment_asset (id)
);

CREATE INDEX idx_portfolio_transaction_user_time ON portfolio_transaction (user_id, traded_at);
CREATE INDEX idx_portfolio_transaction_asset ON portfolio_transaction (asset_id);
