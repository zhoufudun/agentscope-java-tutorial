# Chapter12 计划与任务分解 - HTTP 接口测试用例

本文件为 `chapter12-plan` 模块对外暴露的 REST 接口提供可执行的测试用例（基于 `curl`）。

模块已迁移到 AgentScope 2.x，关键变化：

- Agent 的“先规划、再执行”能力由 1.x 的 `ReActAgent + PlanNotebook`（`io.agentscope.core.plan.*`，2.x 已整包移除）
  改为 **harness 计划模式（Plan Mode）**：`HarnessAgent.builder().enablePlanMode().enableTaskList()...build()`，
  计划以 workspace 下 `plans/PLAN.md` 文件呈现，状态保存在 `io.agentscope.core.state.PlanModeContextState`。
- 结构化计划 CRUD（`/api/plan/*`）由**教程自有领域模型**（`model/Plan`、`model/SubTask`、
  `plan/PlanNotebook`、`plan/PlanStorage`）支撑，配合 H2 持久化，用于可视化“任务分解 / 进度追踪”。
- 流式对话由 `agent.streamEvents(msg, ctx)`（`Flux<AgentEvent>`）驱动；`plan_exit` 触发 HITL 确认时
  以 SSE 事件 `{"t":"paused"}` 通知前端，`/api/chat/resume` 批准后回到 BUILD 模式继续执行。

覆盖两类接口：

- `ChatController`（前缀 `/api`）
- `PlanController`（前缀 `/api/plan`）

> 运行前提：`mvn -pl chapter12-plan spring-boot:run` 启动服务（默认端口 8080）。
> 涉及模型推理的用例（对话/恢复）需要配置有效的 `DASHSCOPE_API_KEY`；纯 CRUD/查询用例无需模型。
> H2 控制台：`http://localhost:8080/h2-console`。

---

## 一、ChatController（`/api`）

### 用例 1.1 发起对话（SSE 流式，触发计划模式）
- 方法：`POST /api/chat`
- 目的：验证 Agent 处理复杂任务时进入计划模式（`plan_enter` → `plan_write` → `plan_exit`），
  流式返回思维/文本/工具事件。

```bash
curl -N -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: demo-1" \
  -d '{"message": "帮我设计一个博客系统的数据库表结构，先规划再执行"}'
```

预期：HTTP 200，`Content-Type: text/event-stream`；持续收到形如
`{"t":"think","d":"..."}`、`{"t":"text","d":"..."}`、`{"t":"tool_start","n":"plan_write"}`、
`{"t":"tool","n":"plan_write","d":"..."}` 的事件；当 Agent 调用 `plan_exit` 请求批准时，收到 `{"t":"paused"}`。

### 用例 1.2 恢复暂停中的 Agent（批准 plan_exit）
- 方法：`POST /api/chat/resume`
- 目的：验证收到 `{"t":"paused"}` 后，恢复接口批准 HITL 确认并让 Agent 回到 BUILD 模式继续执行。

```bash
curl -N -X POST "http://localhost:8080/api/chat/resume" \
  -H "X-Session-Id: demo-1"
```

预期：HTTP 200，`text/event-stream`；Agent 继续输出执行阶段的文本/工具事件。
若当前会话没有待确认项，返回单条 `{"t":"error","d":"Agent is not paused or has no pending confirmation."}`。

### 用例 1.3 请求停止 Agent
- 方法：`POST /api/chat/stop`
- 目的：验证可中断当前运行（2.x 使用 `HarnessAgent.interrupt()`）。

```bash
curl -X POST "http://localhost:8080/api/chat/stop"
```

预期：HTTP 200，返回体 `{"status":"stop_requested","message":"Agent will pause after next plan tool"}`。

### 用例 1.4 查询 Agent 状态
- 方法：`GET /api/chat/status`

```bash
curl "http://localhost:8080/api/chat/status"
```

预期：HTTP 200，返回 `paused`（布尔）、`hasPlan`、`planName`、`subtaskCount`。
其中 `hasPlan/planName/subtaskCount` 来自教程自有的结构化 `PlanNotebook`（若尚未通过 `/api/plan` 创建计划则 `hasPlan=false`）。

### 用例 1.5 重置 Agent 与会话
- 方法：`POST /api/reset`

```bash
curl -X POST "http://localhost:8080/api/reset"
```

预期：HTTP 200，返回体 `{"status":"reset","message":"Agent and plan have been reset"}`；
`paused` 复位为 `false`，各会话上下文与待确认项被清空。

### 用例 1.6 健康检查
- 方法：`GET /api/health`

```bash
curl "http://localhost:8080/api/health"
```

预期：HTTP 200，返回 `{"status":"UP","service":"chapter12-plan-tutorial","version":"1.0.0"}`。

---

## 二、PlanController（`/api/plan`，结构化计划 CRUD）

> 说明：这一组接口操作的是教程自有的结构化计划模型（H2 持久化），与 Agent 的 harness Plan Mode 独立。

### 用例 2.1 创建计划
- 方法：`POST /api/plan`

```bash
curl -X POST "http://localhost:8080/api/plan" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "博客系统开发",
        "description": "从零搭建一个博客系统",
        "expectedOutcome": "可运行的博客后端",
        "subtasks": [
          {"name": "设计数据库", "description": "设计表结构", "expectedOutcome": "schema.sql"},
          {"name": "实现接口", "description": "文章 CRUD", "expectedOutcome": "REST API"}
        ]
      }'
```

