package io.agentscope.tutorial.chapter03.config;

import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 会话配置
 *
 * 配置 AgentScope Session 的存储方式：
 * - 开发环境：使用内存会话（InMemorySession）
 * - 生产环境：使用 JSON 文件会话（JsonSession）
 */
@Configuration
public class SessionConfig {

    @Value("${agentscope.session.storage-path:./data/agentscope_sessions}")
    private String storagePath;

    @Value("${agentscope.session.max-history-size:50}")
    private int maxHistorySize;

    /**
     * 创建 AgentScope Session 实例
     *
     * @return Session 实例
     */
    @Bean
    public Session agentscopeSession() {
        Path sessionPath = Paths.get(storagePath);
        return new JsonSession(sessionPath);
    }

    public String getStoragePath() {
        return storagePath;
    }

    public int getMaxHistorySize() {
        return maxHistorySize;
    }
}