-- AgentScope Tutorial 数据库初始化脚本

-- 创建通用数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS agentscope DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE agentscope;

-- 通用表结构（可被各章节示例项目引用）
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) DEFAULT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '1:active 2:archived 3:deleted',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` VARCHAR(64) NOT NULL,
    `role` VARCHAR(32) NOT NULL COMMENT 'user/assistant/system/tool',
    `content` TEXT,
    `tool_name` VARCHAR(128) DEFAULT NULL,
    `tool_args` TEXT DEFAULT NULL COMMENT 'JSON格式的工具参数',
    `metadata` TEXT DEFAULT NULL COMMENT '其他元数据JSON',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_role` (`role`),
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 技能仓库表
CREATE TABLE IF NOT EXISTS `skill_definition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `skill_name` VARCHAR(128) NOT NULL UNIQUE,
    `description` TEXT,
    `tool_definitions` TEXT NOT NULL COMMENT 'JSON数组，工具定义列表',
    `prompt_template` TEXT,
    `enabled` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 计划任务表
CREATE TABLE IF NOT EXISTS `plan_record` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(64) DEFAULT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `goal` TEXT,
    `plan_json` TEXT NOT NULL COMMENT '计划JSON',
    `status` VARCHAR(32) DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 审批记录表
CREATE TABLE IF NOT EXISTS `approval_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `request_id` VARCHAR(64) NOT NULL UNIQUE,
    `session_id` VARCHAR(64) NOT NULL,
    `tool_name` VARCHAR(128) NOT NULL,
    `tool_args` TEXT,
    `reasoning` TEXT,
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/TIMEOUT',
    `approver` VARCHAR(64) DEFAULT NULL,
    `approved_at` TIMESTAMP NULL,
    `reject_reason` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_request_id` (`request_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库文档表 (RAG)
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `kb_id` VARCHAR(64) NOT NULL,
    `doc_title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `chunk_id` VARCHAR(64) NOT NULL,
    `vector_id` VARCHAR(128) DEFAULT NULL,
    `metadata` TEXT COMMENT 'JSON格式的文档元数据',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_kb_id` (`kb_id`),
    INDEX `idx_chunk_id` (`chunk_id`),
    UNIQUE KEY `uk_chunk` (`kb_id`, `chunk_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_type` VARCHAR(64) NOT NULL,
    `agent_id` VARCHAR(64) DEFAULT NULL,
    `user_id` VARCHAR(64) DEFAULT NULL,
    `action` VARCHAR(128) DEFAULT NULL,
    `resource_type` VARCHAR(64) DEFAULT NULL,
    `resource_id` VARCHAR(128) DEFAULT NULL,
    `details` TEXT,
    `ip_address` VARCHAR(64) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_agent_id` (`agent_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;