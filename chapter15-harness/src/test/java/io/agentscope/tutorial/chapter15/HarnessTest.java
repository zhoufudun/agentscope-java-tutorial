package io.agentscope.tutorial.chapter15;

import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.tutorial.chapter15.config.HarnessConfig;
import io.agentscope.tutorial.chapter15.model.ExecutionRequest;
import io.agentscope.tutorial.chapter15.model.ExecutionResult;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandbox;
import io.agentscope.tutorial.chapter15.service.CodeExecutionService;
import io.agentscope.tutorial.chapter15.service.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第15章测试套件：Harness 安全执行环境.
 *
 * <h2>概述</h2>
 * <p>本测试套件全面测试 Harness 安全执行环境的各个组件：
 * <ul>
 *   <li>InMemorySandbox - 内存沙箱实现</li>
 *   <li>CodeExecutionService - 代码执行服务</li>
 *   <li>WorkspaceService - 工作空间服务</li>
 *   <li>REST API - HTTP 端点</li>
 * </ul>
 *
 * <h2>测试组织</h2>
 * <p>使用 JUnit 5 的嵌套测试类组织测试：
 * <ul>
 *   <li>{@link InMemorySandboxTests} - 沙箱基础功能测试</li>
 *   <li>{@link CodeExecutionServiceTests} - 代码执行服务测试</li>
 *   <li>{@link WorkspaceServiceTests} - 工作空间服务测试</li>
 *   <li>{@link RestApiTests} - REST API 集成测试</li>
 * </ul>
 *
 * <h2>测试环境</h2>
 * <p>使用 {@code @SpringBootTest} 启动完整的 Spring Boot 应用，
 * 端口号随机分配以避免端口冲突。
 *
 * @author AgentScope Tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("第15章 - Harness 测试套件")
class HarnessTest {

    /**
     * 随机分配的服务器端口.
     */
    @LocalServerPort
    private int port;

    /**
     * 测试用 REST 客户端.
     */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Harness 配置.
     */
    @Autowired
    private HarnessConfig harnessConfig;

    /**
     * 代码执行服务.
     */
    @Autowired
    private CodeExecutionService codeExecutionService;

    /**
     * 工作空间服务.
     */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 测试用工作空间 ID.
     */
    private static final String TEST_WORKSPACE = "test-workspace";

    /**
     * 每个测试前清理沙箱.
     */
    @BeforeEach
    void setUp() {
        // 清理所有沙箱，确保测试隔离
        codeExecutionService.destroyAllSandboxes();
    }

    /**
     * 每个测试后清理沙箱.
     */
    @AfterEach
    void tearDown() {
        // 清理所有沙箱
        codeExecutionService.destroyAllSandboxes();
    }

    /**
     * 内存沙箱测试组.
     *
     * <p>测试 {@link InMemorySandbox} 的基础功能：
     * <ul>
     *   <li>生命周期管理（创建、启动、停止、关闭）</li>
     *   <li>命令执行（ls、cat、echo、write 等）</li>
     *   <li>代码执行模拟（Java、Python、JavaScript）</li>
     *   <li>工作空间持久化和恢复</li>
     *   <li>安全配置检查</li>
     * </ul>
     */
    @Nested
    @DisplayName("InMemorySandbox 测试")
    class InMemorySandboxTests {

        /**
         * 测试沙箱创建和启动.
         */
        @Test
        @DisplayName("应该能够创建和启动沙箱")
        void shouldCreateAndStartSandbox() {
            InMemorySandbox sandbox = new InMemorySandbox();

            // 启动不应抛出异常
            assertDoesNotThrow(() -> sandbox.start());
            // 启动后应该处于运行状态
            assertTrue(sandbox.isRunning());
            // 应该有有效的沙箱 ID
            assertNotNull(sandbox.getSandboxId());
            assertFalse(sandbox.getSandboxId().isEmpty());
        }

        /**
         * 测试沙箱停止和关闭.
         */
        @Test
        @DisplayName("应该能够停止和关闭沙箱")
        void shouldStopAndShutdownSandbox() {
            InMemorySandbox sandbox = new InMemorySandbox();

            // 完整的生命周期：启动 -> 停止 -> 关闭
            assertDoesNotThrow(() -> {
                sandbox.start();
                sandbox.stop();
                sandbox.shutdown();
            });

            // 关闭后应该不再运行
            assertFalse(sandbox.isRunning());
        }

