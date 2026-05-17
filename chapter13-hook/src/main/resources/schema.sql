-- AgentScope Hook Tutorial Schema
-- 审计日志表

-- 工具调用审计记录表
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL,
    tool_name VARCHAR(255) NOT NULL,
    input_content TEXT,
    output_content TEXT,
    start_time TIMESTAMP NOT NULL,
    duration_ms BIGINT NOT NULL,
    sensitive BOOLEAN DEFAULT FALSE,
    blocked BOOLEAN DEFAULT FALSE,
    trace_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_audit_log_agent ON audit_log(agent_name);
CREATE INDEX idx_audit_log_tool ON audit_log(tool_name);
CREATE INDEX idx_audit_log_time ON audit_log(start_time);
CREATE INDEX idx_audit_log_sensitive ON audit_log(sensitive);