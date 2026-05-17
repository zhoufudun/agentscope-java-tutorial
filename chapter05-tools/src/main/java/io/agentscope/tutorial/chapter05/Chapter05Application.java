package io.agentscope.tutorial.chapter05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope 第五章教程 - 工具系统
 *
 * <p>本应用演示了 AgentScope Java 的完整工具系统，包括：
 * <ul>
 *   <li>内置工具的使用（文件操作等）</li>
 *   <li>自定义工具开发（计算器、天气查询、日期时间）</li>
 *   <li>工具分组管理</li>
 *   <li>元工具（reset_equipped_tools）</li>
 *   <li>Spring Boot 3 集成</li>
 * </ul>
 *
 * <p>启动后可通过以下端点交互：
 * <ul>
 *   <li>POST /api/agent/chat - 发送消息给 Agent</li>
 *   <li>GET /api/agent/tools - 获取工具列表</li>
 *   <li>POST /api/agent/tools/activate - 激活工具分组</li>
 *   <li>GET /api/agent/tools/demo - 演示工具调用</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootApplication
public class Chapter05Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter05Application.class, args);
    }
}