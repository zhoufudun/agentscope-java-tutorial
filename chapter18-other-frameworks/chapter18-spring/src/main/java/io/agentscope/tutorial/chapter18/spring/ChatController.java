/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter18.spring;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot REST 控制器 - AgentScope 聊天端点。
 *
 * <h2>概述</h2>
 * <p>本控制器演示如何在 Spring Boot 中创建 REST API 与 AgentScope Agent 交互。
 * 使用 Spring MVC 注解定义端点，并注入自动配置的 ReActAgent。
 *
 * <h2>Spring MVC 特性演示</h2>
 * <ul>
 *   <li><b>@RestController</b> - 组合 @Controller 和 @ResponseBody</li>
 *   <li><b>@RequestMapping</b> - 统一路径前缀</li>
 *   <li><b>构造函数注入</b> - 注入 ReActAgent</li>
 *   <li><b>Record DTO</b> - Java 16+ Record 作为请求/响应 DTO</li>
 *   <li><b>ResponseEntity</b> - 灵活的 HTTP 响应控制</li>
 * </ul>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/chat - 单轮对话</li>
 *   <li>POST /api/chat/multi - 多轮对话</li>
 *   <li>GET /api/health - 健康检查</li>
 * </ul>
 *
 * <h2>请求示例</h2>
 * <pre>
 * # 聊天端点
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"Hello, who are you?"}'
 *
 * # 健康检查
 * curl http://localhost:8080/api/health
 * </pre>
 *
 * @author AgentScope Tutorial
 * @see org.springframework.web.bind.annotation.RestController
 * @see org.springframework.http.ResponseEntity
 */
@RestController  // 标记为 REST 控制器，所有方法返回值自动序列化为 JSON
@RequestMapping("/api")  // 所有端点统一使用 /api 前缀
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 自动配置的 ReActAgent 实例 */
    private final ReActAgent agent;

    /**
     * 构造 ChatController，注入自动配置的 ReActAgent。
     *
     * <h3>Spring 构造函数注入</h3>
     * <p>Spring 4.3+ 中，单构造函数的类可省略 @Autowired 注解。
     * 构造函数注入是 Spring 团队推荐的依赖注入方式。
     *
     * @param agent agentscope-spring-boot-starter 自动配置的 ReActAgent
     */
    public ChatController(ReActAgent agent) {
        this.agent = agent;
    }

    /**
     * 聊天端点 - 发送消息给 Agent 并返回响应。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>验证请求参数 - 消息不能为空</li>
     *   <li>记录请求日志</li>
     *   <li>构建用户消息 - 使用 Msg.builder()</li>
     *   <li>调用 Agent - 使用 List.of() 传递消息列表</li>
     *   <li>返回 ResponseEntity</li>
     * </ol>
     *
     * @param request 包含用户消息的聊天请求 DTO
     * @return ResponseEntity 包装的响应或错误
     */
    @PostMapping("/chat")  // 映射 POST /api/chat
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        // 参数验证：消息不能为空或仅包含空白字符
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Message cannot be empty"));
        }

        try {
            // 记录请求日志（用于调试和审计）
            log.info("Received chat request: {}", request.message());

            // 构建用户消息对象
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)  // 消息角色：用户
                    .content(TextBlock.builder().text(request.message()).build())  // 消息内容
                    .build();

            // 调用 Agent 并阻塞等待响应
            // 注意：Spring 版本使用 List.of(userMsg) 传递消息列表
            Msg response = agent.call(List.of(userMsg)).block();

            // 安全提取响应文本
            String responseText = response != null ? response.getTextContent() : "No response";

            // 记录响应日志
            log.info("Agent response: {}", responseText);

            // 返回成功响应（HTTP 200）
            return ResponseEntity.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            // 异常处理：记录错误并返回 500 响应
            log.error("Error processing chat request", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * 健康检查端点 - 返回 Agent 状态信息。
     *
     * <h3>Spring Boot Actuator 集成</h3>
     * <p>除本端点外，还可使用 Spring Boot Actuator 提供的标准健康检查：
     * <ul>
     *   <li>/actuator/health - 综合健康状态</li>
     *   <li>/actuator/info - 应用信息</li>
     *   <li>/actuator/metrics - 应用指标</li>
     * </ul>
     *
     * @return 健康状态 Map，包含 status 和 agentName
     */
    @GetMapping("/health")  // 映射 GET /api/health
    public ResponseEntity<Map<String, String>> health() {
        // 返回简单的健康状态 Map
        return ResponseEntity.ok(Map.of(
                "status", "AgentScope Agent is ready",
                "agentName", agent.getName()
        ));
    }

    /**
     * 多轮对话端点 - 处理包含多条消息的对话。
     *
     * <h3>使用场景</h3>
     * <ul>
     *   <li>恢复之前的对话上下文</li>
     *   <li>批量处理多个问题</li>
     *   <li>模拟多轮对话历史</li>
     * </ul>
     *
     * @param request 包含多条消息的请求 DTO
     * @return ResponseEntity 包装的 Agent 响应
     */
    @PostMapping("/chat/multi")  // 映射 POST /api/chat/multi
    public ResponseEntity<?> multiChat(@RequestBody MultiChatRequest request) {
        // 参数验证：消息列表不能为空
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Messages cannot be empty"));
        }

        try {
            // 将多条消息合并为单个对话文本
            StringBuilder conversation = new StringBuilder();
            for (String msg : request.messages()) {
                conversation.append(msg).append("\n");
            }

            // 构建合并后的用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(conversation.toString()).build())
                    .build();

            // 调用 Agent 并获取响应
            Msg response = agent.call(List.of(userMsg)).block();
            String responseText = response != null ? response.getTextContent() : "No response";

            return ResponseEntity.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            log.error("Error processing multi-turn chat", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    // ==================== 请求/响应 DTO ====================
    // 使用 Java 16+ Record 定义不可变数据载体
    // Spring 使用 Jackson 自动序列化/反序列化

    /**
     * 聊天请求 DTO。
     *
     * <h3>Record 特性</h3>
     * <ul>
     *   <li>不可变 - 所有字段 final</li>
     *   <li>自动生成 equals/hashCode/toString</li>
     *   <li>简洁语法 - 无需 getter/setter</li>
     * </ul>
     *
     * @param message 用户消息文本
     */
    public record ChatRequest(String message) {}

    /**
     * 多轮聊天请求 DTO。
     *
     * @param messages 消息列表，作为对话发送
     */
    public record MultiChatRequest(List<String> messages) {}

    /**
     * 聊天响应 DTO。
     *
     * @param response Agent 响应文本
     */
    public record ChatResponse(String response) {}

    /**
     * 错误响应 DTO。
     *
     * @param error 错误信息
     */
    public record ErrorResponse(String error) {}
}