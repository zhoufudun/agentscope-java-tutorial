# AgentScope Java 教程 - 第四章：ReAct Agent 详解

## 项目概述

本项目是 AgentScope Java 教程的第四章示例，展示了如何在 Spring Boot 环境中配置和使用带工具调用的 ReActAgent。

## 功能特性

- **多个自定义工具**：天气查询、计算器、日期时间工具
- **工具注册与配置**：使用 `@Tool` 注解定义工具
- **Agent 生命周期管理**：完整的 Agent 创建和生命周期钩子
- **状态监控**：通过 Hook 监听 Agent 状态变化
- **流式响应**：支持流式输出推理和工具执行结果

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.2.5
- **AgentScope**: 2.0.0
- **构建工具**: Maven

## 项目结构

```
chapter04-react-agent/
├── pom.xml                                    # Maven 配置
├── src/main/java/io/agentscope/tutorial/chapter04/
│   ├── Chapter04Application.java              # Spring Boot 启动类
│   ├── config/
│   │   └── AgentConfig.java                  # Agent 配置类
│   ├── tool/
│   │   ├── WeatherTool.java                  # 天气查询工具
│   │   ├── CalculatorTool.java               # 计算器工具
│   │   └── DateTimeTool.java                 # 日期时间工具
│   ├── service/
│   │   └── AgentService.java                 # Agent 服务层
│   ├── hook/
│   │   └── LoggingHook.java                  # 日志钩子
│   └── controller/
│       └── ChatController.java               # REST API 控制器
├── src/main/resources/
│   ├── application.yml                       # 应用配置
│   └── application-dev.yml                   # 开发环境配置
└── src/test/java/io/agentscope/tutorial/chapter04/
    └── ReActAgentTest.java                   # 单元测试
```

## 快速开始

### 1. 设置环境变量

```bash
# 设置 DashScope API Key
export DASHSCOPE_API_KEY=your_api_key_here

# 可选：设置模型名称（默认 qwen-plus）
export MODEL_NAME=qwen-plus
```

### 2. 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run

# 或者打包后运行
mvn package
java -jar target/chapter04-react-agent-1.0.0.jar
```

### 3. 运行测试

```bash
# 运行单元测试
mvn test

# 跳过集成测试（没有 API Key 时）
SKIP_INTEGRATION_TESTS=true mvn test
```

## API 使用示例

### 1. 同步聊天

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "北京现在的天气怎么样？"}'
```

### 2. 流式聊天

```bash
curl -X POST http://localhost:8080/api/agent/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "请帮我计算 25*4+10 的结果"}' \
  --no-buffer
```

### 3. 查看对话历史

```bash
curl http://localhost:8080/api/agent/history
```

### 4. 查看 Agent 状态

```bash
curl http://localhost:8080/api/agent/status
```

### 5. 清除对话历史

```bash
curl -X DELETE http://localhost:8080/api/agent/history
```

### 6. 中断 Agent 执行

```bash
curl -X POST http://localhost:8080/api/agent/interrupt
```

## 可用工具

### 天气工具

- `get_current_weather(city)`: 获取指定城市的当前天气
- `get_weather_forecast(city, days)`: 获取天气预报
- `get_weather_index(city, index_type)`: 获取生活指数
- `get_supported_index_types()`: 获取支持的指数类型

### 计算器工具

- `calculate(expression)`: 执行数学计算
- `convert_unit(value, from_unit, to_unit)`: 单位换算

### 日期时间工具

- `get_current_time(timezone, format)`: 获取当前时间
- `calculate_days_between(start_date, end_date)`: 计算日期差
- `get_day_of_week(date)`: 获取星期几

## 核心代码示例

### 定义工具

```java
public class MyTools {

    @Tool(
        name = "my_tool",
        description = "工具描述"
    )
    public Mono<String> myTool(
        @ToolParam(name = "param", description = "参数描述")
        String param) {

        return Mono.fromCallable(() -> {
            // 工具实现
            return "结果";
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

### 配置 Agent

```java
@Bean
public ReActAgent reactAgent(Model model, Toolkit toolkit, Hook loggingHook) {
    return ReActAgent.builder()
        .name("小探")
        .sysPrompt("你是一个智能助手...")
        .model(model)
        .toolkit(toolkit)
        .memory(new InMemoryMemory())
        .maxIters(15)
        .hooks(List.of(loggingHook))
        .build();
}
```

### 使用 Hook 监听状态

```java
public class MyHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return Mono.fromCallable(() -> {
            if (event instanceof PreCallEvent e) {
                System.out.println("Agent 开始执行");
            } else if (event instanceof PostActingEvent e) {
                System.out.println("工具执行完成: " + e.getToolUse().getName());
            } else if (event instanceof PostCallEvent e) {
                System.out.println("Agent 执行完成");
            }
            return event;
        });
    }
}
```

## 注意事项

1. **API Key**: 需要设置 `DASHSCOPE_API_KEY` 环境变量
2. **网络**: 天气工具为模拟实现，实际项目中可替换为真实 API
3. **超时**: 默认工具执行超时为 60 秒
4. **迭代次数**: 默认最大迭代次数为 15 次

## 故障排除

### 编译失败

如果遇到依赖解析问题，先在父目录构建：

```bash
cd .. # 回到 agentscope-java 根目录
mvn install -DskipTests
```

### API 调用失败

1. 检查 `DASHSCOPE_API_KEY` 是否正确设置
2. 检查网络连接
3. 查看应用日志 `logs/agentscope-trace.jsonl`

## 相关资源

- [AgentScope Java 官方文档](https://agentscope.io)
- [第四章教程文档](../chapter04-react-agent.md)
- [ReAct Agent 源码](../../agentscope-core/src/main/java/io/agentscope/core/ReActAgent.java)