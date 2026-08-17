# Chapter17 部署模块 —— HTTP 接口测试用例

本文件记录 `chapter17-deployment` 模块 REST 接口的测试用例，配套自动化测试类：

- `src/test/java/io/agentscope/tutorial/chapter17/controller/ChatControllerHttpTest.java`

运行方式（在教程根目录执行）：

```bash
mvn -pl chapter17-deployment test
```

---

## 一、接口清单

| 方法   | 路径                                   | 说明             | 是否依赖外部服务            |
|--------|----------------------------------------|------------------|-----------------------------|
| GET    | `/api/v1/info`                         | 构建 / Git 信息  | 否                          |
| GET    | `/api/v1/stats`                        | 服务调用统计     | 否                          |
| DELETE | `/api/v1/session/{sessionId}`          | 清除会话         | 否                          |
| POST   | `/api/v1/chat`                         | 同步对话         | 是（DashScope，需 AI 模型） |
| POST   | `/api/v1/chat/async`                   | 异步对话         | 是（DashScope）             |
| GET    | `/api/v1/chat/stream/{sessionId}`      | 流式对话（SSE）  | 是（DashScope）             |

> 说明：`/chat` 系列的正向调用需要真实的大模型（DashScope）；在无外部依赖的测试环境中，
> 仅对请求体做参数校验验证（非法入参返回 400），不做正向业务断言，避免测试不稳定。

---

## 二、已覆盖的自动化测试用例

测试基于 `@SpringBootTest + @AutoConfigureMockMvc`，激活 `test` profile，
并通过 `@TestPropertySource` 注入 H2 内存库、关闭 DashScope（`agentscope.dashscope.enabled=false`）。

### TC-01 GET /api/v1/info 返回构建信息

- 前置条件：测试类路径下存在 `META-INF/build-info.properties` 与 `git.properties`
  （见 `src/test/resources/`），据此自动装配 `BuildProperties` / `GitProperties`。
- 请求：`GET /api/v1/info`
- 期望：
  - HTTP 状态码 `200`
  - `$.version == "1.0.0"`
  - `$.gitBranch == "test"`
  - `$.name` 存在

### TC-02 GET /api/v1/stats 返回初始统计

- 请求：`GET /api/v1/stats`
- 期望：
  - HTTP 状态码 `200`
  - `$.totalRequests == 0`
  - `$.totalTokens == 0`
- 说明：初始时 Micrometer 中尚无 `agentscope.chat.*` 指标，服务返回计数为 0。

### TC-03 DELETE /api/v1/session/{sessionId} 清除会话

- 请求：`DELETE /api/v1/session/test-session-1`
- 期望：
  - HTTP 状态码 `204 No Content`
- 说明：无论会话是否存在，删除均为幂等操作。

### TC-04 POST /api/v1/chat 空请求体校验

- 请求：`POST /api/v1/chat`，body = `{}`
- 期望：
  - HTTP 状态码 `400 Bad Request`
- 说明：`userId` / `sessionId` / `message` 均标注 `@NotBlank`，缺失时由 `@Valid` 触发校验失败。

### TC-05 POST /api/v1/chat 缺少 message 字段校验

- 请求：`POST /api/v1/chat`，body = `{"userId":"u1","sessionId":"s1"}`
- 期望：
  - HTTP 状态码 `400 Bad Request`
- 说明：`message` 缺失触发 `@NotBlank` 校验失败。

---

## 三、依赖外部服务、需手动 / 集成环境验证的用例

以下用例需要真实的大模型（DashScope）或中间件，未纳入无依赖单元测试，建议在集成环境中验证。

### TC-06 POST /api/v1/chat 正向对话（需 DashScope）

- 前置：配置 `DASHSCOPE_API_KEY`，`agentscope.dashscope.enabled=true`。
- 请求示例：

  ```bash
  curl -X POST http://localhost:8080/api/v1/chat \
    -H "Content-Type: application/json" \
    -d '{"userId":"u1","sessionId":"s1","message":"你好"}'
  ```

- 期望：HTTP `200`，`$.success == true`，`$.message` 为模型回复文本。

### TC-07 POST /api/v1/chat/async 异步对话（需 DashScope）

- 请求：同 TC-06，路径改为 `/api/v1/chat/async`。
- 期望：HTTP `202 Accepted`，返回 `taskId` 与 `status == "ACCEPTED"`。

### TC-08 GET /api/v1/chat/stream/{sessionId} 流式对话（需 DashScope）

- 请求：`GET /api/v1/chat/stream/s1?userId=u1&message=你好`，`Accept: text/event-stream`
- 期望：以 SSE 形式持续返回 `message` 事件，最终返回 `done` 事件（`[DONE]`）。

### TC-09 健康检查（Actuator / HealthController）

- `GET /actuator/health/liveness`：存活探针，期望 `200`。
- `GET /actuator/health/readiness`：就绪探针，依赖数据库与 Redis；
  在无 Redis 的测试环境返回 `503`，Redis 正常时返回 `200`。

---

## 四、备注

- 上述无依赖用例已随 `mvn -pl chapter17-deployment test` 自动执行并通过。
- 测试所需的 `build-info.properties` / `git.properties` 仅放置于 `src/test/resources`，
  用于测试期装配构建 / Git 信息 Bean；生产环境应由构建流程生成（见根目录说明或本模块 README）。
