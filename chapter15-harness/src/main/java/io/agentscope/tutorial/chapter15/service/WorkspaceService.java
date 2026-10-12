package io.agentscope.tutorial.chapter15.service;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.*;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作空间管理服务 - 提供文件系统操作.
 *
 * <h2>概述</h2>
 * <p>本服务是 Harness 安全执行环境的文件系统管理组件，负责：
 * <ul>
 *   <li>每个工作空间的文件系统隔离</li>
 *   <li>文件操作（读取、写入、编辑、删除、移动）</li>
 *   <li>目录操作（列出目录、glob 匹配、grep 搜索）</li>
 *   <li>文件上传和下载</li>
 * </ul>
 *
 * <h2>工作空间隔离</h2>
 * <p>每个工作空间 ID 对应一个独立的 {@link InMemorySandbox} 实例，
 * 实现了多租户之间的数据隔离。工作空间按需创建，首次访问时自动初始化。
 *
 * <h2>文件路径规范</h2>
 * <ul>
 *   <li>所有路径都相对于 /workspace 根目录</li>
 *   <li>路径会自动规范化（移除尾部斜杠等）</li>
 *   <li>相对路径会自动转换为绝对路径</li>
 * </ul>
 *
 * <h2>与 CodeExecutionService 的关系</h2>
 * <p>本服务专注于文件系统操作，而 {@link CodeExecutionService} 专注于代码执行。
 * 两者共同构成 Harness 的完整功能集。
 *
 * @author AgentScope Tutorial
 * @see InMemorySandbox
 * @see CodeExecutionService
 */
@Slf4j
@Service  // Spring 服务组件
@RequiredArgsConstructor  // 构造函数注入
public class WorkspaceService {

    /**
     * 代码执行服务.
     * 注入此服务以复用沙箱管理功能（虽然本服务维护自己的文件系统映射）。
     */
    private final CodeExecutionService codeExecutionService;

    /**
     * 文件系统映射.
     * 映射：工作空间 ID -> InMemorySandbox 实例
     * 每个工作空间有独立的内存文件系统。
     */
    private final Map<String, InMemorySandbox> filesystems = new ConcurrentHashMap<>();

    /**
     * 获取工作空间的文件系统.
     *
     * <p>使用 computeIfAbsent 保证线程安全的懒加载。
     *
     * @param workspaceId 工作空间标识符
     * @return 文件系统实例
     */
    public InMemorySandbox getFilesystem(String workspaceId) {
        return filesystems.computeIfAbsent(workspaceId, this::createFilesystem);
    }

    /**
     * 为工作空间创建新的文件系统.
     *
     * <p>创建流程：
     * <ol>
     *   <li>创建 InMemorySandbox 实例</li>
     *   <li>启动沙箱</li>
     *   <li>初始化默认文件（README.md, .workspace_id）</li>
     * </ol>
     *
     * @param workspaceId 工作空间标识符
     * @return 新创建的文件系统
     */
    private InMemorySandbox createFilesystem(String workspaceId) {
        InMemorySandbox sandbox = new InMemorySandbox();
        try {
            sandbox.start();
            // 初始化默认工作空间文件
            sandbox.writeFile("/workspace/README.md", "# 工作空间 " + workspaceId + "\n\n欢迎!");
            sandbox.writeFile("/workspace/.workspace_id", workspaceId);
            log.info("为工作空间 {} 创建文件系统", workspaceId);
        } catch (Exception e) {
            log.error("为工作空间 {} 创建文件系统失败: {}", workspaceId, e.getMessage());
        }
        return sandbox;
    }

    /**
     * 列出目录内容.
     *
     * <p>返回指定目录下的所有文件和子目录，包括：
     * <ul>
     *   <li>文件名/目录名</li>
     *   <li>文件大小</li>
     *   <li>文件类型（文件或目录）</li>
     * </ul>
     *
     * @param workspaceId 工作空间 ID
     * @param path 目录路径
     * @return 目录内容列表
     */
    public LsResult ls(String workspaceId, String path) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(path);

