# Chapter14 人类在环（HITL）审批工作流 - HTTP 接口测试用例

本文件为 `chapter14-hitl` 模块对外暴露的 REST 接口提供可执行的测试用例（基于 `curl`）。
模块已迁移到 AgentScope 2.x：

- 移除了 `io.agentscope.core.session.*`（`Session`/`InMemorySession`）整包；会话改用 **String sessionId** 标识，
  由 `io.agentscope.core.state.AgentStateStore` 家族（本模块使用 `InMemoryAgentStateStore`）按
  `(userId, sessionId)` 维度持久化。
- `ReActAgent` 不再使用 `.memory(...)`/`InMemoryMemory`；改为 `.stateStore(store).defaultSessionId("default")`，
  每次调用通过携带 sessionId 的 `RuntimeContext` 自动完成会话 load/save。
- 审批仍由 `ApprovalHook`（`io.agentscope.core.hook.Hook`，2.x 中保留但标注 `@Deprecated`）在推理结束后
  拦截敏感工具调用（`send_email`、`delete_data`），调用 `stopAgent()` 暂停等待人工审批。

覆盖的接口：

- `ChatController`（前缀 `/api`）：聊天、审批回传、中断、清空会话
- `ApprovalController`（前缀 `/api/approval`）：审批查询、手动/批量审批、统计
- `WorkflowController`（前缀 `/api/workflow`）：审批恢复、中断、重置、状态查询

> 运行前提：`mvn -pl chapter14-hitl spring-boot:run` 启动服务（默认端口 8080），
> 并配置好 `DASHSCOPE_API_KEY`（涉及模型调用的用例需要）。
> `/api/chat` 与 `/api/approve` 返回 `text/event-stream`（SSE），建议 `curl -N` 观察流式事件。
> H2 控制台：`http://localhost:8080/h2-console`（JDBC URL：`jdbc:h2:mem:hitl_db`）。

---

## 一、ChatController（`/api`）

### 用例 1.1 发送聊天消息（普通问答，SSE）
- 方法：`POST /api/chat`
- 目的：验证 Agent 可处理请求并以 SSE 流式返回推理/结果事件。

```bash
curl -N -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "s-1001", "message": "帮我查询编号 A100 的数据记录"}'
```

预期：HTTP 200，`Content-Type: text/event-stream`；依次收到 `type=REASONING`（可能多次增量）、
可能的 `type=TOOL_USE`/`type=TOOL_RESULT`、`type=FINAL`，最后一条为 `type=COMPLETE`。

### 用例 1.2 触发需审批工具（暂停等待审批，SSE）
- 方法：`POST /api/chat`
- 目的：验证敏感工具调用触发 `ApprovalHook.stopAgent()`，流中出现 `PENDING_APPROVAL`。

```bash
curl -N -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "s-1002", "message": "给 alice@company.com 发一封主题为“周报”的邮件"}'
```

预期：HTTP 200；流中出现 `type=PENDING_APPROVAL`，携带 `toolCallId`、`toolName=send_email`、`toolInput`，
随后 `type=COMPLETE`。此时应有一条 `PENDING` 审批记录写入 H2（见用例 2.1）。

### 用例 1.3 消息为空（异常校验）
- 方法：`POST /api/chat`

```bash
curl -N -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "s-1003", "message": ""}'
```

预期：HTTP 200（SSE），单条事件 `{"type":"ERROR","error":"消息不能为空"}`。

### 用例 1.4 提交审批决策并恢复执行（SSE）
- 方法：`POST /api/approve`
- 目的：审批通过后向 Agent 注入工具结果并恢复执行。`toolCallId` 取自用例 1.2 的 `PENDING_APPROVAL` 事件。

```bash
curl -N -X POST "http://localhost:8080/api/approve" \
  -H "Content-Type: application/json" \
  -d '{
        "sessionId": "s-1002",
        "toolCallId": "<用例1.2返回的toolCallId>",
        "approved": true,
        "approver": "admin",
        "comment": "同意发送"
      }'
```

