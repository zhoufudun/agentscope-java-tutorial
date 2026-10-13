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
package io.agentscope.tutorial.chapter18.quarkus;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Quarkus JAX-RS REST 控制器 - AgentScope 聊天端点。
 *
 * <h2>概述</h2>
 * <p>本控制器演示如何在 Quarkus 框架中使用 JAX-RS 创建 REST API 与 AgentScope Agent 交互。
 * Quarkus 使用 RESTEasy 作为 JAX-RS 实现。
 *
 * <h2>Quarkus REST 特性演示</h2>
 * <ul>
 *   <li><b>@Path</b> - JAX-RS 路径映射注解</li>
 *   <li><b>@Inject</b> - CDI 字段注入 ReActAgent</li>
 *   <li><b>AgentBean 注入</b> - 注入业务逻辑封装类</li>
 *   <li><b>Record DTO</b> - 使用 Java 16+ Record 定义请求/响应 DTO</li>
 *   <li><b>Response</b> - JAX-RS Response 对象处理 HTTP 响应</li>
 * </ul>
 *
 * <h2>JAX-RS vs Spring MVC</h2>
 * <table border="1">
 *   <tr><th>特性</th><th>JAX-RS (Quarkus)</th><th>Spring MVC</th></tr>
 *   <tr><td>规范</td><td>Jakarta EE 标准</td><td>Spring 私有</td></tr>
 *   <tr><td>路径注解</td><td>@Path</td><td>@RequestMapping</td></tr>
 *   <tr><td>方法注解</td><td>@GET/@POST</td><td>@GetMapping/@PostMapping</td></tr>
 *   <tr><td>响应类型</td><td>Response</td><td>ResponseEntity</td></tr>
 * </table>
 *
 * <h2>请求示例</h2>
 * <pre>
 * # 聊天端点
 * curl -X POST http://localhost:8080/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"Hello, who are you?"}'
 *
 * # 健康检查
 * curl http://localhost:8080/health
 * </pre>
 *
 * @author AgentScope Tutorial
 * @see jakarta.ws.rs.Path
 * @see jakarta.ws.rs.core.Response
 */
@Path("/")  // JAX-RS 根路径，所有端点都基于此路径
@Produces(MediaType.APPLICATION_JSON)  // 默认响应类型为 JSON
@Consumes(MediaType.APPLICATION_JSON)  // 默认接受 JSON 请求
public class ChatResource {

    /**
     * 自动配置的 ReActAgent。
     * <p>使用 CDI 字段注入，由 agentscope-quarkus-extension 自动配置。
     */
    @Inject  // CDI 字段注入
    ReActAgent agent;

    /**
     * 业务逻辑封装 Bean。
     * <p>提供对 Agent 的高级封装，包含日志和状态查询功能。
     */
    @Inject
    AgentBean agentBean;

    /**
     * 聊天端点 - 发送消息给 Agent 并返回响应。
     *
     * <h3>处理流程</h3>
     * <ol>
     *   <li>验证请求参数 - 消息不能为空</li>
     *   <li>构建用户消息 - 使用 Msg.builder()</li>
     *   <li>调用 Agent - 阻塞等待响应</li>
     *   <li>返回 JAX-RS Response</li>
     * </ol>
     *
     * @param request 包含用户消息的聊天请求 DTO
     * @return JAX-RS Response，包含 Agent 响应或错误信息
     */
    @POST  // HTTP POST 方法
    @Path("/chat")  // 映射到 /chat 路径
    public Response chat(ChatRequest request) {
        // 参数验证：消息不能为空或仅包含空白字符
        if (request == null || request.message() == null || request.message().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Message cannot be empty"))
                    .build();
        }

        try {
            // 构建用户消息对象
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)  // 消息角色：用户
                    .content(TextBlock.builder().text(request.message()).build())  // 消息内容
                    .build();

            // 调用 Agent 并阻塞等待响应
            Msg response = agent.call(userMsg).block();
            // 安全提取响应文本
            String responseText = response != null ? response.getTextContent() : "No response";

            // 返回成功响应（HTTP 200）
            return Response.ok(new ChatResponse(responseText)).build();

        } catch (Exception e) {
            // 异常处理：返回 500 错误
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 健康检查端点 - 返回 Agent 状态信息。
     *
     * <h3>用途</h3>
     * <ul>
     *   <li>Kubernetes liveness/readiness 探针</li>
     *   <li>负载均衡器健康检查</li>
     *   <li>Quarkus SmallRye Health 集成</li>
     * </ul>
     *
     * @return 健康状态响应，包含 Agent 名称和框架标识
     */
    @GET  // HTTP GET 方法
    @Path("/health")
    public Response health() {
        return Response.ok(new HealthResponse(
                agentBean.getStatus(),  // 从 AgentBean 获取状态
                agent.getName(),        // Agent 名称
                "Quarkus"               // 框架标识
        )).build();
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
     * @return JAX-RS Response，包含 Agent 响应
     */
    @POST
    @Path("/chat/multi")
    public Response multiChat(MultiChatRequest request) {
        // 参数验证：消息列表不能为空
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Messages cannot be empty"))
                    .build();
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

            return Response.ok(new ChatResponse(responseText)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 回显端点 - 演示使用 AgentBean 获取信息。
     *
     * <h3>演示要点</h3>
     * <ul>
     *   <li>使用 Service Bean 而非直接调用 Agent</li>
     *   <li>GET 方法的简单响应</li>
     * </ul>
     *
     * @return 回显响应，包含 Agent 信息
     */
    @GET
    @Path("/echo")
    public Response echo() {
        return Response.ok(new EchoResponse(
                agent.getName(),       // Agent 名称
                agentBean.getStatus(), // Agent 状态
                "Quarkus"              // 框架标识
        )).build();
    }

    // ==================== 请求/响应 DTO ====================
    // 使用 Java 16+ Record 定义不可变数据载体
    // Quarkus 使用 Jackson 或 JSON-B 自动序列化/反序列化

    /**
     * 聊天请求 DTO。
     * @param message 用户消息文本
     */
    public record ChatRequest(String message) {}

    /**
     * 多轮聊天请求 DTO。
     * @param messages 消息列表
     */
    public record MultiChatRequest(java.util.List<String> messages) {}

    /**
     * 聊天响应 DTO。
     * @param response Agent 响应文本
     */
    public record ChatResponse(String response) {}

    /**
     * 错误响应 DTO。
     * @param error 错误信息
     */
    public record ErrorResponse(String error) {}

    /**
     * 健康检查响应 DTO。
     * @param status    状态描述
     * @param agentName Agent 名称
     * @param framework 框架名称
     */
    public record HealthResponse(String status, String agentName, String framework) {}

    /**
     * 回显响应 DTO。
     * @param agentName Agent 名称
     * @param status    Agent 状态
     * @param framework 框架名称
     */
    public record EchoResponse(String agentName, String status, String framework) {}
}