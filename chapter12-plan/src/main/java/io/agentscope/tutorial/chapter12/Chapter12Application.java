package io.agentscope.tutorial.chapter12;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 第十二章：计划与任务分解 - Spring Boot 启动类.
 *
 * <p>本应用展示如何使用 AgentScope Java 实现自动化任务规划系统。
 *
 * <p>主要功能：
 * <ul>
 *   <li>与 Agent 进行多轮对话，支持复杂任务的计划创建与执行</li>
 *   <li>实时监控计划状态变化，通过 SSE 推送给前端</li>
 *   <li>使用 H2 数据库持久化存储计划和历史记录</li>
 *   <li>提供 REST API 管理计划（创建、修订、完成）</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter12Application {

    private static final Logger log = LoggerFactory.getLogger(Chapter12Application.class);

    public static void main(String[] args) {
        printBanner();
        SpringApplication.run(Chapter12Application.class, args);
        printUsage();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║  AgentScope Java - 第十二章：计划与任务分解                    ║");
        System.out.println("║  Chapter 12: Plan and Task Decomposition                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("┌──────────────────────────────────────────────────────────────────┐");
        System.out.println("│                      服务已启动成功                                │");
        System.out.println("└──────────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  访问地址:");
        System.out.println("    • 首页:       http://localhost:8080");
        System.out.println("    • H2 Console: http://localhost:8080/h2-console");
        System.out.println();
        System.out.println("  API 接口:");
        System.out.println("    • POST /api/chat           - 与 Agent 对话 (SSE 流式)");
        System.out.println("    • POST /api/chat/resume    - 恢复暂停中的 Agent");
        System.out.println("    • POST /api/chat/stop      - 请求停止 Agent");
        System.out.println("    • GET  /api/plan           - 获取当前计划");
        System.out.println("    • GET  /api/plan/stream    - SSE 流式推送计划状态");
        System.out.println("    • POST /api/plan           - 创建新计划");
        System.out.println("    • PUT  /api/plan           - 更新计划信息");
        System.out.println("    • DELETE /api/plan/subtasks/{idx} - 删除子任务");
        System.out.println("    • PATCH /api/plan/subtasks/{idx}/state - 更新子任务状态");
        System.out.println("    • POST /api/plan/subtasks/{idx}/finish - 完成子任务");
        System.out.println("    • POST /api/plan/finish    - 完成计划");
        System.out.println("    • GET  /api/plan/history   - 获取历史计划列表");
        System.out.println("    • GET  /api/plan/stats     - 获取计划统计");
        System.out.println();
        System.out.println("  环境变量:");
        System.out.println("    • DASHSCOPE_API_KEY - 阿里云百炼 API Key (可选)");
        System.out.println();
    }
}