预期：HTTP 200（SSE）；恢复后收到后续 `REASONING`/`TOOL_RESULT`/`FINAL`，最后 `COMPLETE`。
对应审批记录状态由 `PENDING` 变为 `APPROVED`。

### 用例 1.5 拒绝审批（SSE）
- 方法：`POST /api/approve`

```bash
curl -N -X POST "http://localhost:8080/api/approve" \
  -H "Content-Type: application/json" \
  -d '{
        "sessionId": "s-1002",
        "toolCallId": "<toolCallId>",
        "approved": false,
        "approver": "admin",
        "comment": "收件人不合规"
      }'
```

预期：HTTP 200（SSE）；Agent 收到“审批被拒绝”的工具结果并据此继续，审批记录状态变为 `REJECTED`。

### 用例 1.6 缺少 toolCallId（异常校验）
- 方法：`POST /api/approve`

```bash
curl -N -X POST "http://localhost:8080/api/approve" \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "s-1002", "approved": true}'
```

预期：HTTP 200（SSE），单条事件 `{"type":"ERROR","error":"toolCallId 不能为空"}`。

### 用例 1.7 中断会话
- 方法：`POST /api/interrupt/{sessionId}`
- 目的：验证按 sessionId 精确中断在途调用（2.x：`agent.interrupt(RuntimeContext.builder().sessionId(id).build())`）。

```bash
curl -X POST "http://localhost:8080/api/interrupt/s-1002"
```

预期：HTTP 200，返回 `{"success":true,"message":"代理已中断"}`。

### 用例 1.8 清空会话
- 方法：`DELETE /api/session/{sessionId}`
- 目的：验证按 `(userId=null, sessionId)` 删除持久化会话状态，并清理该会话的审批记录。

```bash
curl -X DELETE "http://localhost:8080/api/session/s-1002"
```

预期：HTTP 200，返回 `{"success":true,"message":"会话已清空"}`；
随后 `GET /api/approval/session/s-1002` 应返回空数组。

---

## 二、ApprovalController（`/api/approval`）

### 用例 2.1 查看全部待审批请求
- 方法：`GET /api/approval/pending`

```bash
curl "http://localhost:8080/api/approval/pending"
```

预期：HTTP 200，返回 `PENDING` 状态的审批请求数组；在用例 1.2 之后至少有 1 条，
每条含 `id/sessionId/toolCallId/toolName/toolInput/status/createdAt`。

### 用例 2.2 查看指定会话的待审批请求
- 方法：`GET /api/approval/pending/{sessionId}`

```bash
curl "http://localhost:8080/api/approval/pending/s-1002"
```

预期：HTTP 200，仅返回 `sessionId=s-1002` 且 `status=PENDING` 的记录。

### 用例 2.3 查看指定会话的全部审批请求
- 方法：`GET /api/approval/session/{sessionId}`

```bash
curl "http://localhost:8080/api/approval/session/s-1002"
```

预期：HTTP 200，返回该会话所有状态的审批请求（不限 `PENDING`）。

### 用例 2.4 查看单条审批详情
- 方法：`GET /api/approval/{id}`

```bash
curl "http://localhost:8080/api/approval/1"
```

预期：存在则 HTTP 200 返回该记录；不存在则 HTTP 404。

### 用例 2.5 手动审批（按审批 ID）
- 方法：`POST /api/approval/{id}`
- 说明：仅用于管理后台/流程系统直接决策，不驱动 Agent 恢复（恢复请用 `/api/approve` 或 `/api/workflow/resume`）。

```bash
curl -X POST "http://localhost:8080/api/approval/1?approved=true&approver=admin&comment=OK"
```

预期：状态为 `PENDING` 时 HTTP 200，返回 `success=true` 及新的 `status`；
非 `PENDING` 或不存在时 HTTP 400，`success=false`。

### 用例 2.6 手动审批（按 sessionId + toolCallId）
- 方法：`POST /api/approval/tool-call/{sessionId}/{toolCallId}`

```bash
curl -X POST "http://localhost:8080/api/approval/tool-call/s-1002/<toolCallId>?approved=false&approver=admin&comment=拒绝"
```

