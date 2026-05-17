package io.agentscope.tutorial.chapter15.service;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.*;
import io.agentscope.tutorial.chapter15.sandbox.InMemorySandbox;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Workspace management service that provides filesystem operations.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Per-workspace filesystem isolation</li>
 *   <li>File operations (read, write, edit, delete, move)</li>
 *   <li>Directory operations (ls, glob, grep)</li>
 *   <li>File upload/download</li>
 * </ul>
 *
 * <p>Each workspace has its own isolated in-memory filesystem backed by
 * a {@link InMemorySandbox}.
 */
@Slf4j
@Service
@RequiredArgs构造函数
public class WorkspaceService {

    private final CodeExecutionService codeExecutionService;

    // Map of workspace ID to filesystem instance
    private final Map<String, InMemorySandbox> filesystems = new ConcurrentHashMap<>();

    /**
     * Gets the filesystem for a workspace.
     */
    public InMemorySandbox getFilesystem(String workspaceId) {
        return filesystems.computeIfAbsent(workspaceId, this::createFilesystem);
    }

    /**
     * Creates a new filesystem for a workspace.
     */
    private InMemorySandbox createFilesystem(String workspaceId) {
        InMemorySandbox sandbox = new InMemorySandbox();
        try {
            sandbox.start();
            // Initialize default workspace files
            sandbox.writeFile("/workspace/README.md", "# Workspace " + workspaceId + "\n\nWelcome!");
            sandbox.writeFile("/workspace/.workspace_id", workspaceId);
            log.info("Created filesystem for workspace: {}", workspaceId);
        } catch (Exception e) {
            log.error("Failed to create filesystem for workspace {}: {}", workspaceId, e.getMessage());
        }
        return sandbox;
    }

    /**
     * Lists directory contents.
     */
    public LsResult ls(String workspaceId, String path) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(path);

        List<FileInfo> entries = sandbox.getFileSystem().keySet().stream()
                .filter(p -> p.startsWith(normalizedPath))
                .map(p -> {
                    String relativePath = p.substring(normalizedPath.length());
                    if (relativePath.isEmpty() || relativePath.equals("/")) {
                        return null;
                    }
                    // Get the first level of the path
                    int slashIdx = relativePath.indexOf('/', 1);
                    if (slashIdx == -1) {
                        return FileInfo.ofFile(p, sandbox.getFileSystem().get(p).length, "");
                    } else {
                        return FileInfo.ofDir(p.substring(0, slashIdx), "");
                    }
                })
                .filter(e -> e != null)
                .distinct()
                .sorted((a, b) -> {
                    // Dirs first, then files
                    if (a.isDirectory() != b.isDirectory()) {
                        return a.isDirectory() ? -1 : 1;
                    }
                    return a.path().compareTo(b.path());
                })
                .toList();

