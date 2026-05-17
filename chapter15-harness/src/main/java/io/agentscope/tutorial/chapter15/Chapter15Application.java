package io.agentscope.tutorial.chapter15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Chapter 15: Harness Security Execution Environment Application.
 *
 * <p>This application demonstrates the Harness security execution environment:
 * <ul>
 *   <li>Sandbox lifecycle management (create, start, stop, shutdown)</li>
 *   <li>Code execution with timeout control</li>
 *   <li>Per-workspace filesystem isolation</li>
 *   <li>Workspace management operations (read, write, edit, delete, move)</li>
 *   <li>Security validation before execution</li>
 * </ul>
 *
 * <p>REST API endpoints:
 * <ul>
 *   <li>POST /api/execute - 在沙箱中执行代码</li>
 *   <li>POST /api/execute/command - Execute shell command</li>
 *   <li>GET /api/execute/sandbox/{workspace} - Get sandbox info</li>
 *   <li>DELETE /api/execute/sandbox/{workspace} - Destroy sandbox</li>
 *   <li>GET /api/workspace/{id}/ls - List directory</li>
 *   <li>GET /api/workspace/{id}/read - Read file</li>
 *   <li>POST /api/workspace/{id}/write - Write file</li>
 *   <li>PUT /api/workspace/{id}/edit - Edit file</li>
 *   <li>DELETE /api/workspace/{id}/file - Delete file</li>
 *   <li>GET /api/workspace/{id}/grep - Search pattern</li>
 *   <li>GET /api/workspace/{id}/glob - Find files</li>
 *   <li>POST /api/workspace/{id}/upload - Upload files</li>
 *   <li>POST /api/workspace/{id}/download - Download files</li>
 *   <li>GET /api/workspace/{id}/stats - Workspace statistics</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Execute Java code
 * curl -X POST http://localhost:8080/api/execute \
 *   -H "Content-Type: application/json" \
 *   -d '{"code":"public static void main(String[] args){System.out.println(\"Hello!\");}","language":"java","workspace":"test"}'
 *
 * // Execute shell command
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
 * // List directory
 * curl "http://localhost:8080/api/workspace/test/ls"
 * }</pre>
 */
@SpringBootApplication
@EnableConfigurationProperties
public class Chapter15Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter15Application.class, args);
    }
}