预期：命中则 HTTP 200，返回 `success=true`、`toolCallId`、`status`；否则 HTTP 400。

### 用例 2.7 批量拒绝会话内待审批项
- 方法：`POST /api/approval/reject-all/{sessionId}`

```bash
curl -X POST "http://localhost:8080/api/approval/reject-all/s-1002?approver=admin&reason=会话取消"
```

预期：HTTP 200，返回 `success=true`、`rejectedCount`（本次处理数量）、`sessionId`。

### 用例 2.8 批量批准会话内待审批项
- 方法：`POST /api/approval/approve-all/{sessionId}`

```bash
curl -X POST "http://localhost:8080/api/approval/approve-all/s-1002?approver=admin&reason=统一放行"
```

预期：HTTP 200，返回 `success=true`、`approvedCount`、`sessionId`。

### 用例 2.9 查询审批统计
- 方法：`GET /api/approval/stats`

```bash
curl "http://localhost:8080/api/approval/stats"
```

预期：HTTP 200，返回 `total/pending/approved/rejected/timeout` 五项计数。

---

## 三、WorkflowController（`/api/workflow`）

### 用例 3.1 审批后恢复工作流（SSE，表单参数）
- 方法：`POST /api/workflow/resume`
- 说明：等价于 `/api/approve`，但以查询/表单参数传递。

```bash
curl -N -X POST "http://localhost:8080/api/workflow/resume" \
  --data-urlencode "sessionId=s-1002" \
  --data-urlencode "toolCallId=<toolCallId>" \
  --data-urlencode "approved=true" \
  --data-urlencode "approver=admin" \
  --data-urlencode "comment=同意"
```

预期：HTTP 200（SSE），恢复执行并输出后续事件，最后 `COMPLETE`。

### 用例 3.2 中断工作流
- 方法：`POST /api/workflow/interrupt/{sessionId}`

```bash
curl -X POST "http://localhost:8080/api/workflow/interrupt/s-1002"
```

预期：HTTP 200，返回 `{"success":true,"message":"工作流已中断","sessionId":"s-1002"}`。

### 用例 3.3 重置（清空）工作流
- 方法：`DELETE /api/workflow/reset/{sessionId}`

```bash
curl -X DELETE "http://localhost:8080/api/workflow/reset/s-1002"
```

预期：HTTP 200，返回 `{"success":true,"message":"工作流已重置","sessionId":"s-1002"}`；
底层同样按 `(null, sessionId)` 删除持久化会话状态并清理审批记录。

### 用例 3.4 查询工作流状态（占位实现）
- 方法：`GET /api/workflow/status/{sessionId}`

```bash
curl "http://localhost:8080/api/workflow/status/s-1002"
```

预期：HTTP 200，返回 `sessionId`、`status="UNKNOWN"`、`message="工作流状态查询功能开发中"`（当前为占位）。

---

## 四、端到端建议流程

1. 用例 1.2 发送触发 `send_email` 的消息，从 SSE 中记录 `toolCallId`。
2. 用例 2.1 / 2.2 确认已生成 `PENDING` 审批记录。
3. 用例 1.4（或 3.1）以该 `toolCallId` 提交“批准”，观察 Agent 恢复并完成；用例 2.4 确认状态变为 `APPROVED`。
4. 另起一个会话重复步骤 1，改用用例 1.5 提交“拒绝”，确认状态变为 `REJECTED`。
5. 用例 2.9 查看统计计数；用例 1.8 / 3.3 清空会话后，`/api/approval/session/{id}` 返回空。

> 说明：涉及模型推理与工具调用的用例（1.1/1.2/1.4/1.5/3.1）依赖真实 `DASHSCOPE_API_KEY`；
> 纯审批查询/统计接口（`/api/approval/*` 只读、`/api/approval/{id}` 手动审批）可脱离模型独立验证。
> 审批超时由 `ApprovalService.handleTimeoutRequests()` 定时任务（每 60 秒）处理，超时记录状态置为 `TIMEOUT`。
