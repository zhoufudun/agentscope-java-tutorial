-- ============================================
-- AgentScope 技能系统数据库 Schema
-- ============================================
--
-- 兼容性说明（H2 MODE=MySQL 与 生产 MySQL 双兼容）：
-- 1. 本 Schema 保留了 MySQL 生产环境常用的 ENGINE=InnoDB / DEFAULT CHARSET=utf8mb4 /
--    COLLATE=utf8mb4_unicode_ci、行内 COMMENT、UNIQUE KEY、行内 INDEX 等写法。
--    实测这些写法在 H2(MODE=MySQL) 下均可被解析（会被忽略或等价映射），因此无需为测试删除，
--    以免削弱生产 MySQL 的字符集/注释等语义（utf8mb4 对中文/emoji 很重要）。
-- 2. 唯一需要修正的差异：索引名的作用域。
--    - MySQL：索引名仅需“表内唯一”，因此多张表可复用同名索引（如 idx_skill_id）。
--    - H2   ：索引名需“Schema 内唯一”，同名索引会报 "Index already exists"。
--    因此这里把原先在多张表中重复的 idx_skill_id 统一改为“表名前缀 + 列名”的唯一名
--    （idx_resource_skill_id / idx_version_skill_id / idx_log_skill_id）。
--    该命名在 MySQL 与 H2 下都合法，不改变任何索引语义，故不影响生产 MySQL。
-- 3. 未采用“测试专用 schema + spring.sql.init.schema-locations 覆盖”方案：
--    因为 SkillServiceTest 使用的是 dev Profile（application.yml + application-dev.yml，
--    均为 H2 MODE=MySQL），且上述改动已能让同一份 schema.sql 同时兼容 H2 与 MySQL，
--    单文件方案更简单、维护成本更低。
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
    INDEX idx_resource_skill_id (skill_id)
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
    INDEX idx_version_skill_id (skill_id)
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
    INDEX idx_log_skill_id (skill_id),
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