        return LsResult.success(entries);
    }

    /**
     * Reads file content.
     */
    public ReadResult read(String workspaceId, String filePath, int offset, int limit) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        String content = sandbox.readFile(normalizedPath);
        if (content == null) {
            return ReadResult.fail("File not found: " + filePath);
        }

        String[] lines = content.split("\n");
        int start = Math.min(offset, lines.length);
        int end = Math.min(offset + limit, lines.length);
        String result = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));

        return ReadResult.success(new FileData(result, "utf-8"));
    }

    /**
     * Writes content to a file.
     */
    public WriteResult write(String workspaceId, String filePath, String content) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        if (sandbox.fileExists(normalizedPath)) {
            return WriteResult.fail("File already exists: " + filePath);
        }

        sandbox.writeFile(normalizedPath, content);
        log.info("Wrote file {} in workspace {}", filePath, workspaceId);
        return WriteResult.ok(filePath);
    }

    /**
     * Overwrites content to a file (creates if not exists).
     */
    public WriteResult overwrite(String workspaceId, String filePath, String content) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        sandbox.writeFile(normalizedPath, content);
        log.info("Overwrote file {} in workspace {}", filePath, workspaceId);
        return WriteResult.ok(filePath);
    }

    /**
     * Edits file content with string replacement.
     */
    public EditResult edit(String workspaceId, String filePath, String oldString,
                           String newString, boolean replaceAll) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(filePath);

        String content = sandbox.readFile(normalizedPath);
        if (content == null) {
            return EditResult.fail("File not found: " + filePath);
        }

        int count = content.split(oldString, -1).length - 1;
        if (count == 0) {
            return EditResult.fail("String not found: " + oldString);
        }
        if (count > 1 && !replaceAll) {
            return EditResult.fail("Multiple occurrences. Use replaceAll=true to replace all.");
        }

        String newContent = replaceAll
                ? content.replace(oldString, newString)
                : content.replaceFirst(oldString, newString);

        sandbox.writeFile(normalizedPath, newContent);
        log.info("Edited file {} in workspace {}: {} occurrences", filePath, workspaceId, count);
        return EditResult.ok(filePath, count);
    }

    /**
     * Deletes a file or directory.
     */
    public WriteResult delete(String workspaceId, String path) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedPath = normalizePath(path);

        sandbox.deleteFile(normalizedPath);
        log.info("Deleted {} in workspace {}", path, workspaceId);
        return WriteResult.ok(path);
    }

    /**
     * Moves a file or directory.
     */
    public WriteResult move(String workspaceId, String fromPath, String toPath) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String normalizedFrom = normalizePath(fromPath);
        String normalizedTo = normalizePath(toPath);

        String content = sandbox.readFile(normalizedFrom);
        if (content == null) {
            return WriteResult.fail("Source not found: " + fromPath);
        }

        sandbox.writeFile(normalizedTo, content);
        sandbox.deleteFile(normalizedFrom);
        log.info("Moved {} to {} in workspace {}", fromPath, toPath, workspaceId);
        return WriteResult.ok(toPath);
    }

    /**
     * Searches for a pattern in files.
     */
    public GrepResult grep(String workspaceId, String pattern, String path, String glob) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);

        List<GrepMatch> matches = new java.util.ArrayList<>();
        String searchPath = path != null ? normalizePath(path) : "/workspace";

        sandbox.getFileSystem().keySet().stream()
                .filter(p -> !p.contains(".workspace_id")) // Skip metadata files
                .filter(p -> glob == null || p.endsWith(glob.replace("*", "")))
                .forEach(filePath -> {
                    String content = sandbox.readFile(filePath);
                    if (content != null) {
                        String[] lines = content.split("\n");
                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].contains(pattern)) {
                                matches.add(new GrepMatch(filePath, i + 1, lines[i]));
                            }
                        }
                    }
                });

        return GrepResult.success(matches);
    }

    /**
     * Finds files matching a glob pattern.
     */
    public GlobResult glob(String workspaceId, String pattern, String path) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);
        String searchPath = path != null ? normalizePath(path) : "/workspace";

        String globPattern = pattern.replace("*", ".*").replace("?", ".");

        List<FileInfo> files = sandbox.getFileSystem().keySet().stream()
                .filter(p -> p.matches(".*" + globPattern + ".*"))
                .filter(p -> !p.contains(".workspace_id"))
                .map(p -> FileInfo.ofFile(p, sandbox.getFileSystem().get(p).length, ""))
                .toList();

        return GlobResult.success(files);
    }

    /**
     * Uploads multiple files.
     */
    public List<FileUploadResponse> uploadFiles(String workspaceId,
                                                  List<Map.Entry<String, byte[]>> files) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);

        return files.stream().map(entry -> {
            try {
                sandbox.writeFile(normalizePath(entry.getKey()),
                        new String(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
                return FileUploadResponse.success(entry.getKey());
            } catch (Exception e) {
                log.error("Failed to upload file {}: {}", entry.getKey(), e.getMessage());
                return FileUploadResponse.fail(entry.getKey(), e.getMessage());
            }
        }).toList();
    }

    /**
     * Downloads multiple files.
     */
    public List<FileDownloadResponse> downloadFiles(String workspaceId, List<String> paths) {
        InMemorySandbox sandbox = getFilesystem(workspaceId);

        return paths.stream().map(path -> {
            String normalizedPath = normalizePath(path);
            byte[] content = sandbox.getFileSystem().get(normalizedPath);
            if (content != null) {
                return FileDownloadResponse.success(normalizedPath, content);
            }
            return FileDownloadResponse.fail(normalizedPath, "File not found");
        }).toList();
    }

    /**
     * Checks if a file exists.
     */
    public boolean exists(String workspaceId, String path) {
        InMemorySandbox sandbox = filesystems.get(workspaceId);
        if (sandbox == null) {
            return false;
        }
        return sandbox.fileExists(normalizePath(path));
    }

    /**
     * Gets workspace statistics.
     */
    public WorkspaceStats getStats(String workspaceId) {
        InMemorySandbox sandbox = filesystems.get(workspaceId);
        if (sandbox == null) {
            return new WorkspaceStats(workspaceId, 0, 0, 0);
        }

        int fileCount = sandbox.getFileSystem().size();
        int totalSize = sandbox.getFileSystem().values().stream()
                .mapToInt(b -> b.length)
                .sum();

        return new WorkspaceStats(workspaceId, fileCount, totalSize,
                sandbox.isRunning() ? 1 : 0);
    }

    /**
     * Clears all files in a workspace.
     */
    public void clearWorkspace(String workspaceId) {
        InMemorySandbox sandbox = filesystems.remove(workspaceId);
        if (sandbox != null) {
            try {
                sandbox.close();
            } catch (Exception e) {
                log.error("Error closing workspace {}: {}", workspaceId, e.getMessage());
            }
        }
        log.info("Cleared workspace: {}", workspaceId);
    }

    /**
     * Normalizes a path to the workspace format.
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/workspace";
        }
        if (!path.startsWith("/")) {
            path = "/workspace/" + path;
        }
        if (path.equals("/workspace")) {
            return "/workspace";
        }
        // Remove trailing slash
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Workspace statistics record.
     */
    public record WorkspaceStats(
            String workspaceId,
            int fileCount,
            int totalSizeBytes,
            int status  // 0=not exists, 1=running
    ) {}
}