预期：HTTP 200，`success=true`，`message` 含 `created successfully`，`planName="博客系统开发"`。
名称为空时返回 400，`error="Plan name is required"`。

### 用例 2.2 获取当前计划
- 方法：`GET /api/plan`

```bash
curl "http://localhost:8080/api/plan"
```

预期：HTTP 200。存在计划时返回 `hasPlan=true` 及 `id/name/state/subtasks/progress` 等；
无计划时返回 `{"hasPlan":false,"message":"No active plan"}`。

### 用例 2.3 更新计划基本信息
- 方法：`PUT /api/plan`

```bash
curl -X PUT "http://localhost:8080/api/plan" \
  -H "Content-Type: application/json" \
  -d '{"name": "博客系统开发（v2）"}'
```

预期：HTTP 200，`success=true`，`message` 含更新后的名称。

### 用例 2.4 添加子任务
- 方法：`POST /api/plan/subtasks`

```bash
curl -X POST "http://localhost:8080/api/plan/subtasks" \
  -H "Content-Type: application/json" \
  -d '{"name": "编写测试", "description": "单元测试", "expectedOutcome": "覆盖率>80%"}'
```

预期：HTTP 200，`success=true`，`message` 含 `added successfully`。
超过 `maxSubtasks`（默认 15）时 `message` 含 `maximum`。

### 用例 2.5 更新子任务状态
- 方法：`PATCH /api/plan/subtasks/{index}/state`

```bash
curl -X PATCH "http://localhost:8080/api/plan/subtasks/0/state" \
  -H "Content-Type: application/json" \
  -d '{"state": "in_progress"}'
```

预期：HTTP 200，`success=true`。缺少 `state` 字段时返回 400，`error="State is required"`。

### 用例 2.6 完成子任务
- 方法：`POST /api/plan/subtasks/{index}/finish`
- 说明：完成后会自动把下一个待办子任务推进为 `in_progress`。

```bash
curl -X POST "http://localhost:8080/api/plan/subtasks/0/finish" \
  -H "Content-Type: application/json" \
  -d '{"outcome": "已产出 schema.sql，含 5 张表"}'
```

预期：HTTP 200，`success=true`，`message` 含 `marked as done successfully`。
`outcome` 为空时返回 400，`error="Outcome is required"`。

### 用例 2.7 删除子任务
- 方法：`DELETE /api/plan/subtasks/{index}`

```bash
curl -X DELETE "http://localhost:8080/api/plan/subtasks/1"
```

预期：HTTP 200，`success=true`，`message` 含 `deleted successfully`；索引非法时返回 400。

### 用例 2.8 完成计划
- 方法：`POST /api/plan/finish`

```bash
# 正常完成
curl -X POST "http://localhost:8080/api/plan/finish" \
  -H "Content-Type: application/json" \
  -d '{"state": "done", "outcome": "博客系统开发完成"}'

# 放弃计划
curl -X POST "http://localhost:8080/api/plan/finish" \
  -H "Content-Type: application/json" \
  -d '{"state": "abandoned", "outcome": "需求取消"}'
```

预期：HTTP 200，`success=true`；`done` 时 `message` 含 `finished successfully`，
`abandoned` 时 `message` 含 `abandoned`。完成后当前计划被清空（用例 2.2 返回 `hasPlan=false`）。

### 用例 2.9 获取历史计划列表
- 方法：`GET /api/plan/history`

```bash
curl "http://localhost:8080/api/plan/history"
```

预期：HTTP 200，返回计划数组（按创建时间倒序），每项含 `id/name/state/subtasks/progress`。

### 用例 2.10 获取计划统计
- 方法：`GET /api/plan/stats`

```bash
curl "http://localhost:8080/api/plan/stats"
```

预期：HTTP 200，返回 `total/done/abandoned/inProgress/todo` 各状态计数。

### 用例 2.11 获取计划 Markdown 与进度
- 方法：`GET /api/plan/markdown`

```bash
curl "http://localhost:8080/api/plan/markdown"
```

预期：HTTP 200，返回 `markdown`（计划的 Markdown 渲染）与 `progress`（`total/done/percentDone`）。

### 用例 2.12 SSE 订阅计划状态变化
- 方法：`GET /api/plan/stream`

```bash
curl -N "http://localhost:8080/api/plan/stream"
```

预期：HTTP 200，`text/event-stream`；每当计划变更（创建/修订/完成）时推送一条 JSON 快照。

---

## 三、端到端建议流程

1. 用例 1.6 健康检查确认服务已启动。
2. 用例 2.1 创建计划 → 2.2 查看 → 2.4 加子任务 → 2.5/2.6 推进状态 → 2.8 完成计划。
3. 用例 2.9/2.10/2.11 校验历史、统计与 Markdown 渲染。
4. 需要体验 Agent 自动“先规划再执行”时，执行用例 1.1 发起对话，观察计划模式事件流，
   收到 `{"t":"paused"}` 后用用例 1.2 恢复继续。

> 说明：涉及模型推理的用例（1.1 / 1.2）依赖真实 `DASHSCOPE_API_KEY`；
> 仅验证结构化计划 CRUD 与统计时，可只调用 `/api/plan/*` 系列接口，无需模型。
