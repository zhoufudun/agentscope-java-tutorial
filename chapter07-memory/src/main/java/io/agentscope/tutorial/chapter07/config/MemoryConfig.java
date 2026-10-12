package io.agentscope.tutorial.chapter07.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 记忆系统配置类
 *
 * <p>为记忆系统提供核心配置，包括会话状态存储和压缩参数。
 *
 * <p><b>配置内容</b>：
 * <ul>
 *   <li>会话状态存储（{@link AgentStateStore}）</li>
 *   <li>记忆压缩阈值和开关</li>
 *   <li>Agent 模板配置</li>
 * </ul>
 *
 * <p><b>2.x 迁移说明</b>：
 * AgentScope 2.x 对记忆系统做了重大重构：
 * <ul>
 *   <li>移除了 {@code ReActAgent.Builder.memory(...)} 方法</li>
 *   <li>移除了 {@code io.agentscope.core.state.StatePersistence} 接口</li>
 *   <li>会话历史现在由 {@code AgentState} 维护</li>
 *   <li>持久化改用 {@code AgentStateStore} 接口：
 *     <ul>
 *       <li>{@link InMemoryAgentStateStore} - 进程内内存存储</li>
 *       <li>{@code JsonFileAgentStateStore} - 基于 JSON 文件的持久化</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>配置示例</b>（application.yml）：
 * <pre>{@code
 * memory:
 *   max-messages: 100
 *   compression-enabled: true
 *   compression-threshold: 0.8
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Configuration
public class MemoryConfig {

    /**
     * 触发压缩前的最大消息数
     */
    @Value("${memory.max-messages:100}")
    private int maxMessages;

    /**
     * 是否启用记忆压缩
     */
    @Value("${memory.compression-enabled:true}")
    private boolean compressionEnabled;

    /**
     * 压缩阈值（最大消息数的百分比，0.0-1.0）
     * <p>当消息数达到 maxMessages * compressionThreshold 时触发压缩
     */
    @Value("${memory.compression-threshold:0.8}")
    private double compressionThreshold;

    /**
     * 计算触发压缩的消息数阈值
     *
     * @return 触发压缩的消息数量
     */
    public int getCompressionThresholdCount() {
        return (int) (maxMessages * compressionThreshold);
    }

    /**
     * 获取最大消息数配置
     *
     * @return 最大消息数
     */
    public int getMaxMessages() {
        return maxMessages;
    }

    /**
     * 检查是否启用压缩
     *
     * @return 是否启用
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * 获取压缩阈值
     *
     * @return 阈值比例
     */
    public double getCompressionThreshold() {
        return compressionThreshold;
    }

    /**
     * 会话状态存储 Bean
     *
     * <p>这是 2.x 记忆持久化的核心组件。本示例使用 {@link InMemoryAgentStateStore}
     * 作为进程内存储，方便演示和测试。
     *
     * <p><b>生产环境建议</b>：
     * 若需要跨进程/重启保留会话历史，可改用基于文件或数据库的实现：
     * <pre>{@code
     * return new JsonFileAgentStateStore(Paths.get(System.getProperty("user.home"),
     *         ".agentscope", "chapter07", "sessions"));
     * }</pre>
     *
     * <p>该 Store 会被 {@code MemoryService} 注入，用于为每个会话的 Agent
     * 提供 {@code AgentState} 的自动 load/save 能力。
     *
     * @return 共享的 AgentStateStore 实例
     */
    @Bean
    public AgentStateStore agentStateStore() {
        return new InMemoryAgentStateStore();
    }

    /**
     * 创建 ReActAgent 模板 Bean
     *
     * <p>作为参考模板演示 2.x 的记忆配置方式：
     * <ul>
     *   <li>通过 {@code .stateStore(store)} 指定状态存储后端</li>
     *   <li>通过 {@code .defaultSessionId(id)} 指定默认会话标识</li>
     * </ul>
     *
     * <p>这样配置后，Agent 会自动：
     * <ol>
     *   <li>构建时按 sessionId 载入历史状态</li>
     *   <li>每次 {@code call()} 后自动保存状态</li>
     * </ol>
     *
     * <p><b>与 1.x 的对比</b>：
     * <pre>{@code
     * // 1.x 写法（已废弃）
     * .memory(new InMemoryMemory())
     * .statePersistence(...)
     *
     * // 2.x 写法
     * .stateStore(stateStore)
     * .defaultSessionId(sessionId)
     * }</pre>
     *
     * @param model 使用的 AI 模型
     * @param stateStore 会话状态存储
     * @return 配置好的 ReActAgent 实例
     */
    @Bean
    public ReActAgent baseAgentTemplate(Model model, AgentStateStore stateStore) {
        String systemPrompt = """
                你是一个具有持久记忆能力的智能助手。

                你的能力：
                - 能够跨对话记住用户的重要偏好和事实
                - 需要时可以搜索记忆中的信息
                - 基于过去的交互提供个性化回复

                请保持友好、乐于助人，并记住用户分享的重要细节。
                """;

        return ReActAgent.builder()
                .name("MemoryAssistant")
                .sysPrompt(systemPrompt)
                .model(model)
                .maxIters(10)
                // 2.x：会话历史由 AgentState 维护，stateStore + defaultSessionId
                // 实现自动 load/save，替代旧的 memory(...) + statePersistence(...)
                .stateStore(stateStore)
                .defaultSessionId("default")
                .build();
    }
}
