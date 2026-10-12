/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter03.config;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 会话配置类 - 配置 AgentScope 会话状态存储.
 *
 * <p>本配置类负责创建和管理 AgentScope 的会话状态存储实例，是理解 AgentScope 2.x
 * 会话管理机制的关键入口。
 *
 * <h2>核心概念</h2>
 * <p>{@link AgentStateStore} 是 AgentScope 2.x 中用于持久化会话状态的核心接口，
 * 它采用 (userId, sessionId, key) 三元组来定位和存储会话数据。
 *
 * <h2>版本迁移说明（AgentScope 1.1.0 → 2.0.3）</h2>
 * <ul>
 *   <li>{@code io.agentscope.core.session.*} 整包已删除（Session / InMemorySession / JsonSession 均无等价直接类）</li>
 *   <li>2.x 改用 {@link AgentStateStore} 家族作为会话状态持久化后端：
 *     <ul>
 *       <li>{@code InMemoryAgentStateStore}：纯内存实现，进程退出即丢失，适合开发/测试</li>
 *       <li>{@code JsonFileAgentStateStore(Path)}：JSON 文件实现，跨应用重启可恢复，适合本地部署</li>
 *     </ul>
 *   </li>
 *   <li>会话定位方式从 {@code SessionKey / SimpleSessionKey} 对象改为 {@code (userId, sessionId)} 字符串对</li>
 *   <li>{@code userId} 可为 null，表示匿名或单租户场景</li>
 * </ul>
 *
 * <h2>配置项说明</h2>
 * <ul>
 *   <li>{@code agentscope.session.storage-path}：会话状态文件存储路径，默认 {@code ./data/agentscope_sessions}</li>
 *   <li>{@code agentscope.session.max-history-size}：单个会话最大历史消息数，默认 50</li>
 * </ul>
 *
 * <h2>架构说明</h2>
 * <p>本配置将 {@link AgentStateStore} 暴露为 Spring Bean，由 SessionService 注入使用，
 * 作为与 JPA/H2 并行的”AgentScope 原生会话状态”持久化层，用于教学演示两种不同的持久化方式：
 * <ul>
 *   <li>JPA/H2：存储会话元数据和消息明细（关系型数据库方式）</li>
 *   <li>AgentStateStore：存储 AgentScope 原生的会话上下文（框架原生方式）</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see AgentStateStore
 * @see JsonFileAgentStateStore
 * @see io.agentscope.core.state.InMemoryAgentStateStore
 */
@Configuration
public class SessionConfig {

    /**
     * 会话状态文件存储路径.
     *
     * <p>默认值：{@code ./data/agentscope_sessions}
     */
    @Value("${agentscope.session.storage-path:./data/agentscope_sessions}")
    private String storagePath;

    /**
     * 单个会话最大历史消息数.
     *
     * <p>默认值：50。超过此数量时，旧消息会被修剪以控制上下文长度。
     */
    @Value("${agentscope.session.max-history-size:50}")
    private int maxHistorySize;

    /**
     * 创建 AgentScope 会话状态存储实例.
     *
     * <p>这里选用文件持久化实现 {@link JsonFileAgentStateStore}：
     * <ul>
     *   <li>会话状态以 JSON 形式落盘到 storagePath 目录下</li>
     *   <li>按 userId/sessionId 分目录存储</li>
     *   <li>跨应用重启依然可恢复</li>
     * </ul>
     *
     * <p>如果只需内存实现（适合开发测试），可替换为：
     * <pre>{@code
     * return new InMemoryAgentStateStore();
     * }</pre>
     *
     * @return AgentStateStore 实例，用于会话状态的持久化存储
     */
    @Bean
    public AgentStateStore agentStateStore() {
        // 将配置的存储路径转换为 Path 对象
        Path sessionPath = Paths.get(storagePath);
        // 创建基于 JSON 文件的会话状态存储
        return new JsonFileAgentStateStore(sessionPath);
    }

    /**
     * 获取会话状态存储路径.
     *
     * @return 存储路径字符串
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 获取最大历史消息数配置.
     *
     * @return 最大历史消息数
     */
    public int getMaxHistorySize() {
        return maxHistorySize;
    }
}
