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

package io.agentscope.tutorial.chapter09.server;

import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.transport.TransportWrapper;
import io.agentscope.core.a2a.server.transport.jsonrpc.JsonRpcTransportWrapper;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * A2A 服务器组件，通过 A2A 协议暴露 Agent
 *
 * <p>本组件负责管理 A2A 服务器的生命周期，包括初始化、
 * 提供传输包装器访问和 AgentCard 服务发现。
 *
 * <h2>核心职责</h2>
 * <ul>
 *   <li>从 Spring Boot 自动配置初始化 AgentScopeA2aServer</li>
 *   <li>提供传输包装器访问用于请求处理</li>
 *   <li>暴露 AgentCard 用于服务发现</li>
 * </ul>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>AgentScopeA2aServer</b>：A2A 服务器的核心类，
 *       当 Spring 上下文中存在 ReActAgent 或其 Builder 时自动创建</li>
 *   <li><b>TransportWrapper</b>：封装传输协议的处理逻辑</li>
 *   <li><b>JsonRpcTransportWrapper</b>：JSON-RPC 协议的具体实现</li>
 *   <li><b>AgentCard</b>：包含 Agent 所有元数据的配置对象</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Component
public class AgentServer {

    private static final Logger log = LoggerFactory.getLogger(AgentServer.class);

    /** JSON-RPC 传输协议标识 */
    private static final String JSONRPC_TRANSPORT = TransportProtocol.JSONRPC.asString();

    /** A2A 服务器实例，由自动配置创建 */
    private final AgentScopeA2aServer a2aServer;

    /** JSON-RPC 传输包装器 */
    private JsonRpcTransportWrapper jsonRpcTransportWrapper;

    /**
     * 构造函数注入 A2A 服务器
     *
     * <p>当 Spring 上下文中存在 ReActAgent 或其 Builder 时，
     * agentscope-a2a-spring-boot-starter 会自动创建 AgentScopeA2aServer Bean。
     *
     * @param a2aServer 自动配置的 A2A 服务器
     */
    @Autowired
    public AgentServer(AgentScopeA2aServer a2aServer) {
        this.a2aServer = a2aServer;
    }

    /**
     * 在上下文刷新时初始化传输包装器
     *
     * <p>使用 Spring 事件监听器在应用启动完成后执行初始化。
     * 这确保所有依赖 Bean 都已就绪。
     */
    @org.springframework.context.event.EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            // 获取 JSON-RPC 传输包装器
            // 传输包装器负责处理特定协议的请求和响应
            this.jsonRpcTransportWrapper = a2aServer.getTransportWrapper(
                    JSONRPC_TRANSPORT, JsonRpcTransportWrapper.class);

            // 获取并记录 Agent Card 信息
            AgentCard agentCard = a2aServer.getAgentCard();
            log.info("=== A2A 服务器已初始化 ===");
            log.info("Agent 名称：{}", agentCard.name());
            log.info("Agent URL：{}", agentCard.url());
            log.info("Agent 版本：{}", agentCard.version());
            log.info("传输协议：{}", JSONRPC_TRANSPORT);
            log.info("技能数量：{}", agentCard.skills().size());
            log.info("================================");

        } catch (Exception e) {
            log.error("初始化 A2A 服务器传输包装器失败", e);
        }
    }

    /**
     * 获取 A2A 服务器实例
     *
     * @return AgentScopeA2aServer 实例
     */
    public AgentScopeA2aServer getA2aServer() {
        return a2aServer;
    }

    /**
     * 获取 JSON-RPC 传输包装器
     *
     * <p>此包装器处理 JSON-RPC 请求，可被 A2aJsonRpcController 使用。
     *
     * @return JsonRpcTransportWrapper，如果未初始化则返回 null
     */
    public JsonRpcTransportWrapper getJsonRpcTransportWrapper() {
        return jsonRpcTransportWrapper;
    }

    /**
     * 获取 AgentCard
     *
     * <p>AgentCard 包含此 Agent 的所有元数据，
     * 包括其能力和端点信息。
     *
     * @return AgentCard 实例
     */
    public AgentCard getAgentCard() {
        return a2aServer.getAgentCard();
    }

    /**
     * 按类型获取传输包装器
     *
     * <p>如果配置了其他传输协议，可使用此方法访问。
     *
     * @param transportType 传输类型（如 "JSONRPC"）
     * @return TransportWrapper 实例
     * @throws IllegalArgumentException 如果找不到指定的传输类型
     */
    public TransportWrapper<?, ?> getTransportWrapper(String transportType) {
        return a2aServer.getTransportWrapper(transportType);
    }
}