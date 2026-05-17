-- Memory Records Table
CREATE TABLE IF NOT EXISTS memory_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    content TEXT NOT NULL,
    content_type VARCHAR(50) DEFAULT 'text',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_index INT NOT NULL,
    is_summarized BOOLEAN DEFAULT FALSE,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_session_index (session_id, message_index)
);

-- Session Metadata Table
CREATE TABLE IF NOT EXISTS session_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255),
    session_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_count INT DEFAULT 0,
    last_activity_at TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- Memory Compression History Table
CREATE TABLE IF NOT EXISTS memory_compression_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    original_message_count INT NOT NULL,
    compressed_message_count INT NOT NULL,
    compression_ratio DECIMAL(5,2),
    summary_content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
);