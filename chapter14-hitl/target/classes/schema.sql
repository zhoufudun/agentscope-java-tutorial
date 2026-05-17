-- AgentScope Java - Chapter 14: HITL Database Schema
-- 人类在环审批工作流数据库结构

-- 审批请求表
CREATE TABLE IF NOT EXISTS approval_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    tool_call_id VARCHAR(255) NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_input TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver VARCHAR(100),
    comment TEXT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    timeout_seconds INT DEFAULT 3600,
    INDEX idx_session_id (session_id),
    INDEX idx_tool_call_id (tool_call_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 审批历史记录表
CREATE TABLE IF NOT EXISTS approval_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    approval_request_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    actor VARCHAR(100),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id)
);

-- 审批配置表
CREATE TABLE IF NOT EXISTS approval_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_name VARCHAR(100) NOT NULL UNIQUE,
    require_approval BOOLEAN DEFAULT TRUE,
    approval_level INT DEFAULT 1,
    timeout_seconds INT DEFAULT 3600,
    auto_approve_hours INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);