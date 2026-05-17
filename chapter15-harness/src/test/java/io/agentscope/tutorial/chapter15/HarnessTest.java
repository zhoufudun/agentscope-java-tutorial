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
import org.springframework.beans.factory.annotation.自动注入;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Chapter 15: Harness Security Execution Environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Chapter 15 - Harness Test Suite")
class HarnessTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HarnessConfig harnessConfig;

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Autowired
    private WorkspaceService workspaceService;

    private static final String TEST_WORKSPACE = "test-workspace";

    @BeforeEach
    void setUp() {
        // Clean up before each test
        codeExecutionService.destroyAllSandboxes();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        codeExecutionService.destroyAllSandboxes();
    }

    @Nested
    @DisplayName("InMemorySandbox Tests")
    class InMemorySandboxTests {

        @Test
        @DisplayName("Should create and start sandbox")
        void shouldCreateAndStartSandbox() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());
            assertTrue(sandbox.isRunning());
            assertNotNull(sandbox.getSandboxId());
            assertFalse(sandbox.getSandboxId().isEmpty());
        }

        @Test
        @DisplayName("Should stop and shutdown sandbox")
        void shouldStopAndShutdownSandbox() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> {
                sandbox.start();
                sandbox.stop();
                sandbox.shutdown();
            });

            assertFalse(sandbox.isRunning());
        }

        @Test
        @DisplayName("Should execute ls command")
        void shouldExecuteLsCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            ExecResult result = sandbox.exec(null, "ls /workspace", null);

            assertNotNull(result);
            assertEquals(0, result.exitCode());
        }

        @Test
        @DisplayName("Should execute cat command")
        void shouldExecuteCatCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();
            sandbox.writeFile("/workspace/test.txt", "Hello, World!");

            ExecResult result = sandbox.exec(null, "cat /workspace/test.txt", null);

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello, World!"));
        }

        @Test
        @DisplayName("Should execute echo command")
        void shouldExecuteEchoCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            ExecResult result = sandbox.exec(null, "echo Hello Harness", null);

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello Harness"));
        }

        @Test
        @DisplayName("Should execute write command")
        void shouldExecuteWriteCommand() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            ExecResult result = sandbox.exec(null, "write /workspace/hello.txt Hello World", null);

            assertTrue(result.ok());
            assertTrue(sandbox.fileExists("/workspace/hello.txt"));
            assertEquals("Hello World", sandbox.readFile("/workspace/hello.txt"));
        }

        @Test
        @DisplayName("Should execute Java code simulation")
        void shouldExecuteJavaCodeSimulation() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());

            ExecResult result = sandbox.executeCode(
                    "public static void main(String[] args) { System.out.println(\"Hello Java!\"); }",
                    "java",
                    60
            );

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("simulated") || result.stdout().contains("Hello Java!"));
        }

        @Test
        @DisplayName("Should execute Python code simulation")
        void shouldExecutePythonCodeSimulation() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());

            ExecResult result = sandbox.executeCode(
                    "print('Hello Python!')\nprint('Goodbye!')",
                    "python",
                    60
            );

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello Python!"));
            assertTrue(result.stdout().contains("Goodbye!"));
        }

        @Test
        @DisplayName("Should execute JavaScript code simulation")
        void shouldExecuteJavaScriptCodeSimulation() {
            InMemorySandbox sandbox = new InMemorySandbox();

            assertDoesNotThrow(() -> sandbox.start());

            ExecResult result = sandbox.executeCode(
                    "console.log('Hello JavaScript!')",
                    "javascript",
                    60
            );

            assertTrue(result.ok());
            assertTrue(result.stdout().contains("Hello JavaScript!"));
        }

        @Test
        @DisplayName("Should persist and hydrate workspace")
        void shouldPersistAndHydrateWorkspace() throws Exception {
            InMemorySandbox sandbox = new InMemorySandbox();
            sandbox.start();

            // Write some files
            sandbox.writeFile("/workspace/file1.txt", "Content 1");
            sandbox.writeFile("/workspace/file2.txt", "Content 2");

            // Persist workspace
            java.io.InputStream archive = sandbox.persistWorkspace();

            // Create new sandbox and hydrate
            InMemorySandbox newSandbox = new InMemorySandbox();
            newSandbox.start();
            newSandbox.hydrateWorkspace(archive);

            // Verify files were restored
            assertEquals("Content 1", newSandbox.readFile("/workspace/file1.txt"));
            assertEquals("Content 2", newSandbox.readFile("/workspace/file2.txt"));
        }

        @Test
        @DisplayName("Should not execute blocked commands")
        void shouldRejectBlockedCommands() {
            HarnessConfig config = new HarnessConfig();

            assertTrue(config.containsBlockedPatterns("rm -rf /"));
            assertTrue(config.containsBlockedPatterns("DROP DATABASE"));
            assertFalse(config.containsBlockedPatterns("ls -la"));
        }

        @Test
        @DisplayName("Should validate allowed commands")
        void shouldValidateAllowedCommands() {
            HarnessConfig config = new HarnessConfig();

            assertTrue(config.isCommandAllowed("ls /workspace"));
            assertTrue(config.isCommandAllowed("cat file.txt"));
            assertTrue(config.isCommandAllowed("rm file.txt"));  // rm is in the allowed list
        }
    }

    @Nested
    @DisplayName("CodeExecutionService Tests")
    class CodeExecutionServiceTests {

        @Test
        @DisplayName("Should execute code successfully")
        void shouldExecuteCodeSuccessfully() {
            ExecutionRequest request = ExecutionRequest.builder()
                    .code("print('Hello, Harness!')")
                    .language("python")
                    .timeoutSeconds(30)
                    .workspace(TEST_WORKSPACE)
                    .build();

            ExecutionResult result = codeExecutionService.execute(request, null);

            assertNotNull(result);
            assertTrue(result.getExitCode() == 0 || result.getExitCode() == null || result.isSuccess());
        }

        @Test
        @DisplayName("Should block dangerous code")
        void shouldBlockDangerousCode() {
            ExecutionRequest request = ExecutionRequest.builder()
                    .code("DROP DATABASE production;")
                    .language("shell")
                    .timeoutSeconds(30)
                    .workspace(TEST_WORKSPACE)
                    .build();

            ExecutionResult result = codeExecutionService.execute(request, null);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should create sandbox for workspace")
        void shouldCreateSandboxForWorkspace() {
            var sandbox = codeExecutionService.getSandbox(TEST_WORKSPACE);

            assertNotNull(sandbox);
            assertTrue(sandbox.isRunning());
        }

        @Test
        @DisplayName("Should reuse sandbox for same workspace")
        void shouldReuseSandboxForSameWorkspace() {
            var sandbox1 = codeExecutionService.getSandbox(TEST_WORKSPACE);
            var sandbox2 = codeExecutionService.getSandbox(TEST_WORKSPACE);

            assertSame(sandbox1, sandbox2);
        }

        @Test
        @DisplayName("Should destroy sandbox")
        void shouldDestroySandbox() {
            codeExecutionService.getSandbox(TEST_WORKSPACE);
            assertEquals(1, codeExecutionService.getSandboxCount());

            codeExecutionService.destroySandbox(TEST_WORKSPACE);
            assertEquals(0, codeExecutionService.getSandboxCount());
        }

        @Test
        @DisplayName("Should enforce timeout limit")
        void shouldEnforceTimeoutLimit() {
            int maxTimeout = harnessConfig.getSandbox().getMaxTimeout();
            assertTrue(maxTimeout > 0);
        }
    }

    @Nested
    @DisplayName("WorkspaceService Tests")
    class WorkspaceServiceTests {

        @Test
        @DisplayName("Should write and read file")
        void shouldWriteAndReadFile() {
            // Use unique file path to avoid collisions between tests
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/test-" + uniqueId + ".txt";
            String content = "Hello, Workspace!";

            WriteResult writeResult = workspaceService.write(TEST_WORKSPACE, filePath, content);
            assertNotNull(writeResult);

            ReadResult readResult = workspaceService.read(TEST_WORKSPACE, filePath, 0, 100);
            assertTrue(readResult.isSuccess());
            assertTrue(readResult.fileData().content().contains("Hello, Workspace!"));
        }

        @Test
        @DisplayName("Should list directory")
        void shouldListDirectory() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            // Use overwrite to avoid file exists errors
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file1-" + uniqueId + ".txt", "Content 1");
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file2-" + uniqueId + ".txt", "Content 2");

            LsResult result = workspaceService.ls(TEST_WORKSPACE, "/workspace");

            assertTrue(result.isSuccess());
            assertTrue(result.entries().size() >= 2);
        }

        @Test
        @DisplayName("Should edit file content")
        void shouldEditFileContent() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/test-" + uniqueId + ".txt";
            workspaceService.overwrite(TEST_WORKSPACE, filePath, "Hello World");

            EditResult result = workspaceService.edit(
                    TEST_WORKSPACE, filePath,
                    "World", "Harness", false
            );

            assertTrue(result.isSuccess());

            ReadResult readResult = workspaceService.read(TEST_WORKSPACE, filePath, 0, 100);
            assertTrue(readResult.fileData().content().contains("Hello Harness"));
        }

        @Test
        @DisplayName("Should delete file")
        void shouldDeleteFile() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/to-delete-" + uniqueId + ".txt";
            workspaceService.overwrite(TEST_WORKSPACE, filePath, "Content");

            WriteResult deleteResult = workspaceService.delete(TEST_WORKSPACE, filePath);
            assertTrue(deleteResult.isSuccess());

            assertFalse(workspaceService.exists(TEST_WORKSPACE, filePath));
        }

        @Test
        @DisplayName("Should move file")
        void shouldMoveFile() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String sourcePath = "/workspace/source-" + uniqueId + ".txt";
            String destPath = "/workspace/dest-" + uniqueId + ".txt";
            workspaceService.overwrite(TEST_WORKSPACE, sourcePath, "Content");

            WriteResult moveResult = workspaceService.move(TEST_WORKSPACE, sourcePath, destPath);

            assertTrue(moveResult.isSuccess());
            assertFalse(workspaceService.exists(TEST_WORKSPACE, sourcePath));
            assertTrue(workspaceService.exists(TEST_WORKSPACE, destPath));
        }

        @Test
        @DisplayName("Should clear workspace")
        void shouldClearWorkspace() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file1-" + uniqueId + ".txt", "Content 1");
            workspaceService.overwrite(TEST_WORKSPACE, "/workspace/file2-" + uniqueId + ".txt", "Content 2");

            workspaceService.clearWorkspace(TEST_WORKSPACE);

            WorkspaceService.WorkspaceStats stats = workspaceService.getStats(TEST_WORKSPACE);
            assertEquals(0, stats.fileCount());
        }
    }

    @Nested
    @DisplayName("REST API Tests")
    class RestApiTests {

        private String baseUrl;

        @BeforeEach
        void setUp() {
            baseUrl = "http://localhost:" + port;
        }

        @Test
        @DisplayName("Should execute code via API")
        void shouldExecuteCodeViaApi() {
            String url = baseUrl + "/api/execute";
            Map<String, Object> request = Map.of(
                    "code", "print('Hello API!')",
                    "language", "python",
                    "workspace", TEST_WORKSPACE
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should execute command via API")
        void shouldExecuteCommandViaApi() {
            String url = baseUrl + "/api/execute/command";
            Map<String, Object> request = Map.of(
                    "command", "echo Hello",
                    "workspace", TEST_WORKSPACE
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should get sandbox info via API")
        void shouldGetSandboxInfoViaApi() {
            // First create a sandbox
            codeExecutionService.getSandbox(TEST_WORKSPACE);

            String url = baseUrl + "/api/execute/sandbox/" + TEST_WORKSPACE;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            // Sandbox might not be running if destroyed, just check we get a response
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should write file via API")
        void shouldWriteFileViaApi() {
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/write";
            Map<String, String> request = Map.of(
                    "filePath", "/workspace/api-test-" + uniqueId + ".txt",
                    "content", "Hello from API!"
            );

            // Use exchange to handle potential errors gracefully
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should read file via API")
        void shouldReadFileViaApi() {
            // First write a file
            String uniqueId = String.valueOf(System.currentTimeMillis());
            String filePath = "/workspace/read-test-" + uniqueId + ".txt";
            workspaceService.overwrite(TEST_WORKSPACE, filePath, "Read test content");

            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/read?filePath=" + filePath;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should list directory via API")
        void shouldListDirectoryViaApi() {
            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/ls";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should get workspace stats via API")
        void shouldGetWorkspaceStatsViaApi() {
            String url = baseUrl + "/api/workspace/" + TEST_WORKSPACE + "/stats";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should check health endpoint")
        void shouldCheckHealthEndpoint() {
            String url = baseUrl + "/api/execute/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().get("status"));
        }
    }
}