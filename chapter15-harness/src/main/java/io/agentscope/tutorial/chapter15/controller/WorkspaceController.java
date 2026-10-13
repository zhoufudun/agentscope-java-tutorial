package io.agentscope.tutorial.chapter15.controller;

import io.agentscope.harness.agent.filesystem.model.*;
import io.agentscope.tutorial.chapter15.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 工作空间文件系统 REST 控制器.
 *
 * <h2>概述</h2>
 * <p>本控制器提供工作空间文件系统操作的 REST API。每个工作空间都有独立的文件系统，
 * 实现了多租户隔离。控制器封装了 {@link WorkspaceService} 的功能。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li><b>文件操作</b>：读取、写入、编辑、删除、移动文件</li>
 *   <li><b>目录操作</b>：列出目录内容</li>
 *   <li><b>搜索功能</b>：grep 文本搜索、glob 文件匹配</li>
 *   <li><b>批量操作</b>：上传和下载多个文件</li>
 *   <li><b>工作空间管理</b>：统计信息、存在性检查、清空工作空间</li>
 * </ul>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>GET /api/workspace/{id}/ls - 列出目录内容</li>
 *   <li>GET /api/workspace/{id}/read - 读取文件内容</li>
 *   <li>POST /api/workspace/{id}/write - 写入新文件</li>
 *   <li>PUT /api/workspace/{id}/write - 覆盖写入文件</li>
 *   <li>PUT /api/workspace/{id}/edit - 编辑文件（字符串替换）</li>
 *   <li>DELETE /api/workspace/{id}/file - 删除文件</li>
 *   <li>POST /api/workspace/{id}/move - 移动文件</li>
 *   <li>GET /api/workspace/{id}/grep - 搜索文本模式</li>
 *   <li>GET /api/workspace/{id}/glob - 匹配文件名模式</li>
 *   <li>POST /api/workspace/{id}/upload - 上传文件</li>
 *   <li>POST /api/workspace/{id}/download - 下载文件</li>
 *   <li>GET /api/workspace/{id}/exists - 检查文件是否存在</li>
 *   <li>GET /api/workspace/{id}/stats - 获取工作空间统计</li>
 *   <li>DELETE /api/workspace/{id} - 清空工作空间</li>
 * </ul>
 *
 * <h2>工作空间隔离</h2>
 * <p>每个工作空间 ID 对应一个独立的文件系统实例，确保不同用户/会话之间的数据隔离。
 *
 * @author AgentScope Tutorial
 * @see WorkspaceService
 */
@Slf4j  // 自动生成日志对象
@RestController  // REST 控制器
@RequestMapping("/api/workspace")  // 基础路径
@RequiredArgsConstructor  // 构造函数注入
public class WorkspaceController {

    /**
     * 工作空间服务.
     * 提供实际的文件系统操作实现。
     */
    private final WorkspaceService workspaceService;

