package io.agentscope.tutorial.chapter07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope 第七章教程 - 记忆系统
 *
 * <p>本应用演示完整的记忆系统实现，帮助学习者理解如何为 AI Agent 构建持久化记忆能力。
 *
 * <p><b>核心功能</b>：
 * <ul>
 *   <li><b>持久化记忆</b> - 对话历史存储在 H2/MySQL 数据库中，支持跨会话访问</li>
 *   <li><b>多会话隔离</b> - 每个会话/用户拥有独立的记忆空间</li>
 *   <li><b>记忆压缩</b> - 长对话自动摘要，节省上下文空间</li>
 *   <li><b>记忆搜索</b> - 全文检索历史对话内容</li>
 *   <li><b>记忆工具</b> - Agent 可通过工具主动管理记忆</li>
 * </ul>
 *
 * <p><b>2.x 迁移说明</b>：
 * AgentScope 2.x 移除了旧的 {@code Memory} 和 {@code StatePersistence} 接口，
 * 会话历史改由 {@code AgentState} 维护，持久化通过 {@code AgentStateStore} 实现。
 * 详见 {@link io.agentscope.tutorial.chapter07.config.MemoryConfig} 中的配置。
 *
 * <p><b>访问方式</b>：
 * <ul>
 *   <li>记忆管理 API: http://localhost:8080/api/memory</li>
 *   <li>聊天 API: http://localhost:8080/api/chat</li>
 *   <li>H2 数据库控制台: http://localhost:8080/h2-console（仅 dev 配置）</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 发送消息（自动保存到记忆）
 * curl -X POST "http://localhost:8080/api/chat/send?sessionId=user001" \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "你好，我叫张三"}'
 *
 * // 搜索记忆
 * curl "http://localhost:8080/api/memory/search?sessionId=user001&keyword=张三"
 *
 * // 获取记忆统计
 * curl "http://localhost:8080/api/memory/stats?sessionId=user001"
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootApplication
public class Chapter07Application {

    /**
     * 应用程序入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Chapter07Application.class, args);
    }
}