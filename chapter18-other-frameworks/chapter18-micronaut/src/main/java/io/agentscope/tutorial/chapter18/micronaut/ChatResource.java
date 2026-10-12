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
package io.agentscope.tutorial.chapter18.micronaut;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Micronaut REST 控制器 - AgentScope 聊天端点。
 *
 * <h2>概述</h2>
 * <p>本控制器演示如何在 Micronaut 框架中创建 REST API 与 AgentScope Agent 交互。
 *
 * <h2>Micronaut 特性演示</h2>
 * <ul>
 *   <li><b>@Controller</b> - 标记 HTTP 控制器，Micronaut 在编译时处理路由</li>
 *   <li><b>ReActAgent 注入</b> - 构造函数注入自动配置的 Agent</li>
 *   <li><b>@Serdeable DTO</b> - 使用 Micronaut Serde 替代 Jackson，性能更好</li>
 *   <li><b>Netty HTTP</b> - 基于 Netty 的非阻塞 HTTP 处理</li>
 * </ul>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /chat - 单轮对话</li>
 *   <li>POST /chat/multi - 多轮对话</li>
 *   <li>GET /health - 健康检查</li>
 *   <li>GET /echo/{message} - 回显测试</li>
 * </ul>
 *
 * <h2>请求示例</h2>
 * <pre>
 * # 聊天端点
 * curl -X POST http://localhost:8080/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"Hello, who are you?"}'
 *
 * # 健康检查端点
 * curl http://localhost:8080/health
 * </pre>
 *
 * @author AgentScope Tutorial
 * @see io.micronaut.http.annotation.Controller
 */
@Controller  // Micronaut 控制器注解，路由在编译时解析
public class ChatResource {

    /** 自动配置的 ReActAgent 实例 */
    private final ReActAgent agent;

    /** 来自 AgentFactory 的聊天服务 */
    private final AgentFactory.ChatService chatService;

    /**
     * 构造 ChatResource，注入依赖。
     *
     * <h3>依赖注入</h3>
     * <p>Micronaut 使用构造函数注入，在编译时解析依赖关系，
     * 相比运行时反射注入具有更好的性能和启动速度。
     *
     * @param agent       自动配置的 ReActAgent 实例
     * @param chatService AgentFactory 创建的聊天服务
     */
    public ChatResource(ReActAgent agent, AgentFactory.ChatService chatService) {
        this.agent = agent;
        this.chatService = chatService;
    }

    /**
     * 聊天端点 - 发送消息给 Agent 并返回响应。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>验证请求参数 - 消息不能为空</li>
     *   <li>构建用户消息 - 使用 Msg.builder()</li>
     *   <li>调用 Agent - 阻塞等待响应</li>
     *   <li>返回响应 - 包装为 HTTP 响应</li>
     * </ol>
     *
     * @param request 包含用户消息的聊天请求
     * @return Agent 响应，包装在 HttpResponse 中
     */
    @Post("/chat")  // POST 方法映射到 /chat 路径
    @Consumes(MediaType.APPLICATION_JSON)  // 接受 JSON 请求体
    @Produces(MediaType.APPLICATION_JSON)  // 返回 JSON 响应
    public HttpResponse<?> chat(@Body ChatRequest request) {
        // 参数验证：消息不能为空或仅包含空白字符
        if (request == null || request.message() == null || request.message().isBlank()) {
            return HttpResponse.badRequest(new ErrorResponse("Message cannot be empty"));
        }

        try {
            // 构建用户消息对象
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)  // 设置消息角色为用户
                    .content(TextBlock.builder().text(request.message()).build())  // 设置消息内容
                    .build();

            // 调用 Agent 并阻塞等待响应
            Msg response = agent.call(userMsg).block();
            // 安全提取响应文本
            String responseText = response != null ? response.getTextContent() : "No response";

            // 返回成功响应
            return HttpResponse.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            // 异常处理：返回 500 错误
            return HttpResponse.serverError(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * 健康检查端点 - 返回 Agent 状态信息。
     *
     * <h3>用途</h3>
     * <ul>
     *   <li>容器编排健康检查（K8s liveness/readiness）</li>
     *   <li>负载均衡器健康探测</li>
     *   <li>监控系统状态采集</li>
     * </ul>
     *
     * @return 健康状态响应，包含 Agent 名称和框架信息
     */
    @Get("/health")  // GET 方法映射到 /health 路径
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<HealthResponse> health() {
        return HttpResponse.ok(new HealthResponse(
                "AgentScope Agent is ready",  // 状态消息
                agent.getName(),              // Agent 名称
                "Micronaut"                   // 框架标识
        ));
    }

    /**
     * 多轮对话端点 - 处理包含多条消息的对话。
     *
     * <h3>使用场景</h3>
     * <p>当需要在单个请求中发送多条消息时使用，例如：
     * <ul>
     *   <li>恢复之前的对话上下文</li>
     *   <li>批量处理多个问题</li>
     *   <li>模拟多轮对话历史</li>
     * </ul>
     *
     * @param request 包含多条消息的请求
     * @return Agent 响应
     */
    @Post("/chat/multi")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> multiChat(@Body MultiChatRequest request) {
        // 参数验证：消息列表不能为空
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return HttpResponse.badRequest(new ErrorResponse("Messages cannot be empty"));
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
            Msg response = agent.call(userMsg).block();
            String responseText = response != null ? response.getTextContent() : "No response";

            return HttpResponse.ok(new ChatResponse(responseText));

        } catch (Exception e) {
            return HttpResponse.serverError(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * 回显端点 - 演示使用 ChatService 和路径变量。
     *
     * <h3>演示要点</h3>
     * <ul>
     *   <li>@PathVariable 获取 URL 路径参数</li>
     *   <li>使用 Service 层而非直接调用 Agent</li>
     * </ul>
     *
     * @param message URL 路径中的消息
     * @return 回显响应，包含 Agent 信息
     */
    @Get("/echo/{message}")  // 路径参数示例
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<EchoResponse> echo(@PathVariable String message) {
        return HttpResponse.ok(new EchoResponse(
                chatService.getAgentName(),  // 从 ChatService 获取 Agent 名称
                message,                      // 回显用户消息
                "Micronaut"                   // 框架标识
        ));
    }

    // ==================== 请求/响应 DTO ====================
    // 使用 Java 16+ Record 和 Micronaut @Serdeable 注解
    // @Serdeable 是 Micronaut Serde 的序列化标记，比 Jackson 更高效

    /**
     * 聊天请求 DTO。
     * @param message 用户消息文本
     */
    @Serdeable  // Micronaut Serde 序列化注解，替代 Jackson
    public record ChatRequest(String message) {}

    /**
     * 多轮聊天请求 DTO。
     * @param messages 消息列表
     */
    @Serdeable
    public record MultiChatRequest(java.util.List<String> messages) {}

    /**
     * 聊天响应 DTO。
     * @param response Agent 响应文本
     */
    @Serdeable
    public record ChatResponse(String response) {}

    /**
     * 错误响应 DTO。
     * @param error 错误信息
     */
    @Serdeable
    public record ErrorResponse(String error) {}

    /**
     * 健康检查响应 DTO。
     * @param status    状态消息
     * @param agentName Agent 名称
     * @param framework 框架名称
     */
    @Serdeable
    public record HealthResponse(String status, String agentName, String framework) {}

    /**
     * 回显响应 DTO。
     * @param agentName Agent 名称
     * @param message   回显的消息
     * @param framework 框架名称
     */
    @Serdeable
    public record EchoResponse(String agentName, String message, String framework) {}
}