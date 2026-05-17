package io.agentscope.tutorial.chapter14;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 第十四章案例：人类在环（HITL）审批工作流 Spring Boot 启动类.
 *
 * <p>本案例演示了如何在 AgentScope Java 中实现完整的人类在环审批流程：
 * <ul>
 *   <li>敏感工具执行前自动暂停，等待人工审批</li>
 *   <li>审批请求持久化到 H2 数据库，支持管理后台查看</li>
 *   <li>审批结果通过 API 回传，代理自动恢复执行</li>
 *   <li>定时任务自动处理审批超时</li>
 * </ul>
 *
 * <h2>启动方式</h2>
 * <pre>
 * # 设置 DashScope API Key
 * export DASHSCOPE_API_KEY=your_api_key
 *
 * # 启动应用
 * mvn spring-boot:run
 * </pre>
 *
 * <h2>API 接口</h2>
 * <pre>
 * POST /api/chat                    # 发送聊天消息（SSE 流式）
 * POST /api/approve                 # 提交审批决策（SSE 流式恢复）
 * GET  /api/approval/pending        # 查看所有待审批请求
 * POST /api/approval/{id}           # 手动审批某个请求
 * DELETE /api/session/{sessionId}  # 清空会话
 * </pre>
 *
 * <h2>H2 控制台</h2>
 * <pre>
 * 访问 http://localhost:8080/h2-console
 * JDBC URL: jdbc:h2:mem:hitl_db
 * </pre>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务（用于处理审批超时）
public class Chapter14Application {

    public static void main(String[] args) {
        // 检查必要的环境变量
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            System.err.println();
            System.err.println("=".repeat(60));
            System.err.println("  错误：请设置 DASHSCOPE_API_KEY 环境变量");
            System.err.println("  示例：export DASHSCOPE_API_KEY=sk-xxxxxxx");
            System.err.println("=".repeat(60));
            System.err.println();
            System.err.println("提示：如果是 Windows 系统，请使用：");
            System.err.println("  set DASHSCOPE_API_KEY=sk-xxxxxxx");
            System.err.println("  mvn spring-boot:run");
            System.err.println();
            System.exit(1);
        }

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  AgentScope Java - Chapter 14: 人类在环（HITL）");
        System.out.println("  审批工作流案例");
        System.out.println("=".repeat(60));
        System.out.println("  服务地址: http://localhost:8080");
        System.out.println("  H2 控制台: http://localhost:8080/h2-console");
        System.out.println("  API 文档: http://localhost:8080/api/approval/pending");
        System.out.println("=".repeat(60));
        System.out.println();

        SpringApplication.run(Chapter14Application.class, args);
    }
}