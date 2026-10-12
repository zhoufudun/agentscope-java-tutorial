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
 * 聊天响应 DTO.
 *
 * <p>用于聊天 API 的响应数据传输对象（Data Transfer Object）。
 * 封装 AI 响应或错误信息，提供统一的响应格式。
 *
 * <h2>响应示例（成功）</h2>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "你好！我是 AI 助手，很高兴认识你。",
 *   "sessionId": "session-456",
 *   "durationMs": 1523,
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * }</pre>
 *
 * <h2>响应示例（失败）</h2>
 * <pre>{@code
 * {
 *   "success": false,
 *   "error": "服务暂时不可用，请稍后重试",
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * }</pre>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li><b>统一格式</b> - 成功和失败使用相同的响应结构</li>
 *   <li><b>明确状态</b> - success 字段明确指示请求是否成功</li>
 *   <li><b>可追溯</b> - 包含时间戳和会话 ID 便于问题排查</li>
 *   <li><b>性能信息</b> - 包含响应时间便于监控</li>
 * </ul>
 *
 * @author AgentScope Team
 * @version 1.0.0
 * @see ChatController#chat(ChatRequest)
 */
@Data  // Lombok: getter/setter/toString/equals/hashCode
@Builder  // Lombok: Builder 模式
@NoArgsConstructor  // Lombok: 无参构造器（JSON 反序列化需要）
@AllArgsConstructor  // Lombok: 全参构造器（Builder 需要）
@JsonInclude(JsonInclude.Include.NON_NULL)  // Jackson: 序列化时忽略 null 值字段
public class ChatResponse {

    /**
     * 是否成功.
     *
     * <p>明确指示请求是否成功处理：
     * <ul>
     *   <li>true - AI 成功生成响应，查看 message 字段</li>
     *   <li>false - 处理失败，查看 error 字段</li>
     * </ul>
     */
    private boolean success;

    /**
     * 响应消息.
     *
     * <p>当 success=true 时，包含 AI 生成的响应文本。
     * 当 success=false 时，此字段为 null。
     */
    private String message;

    /**
     * 会话 ID.
     *
     * <p>关联的会话标识符，用于多轮对话追踪。
     * 客户端应在后续请求中使用相同的 sessionId。
     */
    private String sessionId;

    /**
     * 处理时间（毫秒）.
     *
     * <p>从收到请求到生成响应的总耗时。
     * 用于客户端监控和性能分析。
     *
     * <p>典型值：
     * <ul>
     *   <li>简单问答：500-2000ms</li>
     *   <li>复杂推理：3000-10000ms</li>
     * </ul>
     */
    private Long durationMs;

    /**
     * 时间戳.
     *
     * <p>响应生成的 UTC 时间，用于客户端记录和排序。
     * Jackson 会将其序列化为 ISO 8601 格式。
     */
    private Instant timestamp;

    /**
     * 错误信息（如果有）.
     *
     * <p>当 success=false 时，包含用户友好的错误描述。
     * 生产环境不应暴露内部实现细节或堆栈信息。
     */
    private String error;

    /**
     * 创建成功响应（简单版本）.
     *
     * <p>仅包含响应消息的最简形式。
     *
     * @param message AI 响应内容
     * @return 成功响应对象
     */
    public static ChatResponse success(String message) {
        return ChatResponse.builder()
            .success(true)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建成功响应（完整版本）.
     *
     * <p>包含会话 ID 和处理时间的完整响应。
     *
     * @param message    AI 响应内容
     * @param sessionId  会话 ID
     * @param durationMs 处理耗时（毫秒）
     * @return 成功响应对象
     */
    public static ChatResponse success(String message, String sessionId, long durationMs) {
        return ChatResponse.builder()
            .success(true)
            .message(message)
            .sessionId(sessionId)
            .durationMs(durationMs)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * 创建错误响应.
     *
     * <p>用于返回处理失败的情况。
     *
     * <p><b>安全注意：</b>errorMessage 应该是用户友好的描述，
     * 不要暴露内部异常信息或堆栈跟踪。
     *
     * @param errorMessage 用户友好的错误描述
     * @return 错误响应对象
     */
    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
            .success(false)
            .error(errorMessage)
            .timestamp(Instant.now())
            .build();
    }
}