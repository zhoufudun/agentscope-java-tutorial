# Chapter13 Hook 系统 - HTTP 接口测试用例

本文件为 `chapter13-hook` 模块对外暴露的 REST 接口提供可执行的测试用例（基于 `curl`）。
模块已迁移到 AgentScope 2.x：`ReActAgent` 不再使用 `.memory(...)`/`InMemoryMemory`，会话上下文由
`AgentState` 管理；Hook 事件类（`PreCallEvent`/`PostActingEvent` 等）在 2.x 中仍然保留（标注
`@Deprecated`，但可继续通过 `ReActAgent.Builder.hooks(...)` 装配）。以下用例覆盖两类接口：

- `ChatController`（前缀 `/api/chat`）
- `AuditController`（前缀 `/api/audit`）

> 运行前提：`mvn -pl chapter13-hook spring-boot:run` 启动服务（默认端口 8080），
> 并配置好 `DASHSCOPE_API_KEY`（涉及模型调用的用例需要）。
> Hook 链默认按优先级执行：AuditHook(60) → TimingHook(100) → LoggingHook(900) → TraceExporter(900) → MetricsHook(950)。

---

## 一、ChatController（`/api/chat`）

### 用例 1.1 同步调用 Agent（正常）
- 方法：`POST /api/chat/call`
- 目的：验证 Agent 可处理请求并返回回复，同时触发完整 Hook 链。

```bash
curl -X POST "http://localhost:8080/api/chat/call" \
  -H "Content-Type: application/json" \
  -d '{"message": "Calculate 123 + 456"}'
```

预期：HTTP 200，返回体 `success=true`、`response` 非空、`agent` 为配置的 Agent 名称、`durationMs >= 0`。
日志中可见 `[AUDIT]`/`[TIMING]` 等 Hook 输出；若触发 `calculate` 工具，`MetricsHook` 会累加工具调用统计。

### 用例 1.2 同步调用空消息（异常校验）
- 方法：`POST /api/chat/call`
- 目的：验证空消息返回 400。

```bash
curl -i -X POST "http://localhost:8080/api/chat/call" \
  -H "Content-Type: application/json" \
  -d '{"message": ""}'
```

预期：HTTP 400，返回体 `{"error":"message is required"}`。

### 用例 1.3 异步调用 Agent
- 方法：`POST /api/chat/call-async`
- 目的：验证异步接口立即返回 202，后台异步执行。

```bash
curl -i -X POST "http://localhost:8080/api/chat/call-async" \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我查询北京的天气"}'
```

预期：HTTP 202 Accepted，返回体包含 `message="异步调用已接受"` 与截断后的 `input`；
服务端控制台随后打印异步完成日志。

### 用例 1.4 查询性能指标
- 方法：`GET /api/chat/metrics`
- 目的：验证 `MetricsHook` 收集的指标可被读取。

```bash
curl "http://localhost:8080/api/chat/metrics"
```

预期：HTTP 200，返回 `totalCalls/totalTimeMs/averageTimeMs/totalErrors/toolStats`。
在执行过 1.1 之后，`totalCalls >= 1`。

### 用例 1.5 查询 Agent 信息与 Hook 链
- 方法：`GET /api/chat/info`

```bash
curl "http://localhost:8080/api/chat/info"
```

预期：HTTP 200，返回 `name`、`healthy`、`hookInfo`（包含各 Hook 的 priority 与统计摘要）。

### 用例 1.6 重置性能指标
- 方法：`POST /api/chat/reset`

```bash
curl -X POST "http://localhost:8080/api/chat/reset"
```

预期：HTTP 200，`success=true`、`message="指标已重置"`；随后调用 1.4 应看到 `totalCalls=0`。

### 用例 1.7 健康检查
- 方法：`GET /api/chat/health`
- 说明：该接口会实际发起一次模型调用做健康探测，需要有效的 `DASHSCOPE_API_KEY`。

```bash
curl "http://localhost:8080/api/chat/health"
```

预期：HTTP 200，`status="UP"`（模型不可用时为 `"DOWN"`），并返回 `agent` 名称。

---

## 二、AuditController（`/api/audit`）

### 用例 2.1 查询内存审计记录（全部）
- 方法：`GET /api/audit/records`

```bash
curl "http://localhost:8080/api/audit/records"
```

预期：HTTP 200，`source="memory"`，`count` 与 `records` 数组长度一致；
每条记录含 `agent/tool/input/output/durationMs/sensitive/blocked/startTime`。

### 用例 2.2 按工具名过滤内存记录
- 方法：`GET /api/audit/records?tool=calculate`

```bash
curl "http://localhost:8080/api/audit/records?tool=calculate"
```

预期：HTTP 200，`records` 仅包含 `tool=calculate` 的记录。

### 用例 2.3 查询数据库审计记录
- 方法：`GET /api/audit/records/database`
- 目的：验证 `AuditHook` 通过 `AuditLogRepository` 落库的记录可分页查询。

```bash
curl "http://localhost:8080/api/audit/records/database?limit=50"
```

预期：HTTP 200，`source="database"`，`records` 每条含数据库主键 `id`；`limit` 生效。

可选按 Agent 或工具过滤：

```bash
curl "http://localhost:8080/api/audit/records/database?agent=TutorialAgent"
curl "http://localhost:8080/api/audit/records/database?tool=calculate"
```

### 用例 2.4 按工具名对比内存/数据库记录
- 方法：`GET /api/audit/records/tool?tool=calculate`

```bash
curl "http://localhost:8080/api/audit/records/tool?tool=calculate"
```

预期：HTTP 200，返回 `memoryCount`/`databaseCount` 及两侧记录数组，便于对比一致性。

### 用例 2.5 查询敏感工具记录
- 方法：`GET /api/audit/records/sensitive`
- 说明：敏感工具集合由配置 `tutorial.hook.audit.sensitive-tools` 决定。

```bash
curl "http://localhost:8080/api/audit/records/sensitive"
```

预期：HTTP 200，`records` 仅包含 `sensitive=true` 的记录；`databaseCount` 为库中敏感记录数。

### 用例 2.6 查询审计统计信息
- 方法：`GET /api/audit/statistics`

```bash
curl "http://localhost:8080/api/audit/statistics"
```

预期：HTTP 200，返回 `totalRecords/memoryRecords/averageDurationMs/sensitiveCount/blockedCount/toolStatistics`；
`toolStatistics` 按工具聚合调用次数与平均耗时。

### 用例 2.7 清空审计记录
- 方法：`DELETE /api/audit/records`
- 目的：验证同时清空内存记录与数据库记录。

```bash
curl -X DELETE "http://localhost:8080/api/audit/records"
```

预期：HTTP 200，`success=true`、`message="审计记录已清空"`；随后调用 2.1 应返回 `count=0`，
2.6 中 `totalRecords=0`。

---

## 三、端到端建议流程

1. 用例 1.6 先重置指标、用例 2.7 先清空审计记录，保证从干净状态开始。
2. 用例 1.1 触发一次含工具调用的对话（如 `Calculate 123 + 456`），驱动完整 Hook 链。
3. 用例 1.4 校验 `totalCalls`/`toolStats` 增长；用例 1.5 查看 `hookInfo`。
4. 用例 2.1 / 2.3 校验内存与数据库审计记录均已写入。
5. 用例 2.6 查看聚合统计；用例 2.7 清空后再校验计数归零。

> 说明：涉及模型推理的用例（1.1 / 1.3 / 1.7）依赖真实 `DASHSCOPE_API_KEY`；
> 若仅验证审计查询与统计逻辑，可在已有数据基础上直接调用 `/api/audit/*` 系列只读接口。
