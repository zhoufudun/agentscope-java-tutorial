/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter17.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 异步聊天响应 DTO.
 *
 * <p>用于异步聊天 API 的响应数据传输对象（Data Transfer Object）。
 * 当客户端发起异步请求时，立即返回此对象告知请求状态。
 *
 * <h2>异步处理流程</h2>
 * <ol>
 *   <li>客户端发送 POST /api/v1/chat/async</li>
 *   <li>服务器立即返回 HTTP 202 + AsyncChatResponse（status=ACCEPTED）</li>
 *   <li>客户端通过 pollUrl 轮询或 WebSocket 等待结果</li>
 *   <li>处理完成后返回 status=COMPLETED 和结果</li>
 * </ol>
 *
 * <h2>任务状态流转</h2>
 * <pre>{@code
 * ACCEPTED -> PROCESSING -> COMPLETED
 *                       \-> FAILED
 * }</pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 创建已接受响应
 * AsyncChatResponse response = AsyncChatResponse.accepted("task-123");
 * // response.status = "ACCEPTED"
 * // response.pollUrl = "/api/v1/chat/async/status/task-123"
 *
 * // 处理完成后
 * AsyncChatResponse completed = AsyncChatResponse.completed("task-123", "AI的回答");
 * }</pre>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see ChatController#chatAsync(ChatRequest)
 */
@Data  // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@Builder  // Lombok: 支持 Builder 模式构造对象
@NoArgsConstructor  // Lombok: 无参构造器（Jackson 反序列化需要）
@AllArgsConstructor  // Lombok: 全参构造器（Builder 需要）
@JsonInclude(JsonInclude.Include.NON_NULL)  // Jackson: 序列化时忽略 null 值字段
public class AsyncChatResponse {

    /**
     * 任务 ID.
     *
     * <p>唯一标识此异步任务，用于后续查询状态和结果。
     * 通常使用 UUID 生成。
     */
    private String taskId;

    /**
     * 任务状态.
     *
     * <p>可能的值：
     * <ul>
     *   <li>ACCEPTED - 请求已接受，等待处理</li>
     *   <li>PROCESSING - 正在处理中</li>
     *   <li>COMPLETED - 处理完成</li>
     *   <li>FAILED - 处理失败</li>
     * </ul>
     */
    private String status;

    /**
     * 状态消息.
     *
     * <p>当 status=COMPLETED 时，包含 AI 响应内容。
     * 当 status=FAILED 时，包含错误信息。
     */
    private String message;

    /**
     * 预计完成时间（秒）.
     *
     * <p>基于历史数据估算的处理时间，帮助客户端决定轮询间隔。
     * 仅在 ACCEPTED 状态时有意义。
     */
    private Integer estimatedCompletionSeconds;

    /**
     * 轮询 URL.
     *
     * <p>客户端可以通过此 URL 查询任务状态和结果。
     * 格式：/api/v1/chat/async/status/{taskId}
     */
    private String pollUrl;

    /**
     * 时间戳.
     *
     * <p>响应生成时间，用于客户端计算等待时间。
     */
    private Instant timestamp;

    /**
     * 创建"已接受"状态的响应.
     *
     * <p>当服务器成功接受异步请求但尚未开始处理时使用。
     * 返回 HTTP 202 (Accepted) 状态码。
     *
     * @param taskId 任务 ID
     * @return 已接受状态的响应对象
     */
    public static AsyncChatResponse accepted(String taskId) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("ACCEPTED")
            .message("请求已接受，正在处理中")
            .pollUrl("/api/v1/chat/async/status/" + taskId)  // 构造轮询 URL
            .estimatedCompletionSeconds(30)  // 默认预估 30 秒
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建"处理中"状态的响应.
     *
     * <p>当任务正在被 AI 处理时使用。
     * 客户端应继续轮询等待完成。
     *
     * @param taskId 任务 ID
     * @return 处理中状态的响应对象
     */
    public static AsyncChatResponse processing(String taskId) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("PROCESSING")
            .message("请求正在处理中")
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建"已完成"状态的响应.
     *
     * <p>当 AI 推理完成时使用，包含最终结果。
     * 返回 HTTP 200 (OK) 状态码。
     *
     * @param taskId 任务 ID
     * @param result AI 响应内容
     * @return 已完成状态的响应对象
     */
    public static AsyncChatResponse completed(String taskId, String result) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("COMPLETED")
            .message(result)  // 包含 AI 的回答
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建"失败"状态的响应.
     *
     * <p>当 AI 推理失败时使用，包含错误信息。
     * 生产环境应返回用户友好的错误消息。
     *
     * @param taskId 任务 ID
     * @param error  错误信息
     * @return 失败状态的响应对象
     */
    public static AsyncChatResponse failed(String taskId, String error) {
        return AsyncChatResponse.builder()
            .taskId(taskId)
            .status("FAILED")
            .message(error)  // 错误描述
            .timestamp(Instant.now())
            .build();
    }
}