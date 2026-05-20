package io.agentscope.tutorial.chapter07.config;

import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.InMemoryMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import io.agentscope.core.model.Model;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.StatePersistence;

/**
 * Memory Configuration
 *
 * <p>Provides configuration for the memory system including:
 * <ul>
 *   <li>Memory implementation beans</li>
 *   <li>Memory statistics tracking</li>
 *   <li>Compression settings</li>
 * </ul>
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
     * Creates a default InMemoryMemory for the base agent template.
     *
     * <p>This is used as a template - actual session memories are created
     * by the MemoryService for proper multi-session isolation.
     *
     * @return A new InMemoryMemory instance
     */
    @Bean
    public Memory defaultMemory() {
        return new InMemoryMemory();
    }

    /**
     * Creates a ReActAgent template for session-based conversations.
     *
     * <p>This template agent is used by MemoryService to create session-specific
     * agents with database-backed memory.
     *
     * @param model The AI model to use
     * @return A configured ReActAgent builder
     */
    @Bean
    public ReActAgent baseAgentTemplate(Model model) {
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
                .memory(new InMemoryMemory())
                .maxIters(10)
                .statePersistence(StatePersistence.builder()
                        .memoryManaged(true)
                        .build())
                .build();
    }
}