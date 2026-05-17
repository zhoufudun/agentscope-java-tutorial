-- Model Provider Table
CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(500),
    base_url VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 100,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chat Config Table
CREATE TABLE IF NOT EXISTS chat_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    temperature DOUBLE DEFAULT 0.7,
    max_tokens INT DEFAULT 2000,
    system_prompt VARCHAR(4000),
    stream_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_provider_name ON model_provider(name);
CREATE INDEX IF NOT EXISTS idx_config_provider ON chat_config(provider_type);
CREATE INDEX IF NOT EXISTS idx_config_default ON chat_config(is_default);