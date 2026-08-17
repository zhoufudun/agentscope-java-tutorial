package io.agentscope.tutorial.chapter07.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Memory Configuration
 *
 * <p>Provides configuration for the memory system including:
 * <ul>
 *   <li>会话状态存储（AgentStateStore）</li>
 *   <li>Memory statistics tracking</li>
 *   <li>Compression settings</li>
 * </ul>
 *
 * <p><b>2.x 迁移说明</b>：AgentScope 2.x 移除了 {@code ReActAgent.Builder.memory(...)}
 * 与 {@code io.agentscope.core.state.StatePersistence}。会话历史现在由 {@code AgentState}
 * 维护，持久化改用 {@code AgentStateStore}：
 * <ul>
 *   <li>{@link io.agentscope.core.state.InMemoryAgentStateStore} —— 进程内内存存储；</li>
 *   <li>{@link io.agentscope.core.state.JsonFileAgentStateStore} —— 基于 JSON 文件的持久化。</li>
 * </ul>
 * 在 Builder 上使用 {@code .stateStore(store).defaultSessionId(id)}，Agent 会在构建时
 * 自动按 sessionId 载入历史，并在每次 {@code call()} 后自动保存。
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class MemoryConfig {

    /**
     * Maximum number of messages to keep in memory before triggering compression.
     */
    @Value("${memory.max-messages:100}")
    private int maxMessages;

    /**
     * Whether memory compression is enabled.
     */
    @Value("${memory.compression-enabled:true}")
    private boolean compressionEnabled;

    /**
     * Compression threshold as a percentage of max messages (0.0 - 1.0).
     */
    @Value("${memory.compression-threshold:0.8}")
    private double compressionThreshold;

    /**
     * Maximum message count before compression is triggered.
     */
    public int getCompressionThresholdCount() {
        return (int) (maxMessages * compressionThreshold);
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public double getCompressionThreshold() {
        return compressionThreshold;
    }

    /**
     * 会话状态存储（2.x 记忆持久化后端）。
     *
     * <p>此处使用 {@link InMemoryAgentStateStore} 作为进程内存储，方便演示与测试。
     * 若需要跨进程/重启保留会话历史，可改为：
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
     * Creates a ReActAgent template for session-based conversations.
     *
     * <p>作为参考模板演示 2.x 的记忆写法：通过 {@code .stateStore(store)} 与
     * {@code .defaultSessionId(id)} 让 Agent 自动维护/持久化会话历史（AgentState），
     * 取代 1.x 的 {@code .memory(new InMemoryMemory())} + {@code .statePersistence(...)}。
     *
     * @param model      The AI model to use
     * @param stateStore 会话状态存储
     * @return A configured ReActAgent using AgentState-backed memory
     */
    @Bean
    public ReActAgent baseAgentTemplate(Model model, AgentStateStore stateStore) {
        String systemPrompt = """
                You are an intelligent assistant with persistent memory capabilities.
                You can:
                - Remember important user preferences and facts across conversations
                - Search through your memory when needed
                - Provide personalized responses based on past interactions

                Always be helpful, friendly, and remember important details shared by the user.
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