        // 过滤出目标目录下的项目
        List<FileInfo> entries = sandbox.getFileSystem().keySet().stream()
                .filter(p -> p.startsWith(normalizedPath))
                .map(p -> {
                    String relativePath = p.substring(normalizedPath.length());
                    if (relativePath.isEmpty() || relativePath.equals("/")) {
                        return null;  // 排除目录本身
                    }
                    // 获取路径的第一层
                    int slashIdx = relativePath.indexOf('/', 1);
                    if (slashIdx == -1) {
                        // 文件
                        return FileInfo.ofFile(p, sandbox.getFileSystem().get(p).length, "");
                    } else {
                        // 目录
                        return FileInfo.ofDir(p.substring(0, slashIdx), "");
                    }
                })
                .filter(e -> e != null)
                .distinct()
                .sorted((a, b) -> {
                    // 目录优先，然后按名称排序
                    if (a.isDirectory() != b.isDirectory()) {
                        return a.isDirectory() ? -1 : 1;
                    }
                    return a.path().compareTo(b.path());
                })
                .toList();

        return LsResult.success(entries);
    }

    /**
     * 读取文件内容.
     *
     * <p>支持分页读取大文件：
     * <ul>
     *   <li>offset: 起始行号</li>
     *   <li>limit: 最大读取行数</li>
     * </ul>
     *
     * @param workspaceId 工作空间 ID
     * @param filePath 文件路径
     * @param offset 起始行号（从 0 开始）
     * @param limit 最大读取行数
     * @return 文件内容
     */
    public ReadResult read(String workspaceId, String filePath, int offset, int limit) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        // 读取文件
        String content = sandbox.readFile(normalizedPath);
        if (content == null) {
            return ReadResult.fail("文件不存在: " + filePath);
        }

        // 分页处理
        String[] lines = content.split("\n");
        int start = Math.min(offset, lines.length);
        int end = Math.min(offset + limit, lines.length);
        String result = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));

        return ReadResult.success(new FileData(result, "utf-8"));
    }

    /**
     * 写入文件（创建新文件）.
     *
     * <p>如果文件已存在，操作将失败。
     * 如需覆盖已存在的文件，请使用 {@link #overwrite} 方法。
     *
     * @param workspaceId 工作空间 ID
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 写入结果
     */
    public WriteResult write(String workspaceId, String filePath, String content) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        // 检查文件是否已存在
        if (sandbox.fileExists(normalizedPath)) {
            return WriteResult.fail("文件已存在: " + filePath);
        }

        sandbox.writeFile(normalizedPath, content);
        log.info("在工作空间 {} 中写入文件 {}", workspaceId, filePath);
        return WriteResult.ok(filePath);
    }

    /**
     * 覆盖写入文件（如不存在则创建）.
     *
     * <p>这是幂等操作，无论文件是否存在都会成功。
     *
     * @param workspaceId 工作空间 ID
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 写入结果
     */
    public WriteResult overwrite(String workspaceId, String filePath, String content) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        sandbox.writeFile(normalizedPath, content);
        log.info("在工作空间 {} 中覆盖写入文件 {}", workspaceId, filePath);
        return WriteResult.ok(filePath);
    }

    /**
     * 编辑文件（字符串替换）.
     *
     * <p>在文件中查找并替换指定字符串。这是一种安全的编辑方式，
     * 避免了全文覆盖可能导致的并发问题。
     *
     * @param workspaceId 工作空间 ID
     * @param filePath 文件路径
     * @param oldString 要查找的字符串
     * @param newString 替换后的字符串
     * @param replaceAll 是否替换所有匹配
     * @return 编辑结果
     */
    public EditResult edit(String workspaceId, String filePath, String oldString,
                           String newString, boolean replaceAll) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        // 读取文件
        String content = sandbox.readFile(normalizedPath);
        if (content == null) {
            return EditResult.fail("文件不存在: " + filePath);
        }

        // 计算匹配次数
        int count = content.split(oldString, -1).length - 1;
        if (count == 0) {
            return EditResult.fail("未找到字符串: " + oldString);
        }
        // 如果有多个匹配但未指定 replaceAll，拒绝操作
        if (count > 1 && !replaceAll) {
            return EditResult.fail("存在多个匹配。请使用 replaceAll=true 替换全部。");
        }

        // 执行替换
        String newContent = replaceAll
                ? content.replace(oldString, newString)
                : content.replaceFirst(oldString, newString);

        sandbox.writeFile(normalizedPath, newContent);
        log.info("在工作空间 {} 中编辑文件 {}: 替换了 {} 处", workspaceId, filePath, count);
        return EditResult.ok(filePath, count);
    }

    /**
     * 删除文件或目录.
     *
     * @param workspaceId 工作空间 ID
     * @param path 文件或目录路径
     * @return 删除结果
     */
    public WriteResult delete(String workspaceId, String path) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(path);

        sandbox.deleteFile(normalizedPath);
        log.info("从工作空间 {} 删除 {}", workspaceId, path);
        return WriteResult.ok(path);
    }

    /**
     * 移动文件或目录.
     *
     * <p>也可用于重命名文件。
     *
     * @param workspaceId 工作空间 ID
     * @param fromPath 源路径
     * @param toPath 目标路径
     * @return 移动结果
     */
    public WriteResult move(String workspaceId, String fromPath, String toPath) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedFrom = normalizePath(fromPath);
        String normalizedTo = normalizePath(toPath);

        // 读取源文件
        String content = sandbox.readFile(normalizedFrom);
        if (content == null) {
            return WriteResult.fail("源文件不存在: " + fromPath);
        }

        // 写入目标位置并删除源文件
        sandbox.writeFile(normalizedTo, content);
        sandbox.deleteFile(normalizedFrom);
        log.info("在工作空间 {} 中将 {} 移动到 {}", workspaceId, fromPath, toPath);
        return WriteResult.ok(toPath);
    }

    /**
     * 搜索文本模式（grep）.
     *
     * <p>在文件中搜索指定的文本模式，返回所有匹配的行。
     *
     * @param workspaceId 工作空间 ID
     * @param pattern 要搜索的文本模式
     * @param path 搜索的起始路径（可选）
     * @param glob 文件名匹配模式（可选，如 *.java）
     * @return 搜索结果列表
     */
    public GrepResult grep(String workspaceId, String pattern, String path, String glob) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);

        List<GrepMatch> matches = new java.util.ArrayList<>();
        String searchPath = path != null ? normalizePath(path) : "/workspace";

        // 遍历文件系统搜索
        sandbox.getFileSystem().keySet().stream()
                .filter(p -> !p.contains(".workspace_id"))  // 跳过元数据文件
                .filter(p -> glob == null || p.endsWith(glob.replace("*", "")))
                .forEach(filePath -> {
                    String content = sandbox.readFile(filePath);
                    if (content != null) {
                        String[] lines = content.split("\n");
                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].contains(pattern)) {
                                // 添加匹配结果：文件路径、行号（从 1 开始）、匹配行内容
                                matches.add(new GrepMatch(filePath, i + 1, lines[i]));
                            }
                        }
                    }
                });

        return GrepResult.success(matches);
    }

    /**
     * 查找匹配模式的文件（glob）.
     *
     * <p>使用 glob 模式匹配文件名，返回所有匹配的文件。
     *
     * @param workspaceId 工作空间 ID
     * @param pattern glob 模式（如 *.java, test-*.xml）
     * @param path 搜索的起始路径（可选）
     * @return 匹配的文件列表
     */
    public GlobResult glob(String workspaceId, String pattern, String path) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String searchPath = path != null ? normalizePath(path) : "/workspace";

        // 将 glob 模式转换为正则表达式
        String globPattern = pattern.replace("*", ".*").replace("?", ".");

        List<FileInfo> files = sandbox.getFileSystem().keySet().stream()
                .filter(p -> p.matches(".*" + globPattern + ".*"))
                .filter(p -> !p.contains(".workspace_id"))  // 跳过元数据文件
                .map(p -> FileInfo.ofFile(p, sandbox.getFileSystem().get(p).length, ""))
                .toList();

        return GlobResult.success(files);
    }

    /**
     * 上传多个文件.
     *
     * <p>批量上传文件到工作空间。每个文件独立处理，
     * 单个文件失败不影响其他文件。
     *
     * @param workspaceId 工作空间 ID
     * @param files 文件列表（文件名 -> 内容 的映射）
     * @return 每个文件的上传结果
     */
    public List<FileUploadResponse> uploadFiles(String workspaceId,
                                                  List<Map.Entry<String, byte[]>> files) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);

        return files.stream().map(entry -> {
            try {
                // 将字节数组转换为字符串并写入
                sandbox.writeFile(normalizePath(entry.getKey()),
                        new String(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
                return FileUploadResponse.success(entry.getKey());
            } catch (Exception e) {
                log.error("上传文件 {} 失败: {}", entry.getKey(), e.getMessage());
                return FileUploadResponse.fail(entry.getKey(), e.getMessage());
            }
        }).toList();
    }

    /**
     * 下载多个文件.
     *
     * <p>批量下载工作空间中的文件。
     *
     * @param workspaceId 工作空间 ID
     * @param paths 要下载的文件路径列表
     * @return 每个文件的下载结果（包含文件内容）
     */
    public List<FileDownloadResponse> downloadFiles(String workspaceId, List<String> paths) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);

        return paths.stream().map(path -> {
            String normalizedPath = normalizePath(path);
            byte[] content = sandbox.getFileSystem().get(normalizedPath);
            if (content != null) {
                return FileDownloadResponse.success(normalizedPath, content);
            }
            return FileDownloadResponse.fail(normalizedPath, "文件不存在");
        }).toList();
    }

    /**
     * 检查文件是否存在.
     *
     * @param workspaceId 工作空间 ID
     * @param path 文件路径
     * @return true 如果文件存在
     */
    public boolean exists(String workspaceId, String path) {
        InMemorySandbox sandbox = filesystems.get(workspaceId);
        if (sandbox == null) {
            return false;
        }
        return sandbox.fileExists(normalizePath(path));
    }

    /**
     * 获取工作空间统计信息.
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间统计信息
     */
    public WorkspaceStats getStats(String workspaceId) {
        InMemorySandbox sandbox = filesystems.get(workspaceId);
        if (sandbox == null) {
            return new WorkspaceStats(workspaceId, 0, 0, 0);
        }

        // 计算文件数量和总大小
        int fileCount = sandbox.getFileSystem().size();
        int totalSize = sandbox.getFileSystem().values().stream()
                .mapToInt(b -> b.length)
                .sum();

        return new WorkspaceStats(workspaceId, fileCount, totalSize,
                sandbox.isRunning() ? 1 : 0);
    }

    /**
     * 清空工作空间.
     *
     * <p>删除工作空间中的所有文件并释放资源。
     *
     * @param workspaceId 要清空的工作空间 ID
     */
    public void clearWorkspace(String workspaceId) {
        InMemorySandbox sandbox = filesystems.remove(workspaceId);
        if (sandbox != null) {
            try {
                sandbox.close();
            } catch (Exception e) {
                log.error("关闭工作空间 {} 时出错: {}", workspaceId, e.getMessage());
            }
        }
        log.info("已清空工作空间: {}", workspaceId);
    }

    /**
     * 规范化路径.
     *
     * <p>处理逻辑：
     * <ul>
     *   <li>null 路径转换为 /workspace</li>
     *   <li>相对路径添加 /workspace/ 前缀</li>
     *   <li>移除尾部斜杠</li>
     * </ul>
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/workspace";
        }
        // 相对路径转绝对路径
        if (!path.startsWith("/")) {
            path = "/workspace/" + path;
        }
        if (path.equals("/workspace")) {
            return "/workspace";
        }
        // 移除尾部斜杠
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 工作空间统计记录.
     *
     * <p>使用 Java Record 定义不可变的统计数据结构。
     *
     * @param workspaceId 工作空间 ID
     * @param fileCount 文件数量
     * @param totalSizeBytes 总大小（字节）
     * @param status 状态（0=不存在, 1=运行中）
     */
    public record WorkspaceStats(
            String workspaceId,
            int fileCount,
            int totalSizeBytes,
            int status
    ) {}
}