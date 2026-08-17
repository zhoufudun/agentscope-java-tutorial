# Chapter07 记忆系统 - HTTP 接口测试用例

本文件为 `chapter07-memory` 模块对外暴露的 REST 接口提供可执行的测试用例（基于 `curl`）。
接口迁移到 AgentScope 2.x 后，会话历史由 `AgentState` 维护、并通过 `AgentStateStore` 持久化，
而业务侧的记忆记录仍由 JPA（`MemoryRecord` / `MemoryRepository`）落库。以下用例覆盖两类接口：

- `ChatController`（前缀 `/api/chat`）
- `MemoryController`（前缀 `/api/memory`）

> 运行前提：`mvn -pl chapter07-memory spring-boot:run` 启动服务（默认端口 8080），
> 并配置好 `DASHSCOPE_API_KEY`（涉及模型调用的用例需要）。

---

## 一、ChatController（`/api/chat`）

### 用例 1.1 发送消息（正常）
- 方法：`POST /api/chat/send`
- 目的：验证消息可被处理并返回回复与记忆统计。

```bash
curl -X POST "http://localhost:8080/api/chat/send?sessionId=user001" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，我叫张三"}'
```

预期：HTTP 200，返回体包含 `sessionId=user001`、`response` 非空、`memoryStats.totalMessages >= 1`。

### 用例 1.2 发送空消息（异常校验）
- 方法：`POST /api/chat/send`
- 目的：验证空消息返回 400。

```bash
curl -i -X POST "http://localhost:8080/api/chat/send?sessionId=user001" \
  -H "Content-Type: application/json" \
  -d '{"message": ""}'
```

预期：HTTP 400，返回体 `{"error":"Message cannot be empty"}`。

### 用例 1.3 携带上下文发送
- 方法：`POST /api/chat/send-with-context`
- 目的：验证 `context` 会被前置拼接到用户消息。

```bash
curl -X POST "http://localhost:8080/api/chat/send-with-context?sessionId=user001" \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我总结一下", "context": "当前工单编号 12345"}'
```

预期：HTTP 200，`response` 非空；对话记忆中包含拼接后的用户消息。

### 用例 1.4 查询对话历史
- 方法：`GET /api/chat/history`
- 目的：验证历史返回按顺序排列，`limit` 生效。

```bash
curl "http://localhost:8080/api/chat/history?sessionId=user001&limit=5"
```

预期：HTTP 200，`count <= 5`，`history` 为消息数组（含 role/content/messageIndex）。

### 用例 1.5 查询记忆统计
- 方法：`GET /api/chat/stats`

```bash
curl "http://localhost:8080/api/chat/stats?sessionId=user001"
```

预期：HTTP 200，返回 `totalMessages/userMessages/assistantMessages/cachedCount`。

### 用例 1.6 服务信息根路径

```bash
curl "http://localhost:8080/api/chat"
```

预期：HTTP 200，返回服务名与可用端点列表。

---

## 二、MemoryController（`/api/memory`）

### 用例 2.1 查询历史
- 方法：`GET /api/memory/history`

```bash
curl "http://localhost:8080/api/memory/history?sessionId=user001"
```

预期：HTTP 200，`history` 数组，`count` 与库中记录数一致。

### 用例 2.2 记忆统计

```bash
curl "http://localhost:8080/api/memory/stats?sessionId=user001"
```

预期：HTTP 200，统计字段齐全。

### 用例 2.3 关键词检索（会话内）
- 方法：`GET /api/memory/search`

```bash
curl "http://localhost:8080/api/memory/search?sessionId=user001&keyword=张三"
```

预期：HTTP 200，`acrossSessions=false`，`results` 仅包含 `user001` 会话中命中的记录。

### 用例 2.4 关键词检索（跨会话）

```bash
curl "http://localhost:8080/api/memory/search?sessionId=user001&keyword=张三&acrossSessions=true"
```

预期：HTTP 200，`acrossSessions=true`，`results` 可包含其他会话命中的记录。

### 用例 2.5 最近消息
- 方法：`GET /api/memory/recent`

```bash
curl "http://localhost:8080/api/memory/recent?sessionId=user001&limit=3"
```

预期：HTTP 200，`recent` 至多 3 条，按时间/索引排序。

### 用例 2.6 清空会话记忆
- 方法：`DELETE /api/memory/clear`
- 目的：验证同时清除 JPA 记录与 `AgentStateStore` 中该会话的 `AgentState`。

```bash
curl -X DELETE "http://localhost:8080/api/memory/clear?sessionId=user001"
```

预期：HTTP 200，`message="Memory cleared successfully"`；随后调用 2.1 应返回 `count=0`。

### 用例 2.7 列出全部会话
- 方法：`GET /api/memory/sessions`

```bash
curl "http://localhost:8080/api/memory/sessions"
```

预期：HTTP 200，`sessions` 为去重后的会话 ID 列表。

### 用例 2.8 手动触发记忆压缩
- 方法：`POST /api/memory/compress`

```bash
curl -X POST "http://localhost:8080/api/memory/compress?sessionId=user001"
```

预期：HTTP 200，返回 `messagesBefore/messagesAfter/compressed`；未达阈值时 `compressed=0`。

### 用例 2.9 服务信息根路径

```bash
curl "http://localhost:8080/api/memory"
```

预期：HTTP 200，返回服务名与端点列表。

---

## 三、端到端建议流程

1. 用例 1.1 连续发送 2~3 条消息（同一 `sessionId`），建立会话历史。
2. 用例 1.4 / 2.1 校验历史条数递增。
3. 用例 2.3 检索刚才提到的关键词，确认可命中。
4. 用例 2.6 清空会话，再用 2.1 校验 `count=0`，同时 `AgentState` 上下文被清除。

> 说明：涉及模型推理的用例（1.1 / 1.3）依赖真实 `DASHSCOPE_API_KEY`；
> 若仅验证记忆存取与检索逻辑，可直接调用 `/api/memory/*` 系列只读接口。
