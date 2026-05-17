-- =====================================================
-- AgentScope Java - 计划与任务分解表结构
-- H2 数据库表结构（兼容 MySQL）
-- =====================================================

-- 计划主表
CREATE TABLE IF NOT EXISTS plan_records (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    expected_outcome VARCHAR(1000),
    actual_outcome VARCHAR(2000),
    state VARCHAR(20) NOT NULL DEFAULT 'TODO',
    subtasks_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 子任务表
CREATE TABLE IF NOT EXISTS subtask_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL,
    task_index INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    expected_outcome VARCHAR(1000),
    actual_outcome VARCHAR(2000),
    state VARCHAR(20) NOT NULL DEFAULT 'TODO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES plan_records(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_plan_state ON plan_records(state);
CREATE INDEX IF NOT EXISTS idx_plan_name ON plan_records(name);
CREATE INDEX IF NOT EXISTS idx_plan_created ON plan_records(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subtask_plan ON subtask_records(plan_id);
CREATE INDEX IF NOT EXISTS idx_subtask_index ON subtask_records(plan_id, task_index);