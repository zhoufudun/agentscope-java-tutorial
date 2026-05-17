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
package io.agentscope.tutorial.chapter13.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.recorder.JsonlTraceExporter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.spring.boot.properties.AgentscopeProperties;
import io.agentscope.tutorial.chapter13.hook.AuditHook;
import io.agentscope.tutorial.chapter13.hook.LoggingHook;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.hook.TimingHook;
import io.agentscope.tutorial.chapter13.repository.AuditLogRepository;
import io.agentscope.tutorial.chapter13.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Agent 配置类 - 集中管理 Agent 实例的创建和钩子配置.
 *
 * <p>通过 Spring 的依赖注入机制，确保：
 * <ul>
 *   <li>所有钩子只创建一次（单例）</li>
 *   <li>Agent 及其钩子链在应用启动时完成初始化</li>
 *   <li>便于测试时替换模拟实现</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Value("${agentscope.dashscope.model-name:qwen-plus}")
    private String modelName;

    @Value("${agentscope.dashscope.stream:true}")
    private boolean stream;

    @Value("${agentscope.dashscope.enable-thinking:false}")
    private boolean enableThinking;

    @Value("${agentscope.agent.name:TutorialAgent}")
    private String agentName;

    @Value("${agentscope.agent.sys-prompt}")
    private String sysPrompt;

    @Value("${agentscope.agent.max-iters:10}")
    private int maxIters;

    @Value("${tutorial.hook.logging.log-streaming-chunks:false}")
    private boolean logStreamingChunks;

    @Value("${tutorial.hook.metrics.detailed:true}")
    private boolean metricsDetailed;

    @Value("${tutorial.hook.audit.strict-mode:false}")
    private boolean auditStrictMode;

    private final HookConfig hookConfig;

    private final AuditLogRepository auditLogRepository;

    public AgentConfig(HookConfig hookConfig, AuditLogRepository auditLogRepository) {
        this.hookConfig = hookConfig;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 注册 AgentScope 配置属性类.
     */
    @Bean
    @ConfigurationPropertiesBinding
    public AgentscopeProperties agentscopeProperties() {
        return new AgentscopeProperties();
    }

    // ========== Hook Bean 定义 ==========

    /**
     * 配置日志钩子 - 记录所有关键生命周期事件.
     *
     * <p>优先级 900：低优先级，确保在业务逻辑之后执行
     */
    @Bean
    public LoggingHook loggingHook() {
        return new LoggingHook(logStreamingChunks);
    }

    /**
     * 配置监控钩子 - 收集性能指标.
     *
     * <p>优先级 950：最低优先级，专注于指标收集
     */
    @Bean
    public MetricsHook metricsHook() {
        return new MetricsHook(metricsDetailed);
    }

    /**
     * 配置审计钩子 - 记录工具调用的详细信息.
     *
     * <p>优先级 60：高优先级，尽早拦截工具调用
     */
    @Bean
    public AuditHook auditHook() {
        Set<String> sensitiveTools = Set.copyOf(hookConfig.getAudit().getSensitiveTools());
        return new AuditHook(sensitiveTools, auditStrictMode, Set.of(), auditLogRepository);
    }

    /**
     * 配置性能钩子 - 测量各阶段耗时.
     *
     * <p>优先级 100：普通优先级
     */
    @Bean
    public TimingHook timingHook() {
        return new TimingHook(true);
    }

    /**
     * 配置 JSONL 追踪导出器 - 将所有事件写入文件.
     *
     * <p>优先级 900：与日志钩子同级别
     */
    @Bean
    public JsonlTraceExporter traceExporter() {
        return JsonlTraceExporter.builder(Path.of("logs", "agent-trace.jsonl"))
                .includeReasoningChunks(true)
                .includeActingChunks(true)
                .includeSummary(true)
                .priority(900)
                .build();
    }

    /**
     * 配置钩子列表 - 按优先级排序.
     *
     * <p>执行顺序：
     * <ol>
     *   <li>AuditHook(60)      - 工具审计（最早执行）</li>
     *   <li>TimingHook(100)    - 性能测量</li>
     *   <li>LoggingHook(900)   - 日志记录</li>
     *   <li>MetricsHook(950)   - 指标收集（最后执行）</li>
     *   <li>TraceExporter(900) - 追踪导出</li>
     * </ol>
     */
    @Bean
    public List<Hook> hooks(LoggingHook loggingHook,
                           MetricsHook metricsHook,
                           AuditHook auditHook,
                           TimingHook timingHook,
                           JsonlTraceExporter traceExporter) {
        return List.of(
                auditHook,       // 优先级 60
                timingHook,      // 优先级 100
                loggingHook,     // 优先级 900
                traceExporter,   // 优先级 900
                metricsHook      // 优先级 950
        );
    }

    /**
     * 创建示例工具集.
     */
    @Bean
    public Toolkit agentToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ChatService.DemoTools());
        return toolkit;
    }

    /**
     * 创建 ReActAgent 实例.
     *
     * <p>配置说明：
     * <ul>
     *   <li>name: Agent 标识名称</li>
     *   <li>sysPrompt: 系统提示词，定义 Agent 行为</li>
     *   <li>model: 使用 DashScope 阿里云模型</li>
     *   <li>toolkit: 注册的工具集合</li>
     *   <li>memory: 内存管理（InMemoryMemory 仅用于演示，生产环境建议使用持久化方案）</li>
     *   <li>hooks: 钩子链配置</li>
     * </ul>
     */
    @Bean
    public ReActAgent tutorialAgent(List<Hook> hooks, Toolkit agentToolkit) {
        log.info("创建 TutorialAgent 实例...");
        log.info("  模型: {}", modelName);
        log.info("  钩子数量: {}", hooks.size());

        return ReActAgent.builder()
                .name(agentName)
                .sysPrompt(sysPrompt)
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .stream(stream)
                        .enableThinking(enableThinking)
                        .formatter(new DashScopeChatFormatter())
                        .build())
                .toolkit(agentToolkit)
                .memory(new InMemoryMemory())
                .hooks(hooks)
                .build();
    }
}