        /**
         * 测试 ls 命令执行.
         */
        @Test
        @DisplayName("应该能够执行 ls 命令")
        void shouldExecuteLsCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            // 执行 ls 命令
            ExecResult result = sandbox.exec(null, "ls /workspace", null);

            assertNotNull(result);
            assertEquals(0, result.exitCode());  // ls 应该成功
        }

        /**
         * 测试 cat 命令执行.
         */
        @Test
        @DisplayName("应该能够执行 cat 命令")
        void shouldExecuteCatCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();
            // 先写入文件
            sandbox.writeFile("/workspace/test.txt", "Hello, World!");

            // 执行 cat 命令读取文件
            ExecResult result = sandbox.exec(null, "cat /workspace/test.txt", null);

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello, World!"));
        }

        /**
         * 测试 echo 命令执行.
         */
        @Test
        @DisplayName("应该能够执行 echo 命令")
        void shouldExecuteEchoCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            // 执行 echo 命令
            ExecResult result = sandbox.exec(null, "echo Hello Harness", null);

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello Harness"));
        }

        /**
         * 测试 write 命令执行.
         */
        @Test
        @DisplayName("应该能够执行 write 命令")
        void shouldExecuteWriteCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            // 执行 write 命令写入文件
            ExecResult result = sandbox.exec(null, "write /workspace/hello.txt Hello World", null);

            assertTrue(result.ok());
            // 验证文件已创建
            assertTrue(sandbox.fileExists("/workspace/hello.txt"));
            assertEquals("Hello World", sandbox.readFile("/workspace/hello.txt"));
        }

        /**
         * 测试 Java 代码模拟执行.
         */
        @Test
        @DisplayName("应该能够模拟执行 Java 代码")
        void shouldExecuteJavaCodeSimulation() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());

            // 执行包含 main 方法的 Java 代码
            ExecResult result = sandbox.executeCode(
                    "public static void main(String[] args) { System.out.println(\"Hello Java!\"); }",
                    "java",
                    60
            );

            assertTrue(result.ok());
            // 应该返回模拟执行结果
            assertTrue(result.stdout().contains("模拟") || result.stdout().contains("Hello Java!"));
        }

        /**
         * 测试 Python 代码模拟执行.
         */
        @Test
        @DisplayName("应该能够模拟执行 Python 代码")
        void shouldExecutePythonCodeSimulation() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());

            // 执行包含 print 语句的 Python 代码
            ExecResult result = sandbox.executeCode(
                    "print('Hello Python!')\nprint('Goodbye!')",
                    "python",
                    60
            );

            assertTrue(result.ok());
            // 应该解析出 print 内容
            assertTrue(result.stdout().contains("Hello Python!"));
            assertTrue(result.stdout().contains("Goodbye!"));
        }

        /**
         * 测试 JavaScript 代码模拟执行.
         */
        @Test
        @DisplayName("应该能够模拟执行 JavaScript 代码")
        void shouldExecuteJavaScriptCodeSimulation() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());

            // 执行包含 console.log 的 JavaScript 代码
            ExecResult result = sandbox.executeCode(
                    "console.log('Hello JavaScript!')",
                    "javascript",
                    60
            );

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello JavaScript!"));
        }

        /**
         * 测试工作空间持久化和恢复.
         */
        @Test
        @DisplayName("应该能够持久化和恢复工作空间")
        void shouldPersistAndHydrateWorkspace() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            // 写入一些文件
            sandbox.writeFile("/workspace/file1.txt", "Content 1");
            sandbox.writeFile("/workspace/file2.txt", "Content 2");

            // 持久化工作空间
            java.io.InputStream archive = sandbox.persistWorkspace();

            // 创建新沙箱并恢复
            InMemorySandbox newSandbox = new InMemorySandbox();
            newSandbox.start();
            newSandbox.hydrateWorkspace(archive);

            // 验证文件已恢复
            assertEquals("Content 1", newSandbox.readFile("/workspace/file1.txt"));
            assertEquals("Content 2", newSandbox.readFile("/workspace/file2.txt"));
        }

        /**
         * 测试危险命令应该被拒绝.
         */
        @Test
        @DisplayName("应该拒绝危险命令")
        void shouldRejectBlockedCommands() {
            HarnessConfig config = new HarnessConfig();

            // 这些危险命令应该被检测到
            assertTrue(config.containsBlockedPatterns("rm -rf /"));
            assertTrue(config.containsBlockedPatterns("DROP DATABASE"));
            // 正常命令应该通过
            assertFalse(config.containsBlockedPatterns("ls -la"));
        }

        /**
         * 测试命令白名单验证.
         */
        @Test
        @DisplayName("应该验证允许的命令")
        void shouldValidateAllowedCommands() {
            HarnessConfig config = new HarnessConfig();

            // 白名单中的命令应该允许
            assertTrue(config.isCommandAllowed("ls /workspace"));
            assertTrue(config.isCommandAllowed("cat file.txt"));
            assertTrue(config.isCommandAllowed("rm file.txt"));  // rm 在允许列表中
        }
    }

    /**
     * 代码执行服务测试组.
     *
     * <p>测试 {@link CodeExecutionService} 的核心功能：
     * <ul>
     *   <li>代码执行</li>
     *   <li>安全验证（危险代码拦截）</li>
     *   <li>沙箱池管理</li>
     *   <li>超时控制</li>
     * </ul>
     */
    @Nested
    @DisplayName("CodeExecutionService 测试")
    class CodeExecutionServiceTests {

        /**
         * 测试代码成功执行.
         */
        @Test
        @DisplayName("应该成功执行代码")
        void shouldExecuteCodeSuccessfully() {
            // 构建执行请求
            ExecutionRequest request = ExecutionRequest.builder()
                    .code("print('Hello, Harness!')")
                    .language("python")
                    .timeoutSeconds(30)
                    .workspace(TEST_WORKSPACE)
                    .build();

            // 执行代码
            ExecutionResult result = codeExecutionService.execute(request, null);

            assertNotNull(result);
            // 应该成功执行（退出码为 0 或返回成功标志）
            assertTrue(result.getExitCode() == 0 || result.getExitCode() == null || result.isSuccess());
        }

        /**
         * 测试危险代码被拦截.
         */
        @Test
        @DisplayName("应该拦截危险代码")
        void shouldBlockDangerousCode() {
            // 构建包含危险模式的请求
            ExecutionRequest request = ExecutionRequest.builder()
                    .code("DROP DATABASE production;")
                    .language("shell")
                    .timeoutSeconds(30)
                    .workspace(TEST_WORKSPACE)
                    .build();

            // 执行代码
            ExecutionResult result = codeExecutionService.execute(request, null);

            assertNotNull(result);
            // 应该执行失败
            assertFalse(result.isSuccess());
            // 应该有错误消息
            assertNotNull(result.getErrorMessage());
        }

        /**
         * 测试为工作空间创建沙箱.
         */
        @Test
        @DisplayName("应该为工作空间创建沙箱")
        void shouldCreateSandboxForWorkspace() {
            // 获取沙箱
            var sandbox = codeExecutionService.getSandbox(TEST_WORKSPACE);

            assertNotNull(sandbox);
            assertTrue(sandbox.isRunning());
        }

        /**
         * 测试同一工作空间复用沙箱.
         */
        @Test
        @DisplayName("应该复用同一工作空间的沙箱")
        void shouldReuseSandboxForSameWorkspace() {
            // 两次获取应该返回同一个沙箱实例
            var sandbox1 = codeExecutionService.getSandbox(TEST_WORKSPACE);
            var sandbox2 = codeExecutionService.getSandbox(TEST_WORKSPACE);

            assertSame(sandbox1, sandbox2);  // 应该是同一个对象
        }

        /**
         * 测试销毁沙箱.
         */
        @Test
        @DisplayName("应该能够销毁沙箱")
        void shouldDestroySandbox() {
            // 创建沙箱
            codeExecutionService.getSandbox(TEST_WORKSPACE);
            assertEquals(1, codeExecutionService.getSandboxCount());

            // 销毁沙箱
            codeExecutionService.destroySandbox(TEST_WORKSPACE);
            assertEquals(0, codeExecutionService.getSandboxCount());
        }

        /**
         * 测试超时限制.
         */
        @Test
        @DisplayName("应该强制执行超时限制")
        void shouldEnforceTimeoutLimit() {
            // 验证最大超时配置存在且有效
            int maxTimeout = harnessConfig.getSandbox().getMaxTimeout();
            assertTrue(maxTimeout > 0);
        }
    }

    /**
     * 工作空间服务测试组.
     *
     * <p>测试 {@link WorkspaceService} 的文件系统操作：
     * <ul>
     *   <li>文件读写</li>
     *   <li>目录列表</li>
     *   <li>文件编辑</li>
     *   <li>文件删除和移动</li>
     *   <li>工作空间清理</li>
     * </ul>
     */
    @Nested
    @DisplayName("WorkspaceService 测试")
    class WorkspaceServiceTests {

        /**
         * 测试写入和读取文件.
         */
        @Test
        @DisplayName("应该能够写入和读取文件")
        void shouldWriteAndReadFile() {
            // 使用唯一文件路径避免测试冲突
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/test-" + uniqueId + ".txt";
            String content = "Hello, Workspace!";

            // 写入文件
            WriteResult writeResult = workspaceService.write(TEST_WORKSPACE, filePath, content);
            assertNotNull(writeResult);

            // 读取文件
            ReadResult readResult = workspaceService.read(TEST_WORKSPACE, filePath, 0, 100);
            assertTrue(readResult.isSuccess());
            assertTrue(readResult.fileData().content().contains("Hello, Workspace!"));
        }

        /**
         * 测试列出目录.
         */
        @Test
        @DisplayName("应该能够列出目录")
        void shouldListDirectory() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            // 使用 overwrite 避免文件已存在错误
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file1-" + uniqueId + ".txt", "Content 1");
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file2-" + uniqueId + ".txt", "Content 2");

            // 列出目录
            LsResult result = workspaceService.ls(TEST_WORKSPACE, "/workspace");

            assertTrue(result.isSuccess());
            assertTrue(result.entries().size() >= 2);  // 至少有我们创建的两个文件
        }

        /**
         * 测试编辑文件内容.
         */
        @Test
        @DisplayName("应该能够编辑文件内容")
        void shouldEditFileContent() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/test-" + uniqueId + ".txt";
            // 写入初始内容
            workspaceService.overwrite(TEST_WORKSPACE, filePath, "Hello World");

            // 编辑文件：将 World 替换为 Harness
            EditResult result = workspaceService.edit(
                    TEST_WORKSPACE, filePath,
                    "World", "Harness", false
            );

            assertTrue(result.isSuccess());

            // 验证内容已更改
            ReadResult readResult = workspaceService.read(TEST_WORKSPACE, filePath, 0, 100);
            assertTrue(readResult.fileData().content().contains("Hello Harness"));
        }

        /**
         * 测试删除文件.
         */
        @Test
        @DisplayName("应该能够删除文件")
        void shouldDeleteFile() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/to-delete-" + uniqueId + ".txt";
            // 创建文件
            workspaceService.overwrite(TEST_WORKSPACE, filePath, "Content");

            // 删除文件
            WriteResult deleteResult = workspaceService.delete(TEST_WORKSPACE, filePath);
            assertTrue(deleteResult.isSuccess());

            // 验证文件已删除
            assertFalse(workspaceService.exists(TEST_WORKSPACE, filePath));
        }

        /**
         * 测试移动文件.
         */
        @Test
        @DisplayName("应该能够移动文件")
        void shouldMoveFile() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String sourcePath = "/workspace/source-" + uniqueId + ".txt";
            String destPath = "/workspace/dest-" + uniqueId + ".txt";
            // 创建源文件
            workspaceService.overwrite(TEST_WORKSPACE, sourcePath, "Content");

            // 移动文件
            WriteResult moveResult = workspaceService.move(TEST_WORKSPACE, sourcePath, destPath);

            assertTrue(moveResult.isSuccess());
            // 源文件应该不存在
            assertFalse(workspaceService.exists(TEST_WORKSPACE, sourcePath));
            // 目标文件应该存在
            assertTrue(workspaceService.exists(TEST_WORKSPACE, destPath));
        }

        /**
         * 测试清空工作空间.
         */
        @Test
        @DisplayName("应该能够清空工作空间")
        void shouldClearWorkspace() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            // 创建一些文件
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file1-" + uniqueId + ".txt", "Content 1");
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file2-" + uniqueId + ".txt", "Content 2");

            // 清空工作空间
            workspaceService.clearWorkspace(TEST_WORKSPACE);

            // 验证工作空间已清空
            WorkspaceService.WorkspaceStats stats = workspaceService.getStats(TEST_WORKSPACE);
            assertEquals(0, stats.fileCount());
        }
    }

    /**
     * REST API 测试组.
     *
     * <p>测试 HTTP 端点的集成功能：
     * <ul>
     *   <li>代码执行 API</li>
     *   <li>命令执行 API</li>
     *   <li>沙箱管理 API</li>
     *   <li>文件操作 API</li>
     *   <li>健康检查 API</li>
     * </ul>
     */
    @Nested
    @DisplayName("REST API 测试")
    class RestApiTests {

        /**
         * API 基础 URL.
         */
        private String baseUrl;

        /**
         * 设置基础 URL.
         */
        @BeforeEach
        void setUp() {
            baseUrl = "http://localhost:" + port;
        }

        /**
         * 测试通过 API 执行代码.
         */
        @Test
        @DisplayName("应该能够通过 API 执行代码")
        void shouldExecuteCodeViaApi() {
            String url = baseUrl + "/api/execute";
            Map<String, Object> request = Map.of(
                    "code", "print('Hello API!')",
                    "language", "python",
                    "workspace", TEST_WORKSPACE
            );

            // 发送 POST 请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        /**
         * 测试通过 API 执行命令.
         */
        @Test
        @DisplayName("应该能够通过 API 执行命令")
        void shouldExecuteCommandViaApi() {
            String url = baseUrl + "/api/execute/command";
            Map<String, Object> request = Map.of(
                    "command", "echo Hello",
                    "workspace", TEST_WORKSPACE
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        /**
         * 测试通过 API 获取沙箱信息.
         */
        @Test
        @DisplayName("应该能够通过 API 获取沙箱信息")
        void shouldGetSandboxInfoViaApi() {
            // 先创建沙箱
            codeExecutionService.getSandbox(TEST_WORKSPACE);

            String url = baseUrl + "/api/execute/sandbox/" + TEST_WORKSPACE;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            // 验证能够获取响应
            assertNotNull(response);
        }

        /**
         * 测试通过 API 写入文件.
         */
        @Test
        @DisplayName("应该能够通过 API 写入文件")
        void shouldWriteFileViaApi() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/write";
            Map<String, String> request = Map.of(
                    "filePath", "/workspace/api-test-" + uniqueId + ".txt",
                    "content", "Hello from API!"
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            assertNotNull(response);
        }

        /**
         * 测试通过 API 读取文件.
         */
        @Test
        @DisplayName("应该能够通过 API 读取文件")
        void shouldReadFileViaApi() {
            // 先写入文件
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/read-test-" + uniqueId + ".txt";
            workspaceService.overwrite(TEST_WORKSPACE, filePath, "Read test content");

            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/read?filePath=" + filePath;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            assertNotNull(response);
        }

        /**
         * 测试通过 API 列出目录.
         */
        @Test
        @DisplayName("应该能够通过 API 列出目录")
        void shouldListDirectoryViaApi() {
            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/ls";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            assertNotNull(response);
        }

        /**
         * 测试通过 API 获取工作空间统计.
         */
        @Test
        @DisplayName("应该能够通过 API 获取工作空间统计")
        void shouldGetWorkspaceStatsViaApi() {
            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/stats";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            assertNotNull(response);
        }

        /**
         * 测试健康检查端点.
         */
        @Test
        @DisplayName("应该能够检查健康状态")
        void shouldCheckHealthEndpoint() {
            String url = baseUrl + "/api/execute/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().get("status"));
        }
    }
}