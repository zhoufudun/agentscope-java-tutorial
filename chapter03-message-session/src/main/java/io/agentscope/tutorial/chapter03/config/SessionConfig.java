package io.agentscope.tutorial.chapter03.config;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 会话配置
 *
 * <p>迁移说明（AgentScope 1.1.0 → 2.0.3）：
 * <ul>
 *   <li>io.agentscope.core.session.* 整包已删除（Session / InMemorySession / JsonSession 均无等价直接类）。</li>
 *   <li>2.x 改用 io.agentscope.core.state.AgentStateStore 家族作为会话状态持久化后端：
 *     <ul>
 *       <li>InMemoryAgentStateStore：纯内存实现，进程退出即丢失，适合开发/测试。</li>
 *       <li>JsonFileAgentStateStore(Path)：JSON 文件实现，跨应用重启可恢复，适合本地部署。</li>
 *     </ul>
 *   </li>
 *   <li>会话不再用 SessionKey / SimpleSessionKey 对象标识，统一改用 String sessionId
 *       （userId 可为 null 表示匿名/单租户）。</li>
 * </ul>
 *
 * <p>本配置将 {@link AgentStateStore} 暴露为 Spring Bean，由 SessionService 注入使用，
 * 作为与 JPA/H2 并行的“AgentScope 原生会话状态”持久化层，用于教学演示。
 */
@Configuration
public class SessionConfig {

    @Value("${agentscope.session.storage-path:./data/agentscope_sessions}")
    private String storagePath;

    @Value("${agentscope.session.max-history-size:50}")
    private int maxHistorySize;

    /**
     * 创建 AgentScope 会话状态存储实例。
     *
     * <p>这里选用文件持久化实现 {@link JsonFileAgentStateStore}：会话状态以 JSON 形式
     * 落盘到 storagePath 目录下（按 userId/sessionId 分目录），跨应用重启依然可恢复。
     * 若只需内存实现，可替换为 {@code new InMemoryAgentStateStore()}。
     *
     * @return AgentStateStore 实例
     */
    @Bean
    public AgentStateStore agentStateStore() {
        Path sessionPath = Paths.get(storagePath);
        return new JsonFileAgentStateStore(sessionPath);
    }

    public String getStoragePath() {
        return storagePath;
    }

    public int getMaxHistorySize() {
        return maxHistorySize;
    }
}
