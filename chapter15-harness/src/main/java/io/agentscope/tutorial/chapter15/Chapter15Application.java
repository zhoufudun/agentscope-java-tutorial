package io.agentscope.tutorial.chapter15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 第15章：Harness 安全执行环境应用程序.
 *
 * <h2>概述</h2>
 * <p>本章演示 Harness 安全执行环境的核心概念和实现方式。Harness 是 AgentScope 框架中
 * 用于安全执行代码的沙箱组件，它提供了隔离的执行环境，防止恶意代码对宿主系统造成损害。
 *
 * <h2>核心概念</h2>
 * <ul>
 *   <li><b>沙箱(Sandbox)</b>：隔离的代码执行环境，限制对系统资源的访问</li>
 *   <li><b>工作空间(Workspace)</b>：每个会话/用户的独立文件系统区域</li>
 *   <li><b>超时控制</b>：防止无限循环或长时间运行的代码</li>
 *   <li><b>安全验证</b>：执行前检查代码是否包含危险模式</li>
 * </ul>
 *
 * <h2>本章功能演示</h2>
 * <ul>
 *   <li>沙箱生命周期管理（创建、启动、停止、销毁）</li>
 *   <li>带超时控制的代码执行</li>
 *   <li>每个工作空间的文件系统隔离</li>
 *   <li>工作空间管理操作（读取、写入、编辑、删除、移动）</li>
 *   <li>执行前的安全验证</li>
 * </ul>
 *
 * <h2>REST API 端点</h2>
 * <ul>
 *   <li>POST /api/execute - 在沙箱中执行代码</li>
 *   <li>POST /api/execute/command - 执行 shell 命令</li>
 *   <li>GET /api/execute/sandbox/{workspace} - 获取沙箱信息</li>
 *   <li>DELETE /api/execute/sandbox/{workspace} - 销毁沙箱</li>
 *   <li>GET /api/workspace/{id}/ls - 列出目录内容</li>
 *   <li>GET /api/workspace/{id}/read - 读取文件</li>
 *   <li>POST /api/workspace/{id}/write - 写入文件</li>
 *   <li>PUT /api/workspace/{id}/edit - 编辑文件</li>
 *   <li>DELETE /api/workspace/{id}/file - 删除文件</li>
 *   <li>GET /api/workspace/{id}/grep - 搜索模式</li>
 *   <li>GET /api/workspace/{id}/glob - 查找文件</li>
 *   <li>POST /api/workspace/{id}/upload - 上传文件</li>
 *   <li>POST /api/workspace/{id}/download - 下载文件</li>
 *   <li>GET /api/workspace/{id}/stats - 工作空间统计</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 执行 Java 代码
 * curl -X POST http://localhost:8080/api/execute \
 *   -H "Content-Type: application/json" \
 *   -d '{"code":"public static void main(String[] args){System.out.println(\"Hello!\");}","language":"java","workspace":"test"}'
 *
 * // 执行 shell 命令
 * curl -X POST http://localhost:8080/api/execute/command \
 *   -H "Content-Type: application/json" \
 *   -d '{"command":"echo Hello","workspace":"test"}'
 *
 * // 写入文件
 * curl -X POST http://localhost:8080/api/workspace/test/write \
 *   -H "Content-Type: application/json" \
 *   -d '{"filePath":"/workspace/test.java","content":"public class Test{}"}'
 *
 * // 读取文件
 * curl "http://localhost:8080/api/workspace/test/read?filePath=/workspace/test.java"
 *
 * // 列出目录
 * curl "http://localhost:8080/api/workspace/test/ls"
 * }</pre>
 *
 * <h2>架构说明</h2>
 * <p>本示例使用内存沙箱（InMemorySandbox）进行演示。在生产环境中，
 * 建议使用基于 Docker 的沙箱（DockerSandbox）或云端沙箱（E2bSandbox、DaytonaSandbox）
 * 以获得更强的隔离性。
 *
 * @author AgentScope Tutorial
 * @see io.agentscope.tutorial.chapter15.sandbox.InMemorySandbox
 * @see io.agentscope.tutorial.chapter15.service.CodeExecutionService
 * @see io.agentscope.tutorial.chapter15.service.WorkspaceService
 */
@SpringBootApplication  // Spring Boot 应用程序入口注解，自动配置和组件扫描
@EnableConfigurationProperties  // 启用配置属性绑定，用于加载 HarnessConfig
public class Chapter15Application {

    /**
     * 应用程序入口点.
     *
     * <p>启动 Spring Boot 应用程序，初始化所有组件：
     * <ul>
     *   <li>HarnessConfig - 沙箱和执行配置</li>
     *   <li>CodeExecutionService - 代码执行服务</li>
     *   <li>WorkspaceService - 工作空间管理服务</li>
     *   <li>REST 控制器 - API 端点</li>
     * </ul>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Chapter15Application.class, args);
    }
}