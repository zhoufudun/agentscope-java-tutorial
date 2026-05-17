-- =============================================================================
-- AgentScope Session Schema for H2 Database
-- =============================================================================

-- 会话表
CREATE TABLE IF NOT EXISTS agent_sessions (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata    TEXT
);

-- 消息历史表
CREATE TABLE IF NOT EXISTS session_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(36) NOT NULL,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    tool_call_id VARCHAR(255),
    tool_name   VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES agent_sessions(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_messages_session ON session_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON session_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_sessions_updated ON agent_sessions(updated_at);