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

import io.agentscope.core.ReActAgent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求 DTO.
 *
 * <p>用于聊天 API 的请求数据传输对象（Data Transfer Object）。
 * 封装客户端发送的聊天请求参数，支持参数验证和可选配置。
 *
 * <h2>请求示例</h2>
 * <pre>{@code
 * {
 *   "userId": "user-123",
 *   "sessionId": "session-456",
 *   "message": "你好，请介绍一下自己",
 *   "options": {
 *     "maxTokens": 1024,
 *     "temperature": 0.7,
 *     "stream": false
 *   }
 * }
 * }</pre>
 *
 * <h2>参数验证</h2>
 * <p>使用 Jakarta Validation 注解进行参数校验：
 * <ul>
 *   <li>{@code @NotBlank} - 非空且非纯空白字符</li>
 *   <li>{@code @Size} - 限制字符串长度</li>
 * </ul>
 *
 * <h2>生产环境建议</h2>
 * <ul>
 *   <li>限制 message 最大长度，防止恶意大请求</li>
 *   <li>验证 userId 和 sessionId 格式，防止注入攻击</li>
 *   <li>记录请求但不记录敏感的 message 内容</li>
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
public class ChatRequest {

    /**
     * 用户 ID.
     *
     * <p>唯一标识用户，用于：
     * <ul>
     *   <li>多租户数据隔离</li>
     *   <li>用户级别的限流</li>
     *   <li>监控指标标签</li>
     *   <li>审计日志记录</li>
     * </ul>
     *
     * <p>生产环境应从认证 Token 中提取，而非客户端传入。
     */
    @NotBlank(message = "userId 不能为空")  // 验证：非空
    private String userId;

    /**
     * 会话 ID.
     *
     * <p>标识一次对话会话，同一 sessionId 的请求共享对话历史。
     * 用于实现多轮对话的上下文理解。
     *
     * <p>客户端应为每个独立对话生成唯一 sessionId，
     * 通常使用 UUID。
     */
    @NotBlank(message = "sessionId 不能为空")  // 验证：非空
    private String sessionId;

    /**
     * 用户消息.
     *
     * <p>用户发送给 AI 的文本内容。限制最大长度防止滥用。
     *
     * <p><b>安全注意：</b>
     * <ul>
     *   <li>不要将原始消息记录到日志（可能含敏感信息）</li>
     *   <li>考虑内容审核过滤敏感词</li>
     *   <li>限制最大长度防止资源耗尽</li>
     * </ul>
     */
    @NotBlank(message = "message 不能为空")  // 验证：非空
    @Size(max = 10000, message = "message 长度不能超过 10000 字符")  // 验证：长度限制
    private String message;

    /**
     * ReActAgent 实例（由框架自动注入）.
     *
     * <p>通过 Spring 的参数解析器自动注入配置好的 Agent 实例。
     * 客户端无需在 JSON 中传递此字段。
     *
     * <p>注入机制依赖于 AgentScope 的自动配置，
     * 确保 application.yml 中配置了正确的模型参数。
     */
    private ReActAgent agent;

    /**
     * 额外的配置选项.
     *
     * <p>可选的 AI 推理参数，覆盖默认配置。
     * 如果未提供，使用系统默认值。
     */
    private ChatOptions options;

    /**
     * 聊天选项.
     *
     * <p>AI 推理的可选配置参数，支持细粒度控制。
     * 这些参数会传递给底层 LLM API。
     *
     * <h3>参数说明</h3>
     * <ul>
     *   <li><b>maxTokens</b> - 限制响应长度，控制成本</li>
     *   <li><b>temperature</b> - 控制随机性，0=确定性，1=创造性</li>
     *   <li><b>stream</b> - 是否使用流式输出</li>
     * </ul>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatOptions {
        /**
         * 最大 token 数.
         *
         * <p>限制 AI 响应的最大长度。
         * 较小的值可以降低成本，但可能导致回答被截断。
         * 典型值：256-4096。
         */
        private Integer maxTokens;

        /**
         * 温度参数.
         *
         * <p>控制 AI 输出的随机性：
         * <ul>
         *   <li>0.0 - 完全确定性，总是选择最可能的词</li>
         *   <li>0.7 - 平衡，适合大多数场景</li>
         *   <li>1.0 - 高创造性，适合创意写作</li>
         * </ul>
         */
        private Double temperature;

        /**
         * 是否启用流式输出.
         *
         * <p>启用后，AI 的响应会逐字返回，而非等待完整响应。
         * 需配合 SSE 或 WebSocket 使用。
         */
        private Boolean stream;
    }
}