    /**
     * 列出目录内容.
     *
     * <p>返回指定目录下的所有文件和子目录，类似于 Linux 的 ls 命令。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * GET /api/workspace/my-workspace/ls?path=/workspace/src
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param path 目录路径，默认为 /workspace
     * @return 目录内容列表，包含文件名、大小、类型等信息
     */
    @GetMapping("/{workspaceId}/ls")
    public ResponseEntity<LsResult> listDirectory(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "/workspace") String path) {

        log.debug("列出工作空间 {} 中的目录 {}", workspaceId, path);
        LsResult result = workspaceService.ls(workspaceId, path);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 读取文件内容.
     *
     * <p>读取指定文件的内容，支持分页读取大文件。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * GET /api/workspace/my-workspace/read?filePath=/workspace/src/Main.java&offset=0&limit=100
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param filePath 文件路径（必需）
     * @param offset 起始行号，从 0 开始，默认 0
     * @param limit 最大读取行数，默认 1000
     * @return 文件内容
     */
    @GetMapping("/{workspaceId}/read")
    public ResponseEntity<ReadResult> readFile(
            @PathVariable String workspaceId,
            @RequestParam String filePath,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "1000") int limit) {

        log.debug("读取工作空间 {} 中的文件 {}", workspaceId, filePath);
        ReadResult result = workspaceService.read(workspaceId, filePath, offset, limit);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 写入新文件.
     *
     * <p>创建新文件并写入内容。如果文件已存在，操作将失败。
     * 如需覆盖已存在的文件，请使用 PUT 方法。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * POST /api/workspace/my-workspace/write
     * Content-Type: application/json
     *
     * {
     *   "filePath": "/workspace/hello.txt",
     *   "content": "Hello, World!"
     * }
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param request 包含 filePath 和 content 的请求体
     * @return 写入结果
     */
    @PostMapping("/{workspaceId}/write")
    public ResponseEntity<WriteResult> writeFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {

        String filePath = request.get("filePath");
        String content = request.get("content");

        // 验证文件路径
        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(
                    WriteResult.fail("filePath 是必需的")
            );
        }

        log.info("在工作空间 {} 中写入文件 {}", workspaceId, filePath);
        WriteResult result = workspaceService.write(workspaceId, filePath, content);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 覆盖写入文件（如不存在则创建）.
     *
     * <p>写入文件内容，如果文件已存在则覆盖。这是幂等操作。
     *
     * @param workspaceId 工作空间标识符
     * @param request 包含 filePath 和 content 的请求体
     * @return 写入结果
     */
    @PutMapping("/{workspaceId}/write")
    public ResponseEntity<WriteResult> overwriteFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {

        String filePath = request.get("filePath");
        String content = request.get("content");

        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(
                    WriteResult.fail("filePath 是必需的")
            );
        }

        log.info("在工作空间 {} 中覆盖写入文件 {}", workspaceId, filePath);
        WriteResult result = workspaceService.overwrite(workspaceId, filePath, content);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 编辑文件内容（字符串替换）.
     *
     * <p>在文件中查找指定字符串并替换为新字符串。
     * 这是一种安全的编辑方式，避免了全文覆盖可能导致的问题。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * PUT /api/workspace/my-workspace/edit
     * Content-Type: application/json
     *
     * {
     *   "filePath": "/workspace/config.txt",
     *   "oldString": "debug=false",
     *   "newString": "debug=true",
     *   "replaceAll": false
     * }
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param request 包含 filePath、oldString、newString 和可选的 replaceAll 的请求体
     * @return 编辑结果，包含替换次数
     */
    @PutMapping("/{workspaceId}/edit")
    public ResponseEntity<EditResult> editFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, Object> request) {

        String filePath = (String) request.get("filePath");
        String oldString = (String) request.get("oldString");
        String newString = (String) request.get("newString");
        Boolean replaceAll = (Boolean) request.getOrDefault("replaceAll", false);

        // 验证必需参数
        if (filePath == null || oldString == null || newString == null) {
            return ResponseEntity.badRequest().body(
                    EditResult.fail("filePath、oldString 和 newString 是必需的")
            );
        }

        log.info("在工作空间 {} 中编辑文件 {}: 将 '{}' 替换为 '{}'",
                workspaceId, filePath, oldString, newString);
        EditResult result = workspaceService.edit(workspaceId, filePath, oldString, newString, replaceAll);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 删除文件.
     *
     * <p>删除指定路径的文件或目录。
     *
     * @param workspaceId 工作空间标识符
     * @param path 要删除的文件或目录路径
     * @return 删除结果
     */
    @DeleteMapping("/{workspaceId}/file")
    public ResponseEntity<WriteResult> deleteFile(
            @PathVariable String workspaceId,
            @RequestParam String path) {

        log.info("从工作空间 {} 删除 {}", workspaceId, path);
        WriteResult result = workspaceService.delete(workspaceId, path);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 移动文件.
     *
     * <p>将文件从一个位置移动到另一个位置。也可用于重命名文件。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * POST /api/workspace/my-workspace/move
     * Content-Type: application/json
     *
     * {
     *   "from": "/workspace/old-name.txt",
     *   "to": "/workspace/new-name.txt"
     * }
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param request 包含 from 和 to 路径的请求体
     * @return 移动结果
     */
    @PostMapping("/{workspaceId}/move")
    public ResponseEntity<WriteResult> moveFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {

        String fromPath = request.get("from");
        String toPath = request.get("to");

        if (fromPath == null || toPath == null) {
            return ResponseEntity.badRequest().body(
                    WriteResult.fail("from 和 to 路径是必需的")
            );
        }

        log.info("在工作空间 {} 中将 {} 移动到 {}", workspaceId, fromPath, toPath);
        WriteResult result = workspaceService.move(workspaceId, fromPath, toPath);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 搜索文本模式（grep）.
     *
     * <p>在文件中搜索指定的文本模式，类似于 Linux 的 grep 命令。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * GET /api/workspace/my-workspace/grep?pattern=TODO&path=/workspace/src&glob=*.java
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param pattern 要搜索的文本模式（必需）
     * @param path 搜索的起始路径（可选）
     * @param glob 文件名匹配模式（可选，如 *.java）
     * @return 匹配结果列表，包含文件路径、行号和匹配内容
     */
    @GetMapping("/{workspaceId}/grep")
    public ResponseEntity<GrepResult> grep(
            @PathVariable String workspaceId,
            @RequestParam String pattern,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String glob) {

        log.debug("在工作空间 {} 中搜索 '{}'", workspaceId, pattern);
        GrepResult result = workspaceService.grep(workspaceId, pattern, path, glob);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 查找匹配模式的文件（glob）.
     *
     * <p>查找文件名匹配指定模式的所有文件，支持通配符。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * GET /api/workspace/my-workspace/glob?pattern=*.java&path=/workspace/src
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param pattern glob 模式（必需，如 *.java、test-*.xml）
     * @param path 搜索的起始路径（可选）
     * @return 匹配的文件列表
     */
    @GetMapping("/{workspaceId}/glob")
    public ResponseEntity<GlobResult> glob(
            @PathVariable String workspaceId,
            @RequestParam String pattern,
            @RequestParam(required = false) String path) {

        log.debug("在工作空间 {} 中 glob 匹配 '{}'", workspaceId, pattern);
        GlobResult result = workspaceService.glob(workspaceId, pattern, path);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 上传多个文件.
     *
     * <p>支持批量上传多个文件到工作空间。使用 multipart/form-data 格式。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * POST /api/workspace/my-workspace/upload
     * Content-Type: multipart/form-data
     *
     * files: [file1.txt, file2.txt, ...]
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param files 要上传的文件列表
     * @return 每个文件的上传结果
     */
    @PostMapping(value = "/{workspaceId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadFiles(
            @PathVariable String workspaceId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("上传 {} 个文件到工作空间 {}", files.size(), workspaceId);

        // 将 MultipartFile 转换为 (文件名, 字节数组) 对
        List<Map.Entry<String, byte[]>> fileEntries = files.stream()
                .map(f -> {
                    try {
                        return Map.entry(f.getOriginalFilename(), f.getBytes());
                    } catch (Exception e) {
                        log.error("读取文件 {} 失败: {}", f.getOriginalFilename(), e.getMessage());
                        return null;
                    }
                })
                .filter(e -> e != null)
                .toList();

        List<FileUploadResponse> results = workspaceService.uploadFiles(workspaceId, fileEntries);
        return ResponseEntity.ok(results);
    }

    /**
     * 下载多个文件.
     *
     * <p>批量下载指定路径的文件内容。
     *
     * <h3>请求示例</h3>
     * <pre>{@code
     * POST /api/workspace/my-workspace/download
     * Content-Type: application/json
     *
     * {
     *   "paths": ["/workspace/file1.txt", "/workspace/file2.txt"]
     * }
     * }</pre>
     *
     * @param workspaceId 工作空间标识符
     * @param request 包含文件路径列表的请求体
     * @return 每个文件的下载结果，包含文件内容
     */
    @PostMapping("/{workspaceId}/download")
    public ResponseEntity<List<FileDownloadResponse>> downloadFiles(
            @PathVariable String workspaceId,
            @RequestBody Map<String, List<String>> request) {

        List<String> paths = request.get("paths");
        if (paths == null || paths.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("从工作空间 {} 下载 {} 个文件", workspaceId, paths.size());
        List<FileDownloadResponse> results = workspaceService.downloadFiles(workspaceId, paths);
        return ResponseEntity.ok(results);
    }

    /**
     * 检查文件是否存在.
     *
     * <p>快速检查指定路径的文件或目录是否存在。
     *
     * @param workspaceId 工作空间标识符
     * @param path 要检查的路径
     * @return 包含 path 和 exists 的响应
     */
    @GetMapping("/{workspaceId}/exists")
    public ResponseEntity<Map<String, Object>> exists(
            @PathVariable String workspaceId,
            @RequestParam String path) {

        boolean exists = workspaceService.exists(workspaceId, path);
        return ResponseEntity.ok(Map.of(
                "path", path,
                "exists", exists
        ));
    }

    /**
     * 获取工作空间统计信息.
     *
     * <p>返回工作空间的统计信息，包括文件数量、总大小等。
     *
     * @param workspaceId 工作空间标识符
     * @return 工作空间统计信息
     */
    @GetMapping("/{workspaceId}/stats")
    public ResponseEntity<WorkspaceService.WorkspaceStats> getStats(
            @PathVariable String workspaceId) {

        WorkspaceService.WorkspaceStats stats = workspaceService.getStats(workspaceId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 清空工作空间.
     *
     * <p>删除工作空间中的所有文件并释放资源。
     * 这是一个破坏性操作，应谨慎使用。
     *
     * @param workspaceId 要清空的工作空间标识符
     * @return 200 OK 表示成功
     */
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> clearWorkspace(@PathVariable String workspaceId) {
        log.info("清空工作空间: {}", workspaceId);
        workspaceService.clearWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }
}