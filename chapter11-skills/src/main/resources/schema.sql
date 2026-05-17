-- ============================================
-- AgentScope 技能系统数据库 Schema
-- ============================================

-- 技能定义表
CREATE TABLE IF NOT EXISTS skill_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id VARCHAR(255) NOT NULL UNIQUE COMMENT '技能唯一标识 (name_source)',
    name VARCHAR(255) NOT NULL COMMENT '技能名称',
    description TEXT COMMENT '技能描述',
    version VARCHAR(50) DEFAULT '1.0.0' COMMENT '技能版本',
    author VARCHAR(255) COMMENT '技能作者',
    tags VARCHAR(500) COMMENT '技能标签，逗号分隔',
    skill_content TEXT COMMENT '技能内容 (SKILL.md)',
    source VARCHAR(255) DEFAULT 'custom' COMMENT '技能来源',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_skill_name (name),
    INDEX idx_skill_source (source),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 技能资源表
CREATE TABLE IF NOT EXISTS skill_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id VARCHAR(255) NOT NULL COMMENT '所属技能ID',
    resource_path VARCHAR(500) NOT NULL COMMENT '资源路径',
    resource_content TEXT COMMENT '资源内容',
    resource_type VARCHAR(50) COMMENT '资源类型 (script/config/data)',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小',
    checksum VARCHAR(64) COMMENT '文件校验和',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skill_definition(skill_id) ON DELETE CASCADE,
    UNIQUE KEY uk_skill_resource (skill_id, resource_path),
    INDEX idx_skill_id (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 技能版本历史表
CREATE TABLE IF NOT EXISTS skill_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id VARCHAR(255) NOT NULL COMMENT '技能ID',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    skill_content TEXT COMMENT '技能内容快照',
    change_log TEXT COMMENT '版本变更日志',
    is_current BOOLEAN DEFAULT FALSE COMMENT '是否为当前版本',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skill_definition(skill_id) ON DELETE CASCADE,
    UNIQUE KEY uk_skill_version (skill_id, version),
    INDEX idx_skill_id (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 技能执行记录表
CREATE TABLE IF NOT EXISTS skill_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id VARCHAR(255) NOT NULL COMMENT '执行的技能ID',
    execution_id VARCHAR(100) NOT NULL COMMENT '执行批次ID',
    input_params TEXT COMMENT '输入参数 (JSON)',
    output_result TEXT COMMENT '输出结果 (JSON)',
    execution_status VARCHAR(20) COMMENT '执行状态 (SUCCESS/FAILED/PENDING)',
    error_message TEXT COMMENT '错误信息',
    execution_time_ms BIGINT COMMENT '执行耗时 (毫秒)',
    agent_id VARCHAR(255) COMMENT '执行者 Agent ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_skill_id (skill_id),
    INDEX idx_execution_id (execution_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 技能使用统计表
CREATE TABLE IF NOT EXISTS skill_usage_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id VARCHAR(255) NOT NULL COMMENT '技能ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_executions INT DEFAULT 0 COMMENT '总执行次数',
    success_count INT DEFAULT 0 COMMENT '成功次数',
    failure_count INT DEFAULT 0 COMMENT '失败次数',
    avg_execution_time_ms BIGINT DEFAULT 0 COMMENT '平均执行时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skill_definition(skill_id) ON DELETE CASCADE,
    UNIQUE KEY uk_skill_date (skill_id, stat_date),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 初始化示例数据
-- ============================================

-- 插入示例技能
INSERT INTO skill_definition (skill_id, name, description, version, author, tags, skill_content, source, is_active)
VALUES
('data-analysis_custom',
 'data-analysis',
 '数据分析技能 - 执行数据统计、可视化分析和报告生成',
 '1.0.0',
 'AgentScope Tutorial',
 'data-analysis,statistics,python',
 '---
name: data-analysis
description: 数据分析技能 - 执行数据统计、可视化分析和报告生成
version: 1.0.0
---
# 数据分析技能

本技能用于执行数据分析任务，支持：
- 数据加载和清洗
- 统计分析和计算
- 数据可视化
- 分析报告生成',
 'custom',
 TRUE),
('code-review_custom',
 'code-review',
 '代码审查技能 - 自动审查代码质量、发现潜在问题和提出改进建议',
 '1.0.0',
 'AgentScope Tutorial',
 'code-review,quality,lint',
 '---
name: code-review
description: 代码审查技能 - 自动审查代码质量、发现潜在问题和提出改进建议
version: 1.0.0
---
# 代码审查技能

本技能用于执行代码审查任务，支持：
- 代码风格检查
- 潜在 Bug 检测
- 性能问题识别
- 安全漏洞扫描',
 'custom',
 TRUE);