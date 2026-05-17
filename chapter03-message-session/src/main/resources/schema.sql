-- H2 Database Schema for Chat Session Management
-- 此文件在应用启动时自动执行

-- 创建会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    metadata TEXT
);

-- 创建消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    name VARCHAR(100),
    metadata TEXT,
    created_at VARCHAR(50),
    sequence_num INTEGER NOT NULL,
    CONSTRAINT fk_session FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_updated ON chat_sessions(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_session ON chat_messages(session_id, sequence_num);

-- 插入初始测试数据（可选）
-- INSERT INTO chat_sessions (session_id, user_id, title) VALUES ('test-session-001', 'default_user', '测试会话');