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
 * REST controller for workspace filesystem operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/workspace/{id}/ls - List directory contents</li>
 *   <li>GET /api/workspace/{id}/read - Read file content</li>
 *   <li>POST /api/workspace/{id}/write - Write file content</li>
 *   <li>PUT /api/workspace/{id}/edit - Edit file content</li>
 *   <li>POST /api/workspace/{id}/upload - Upload files</li>
 *   <li>POST /api/workspace/{id}/download - Download files</li>
 *   <li>GET /api/workspace/{id}/exists - Check file existence</li>
 *   <li>GET /api/workspace/{id}/stats - Get workspace statistics</li>
 *   <li>DELETE /api/workspace/{id}/clear - Clear workspace</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * Lists directory contents.
     *
     * @param workspaceId workspace identifier
     * @param path directory path (default: /workspace)
     */
    @GetMapping("/{workspaceId}/ls")
    public ResponseEntity<LsResult> listDirectory(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "/workspace") String path) {

        log.debug("Listing directory {} in workspace {}", path, workspaceId);
        LsResult result = workspaceService.ls(workspaceId, path);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Reads file content.
     *
     * @param workspaceId workspace identifier
     * @param filePath file path
     * @param offset line offset (default: 0)
     * @param limit max lines to read (default: 1000)
     */
    @GetMapping("/{workspaceId}/read")
    public ResponseEntity<ReadResult> readFile(
            @PathVariable String workspaceId,
            @RequestParam String filePath,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "1000") int limit) {

        log.debug("Reading file {} in workspace {}", filePath, workspaceId);
        ReadResult result = workspaceService.read(workspaceId, filePath, offset, limit);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Writes content to a new file.
     *
     * @param workspaceId workspace identifier
     * @param request containing filePath and content
     */
    @PostMapping("/{workspaceId}/write")
    public ResponseEntity<WriteResult> writeFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {

        String filePath = request.get("filePath");
        String content = request.get("content");

        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(
                    WriteResult.fail("filePath is required")
            );
        }

        log.info("Writing file {} in workspace {}", filePath, workspaceId);
        WriteResult result = workspaceService.write(workspaceId, filePath, content);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Overwrites file content (creates if not exists).
     */
    @PutMapping("/{workspaceId}/write")
    public ResponseEntity<WriteResult> overwriteFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {

        String filePath = request.get("filePath");
        String content = request.get("content");

        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(
                    WriteResult.fail("filePath is required")
            );
        }

        log.info("Overwriting file {} in workspace {}", filePath, workspaceId);
        WriteResult result = workspaceService.overwrite(workspaceId, filePath, content);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Edits file content with string replacement.
     */
    @PutMapping("/{workspaceId}/edit")
    public ResponseEntity<EditResult> editFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, Object> request) {

        String filePath = (String) request.get("filePath");
        String oldString = (String) request.get("oldString");
        String newString = (String) request.get("newString");
        Boolean replaceAll = (Boolean) request.getOrDefault("replaceAll", false);

        if (filePath == null || oldString == null || newString == null) {
            return ResponseEntity.badRequest().body(
                    EditResult.fail("filePath, oldString, and newString are required")
            );
        }

        log.info("Editing file {} in workspace {}: replacing '{}' with '{}'",
                filePath, workspaceId, oldString, newString);
        EditResult result = workspaceService.edit(workspaceId, filePath, oldString, newString, replaceAll);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Deletes a file.
     */
    @DeleteMapping("/{workspaceId}/file")
    public ResponseEntity<WriteResult> deleteFile(
            @PathVariable String workspaceId,
            @RequestParam String path) {

        log.info("Deleting {} from workspace {}", path, workspaceId);
        WriteResult result = workspaceService.delete(workspaceId, path);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Moves a file.
     */
    @PostMapping("/{workspaceId}/move")
    public ResponseEntity<WriteResult> moveFile(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {

        String fromPath = request.get("from");
        String toPath = request.get("to");

        if (fromPath == null || toPath == null) {
            return ResponseEntity.badRequest().body(
                    WriteResult.fail("from and to paths are required")
            );
        }

        log.info("Moving {} to {} in workspace {}", fromPath, toPath, workspaceId);
        WriteResult result = workspaceService.move(workspaceId, fromPath, toPath);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Searches for a pattern in files.
     */
    @GetMapping("/{workspaceId}/grep")
    public ResponseEntity<GrepResult> grep(
            @PathVariable String workspaceId,
            @RequestParam String pattern,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String glob) {

        log.debug("Searching for '{}' in workspace {}", pattern, workspaceId);
        GrepResult result = workspaceService.grep(workspaceId, pattern, path, glob);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Finds files matching a glob pattern.
     */
    @GetMapping("/{workspaceId}/glob")
    public ResponseEntity<GlobResult> glob(
            @PathVariable String workspaceId,
            @RequestParam String pattern,
            @RequestParam(required = false) String path) {

        log.debug("Globbing for '{}' in workspace {}", pattern, workspaceId);
        GlobResult result = workspaceService.glob(workspaceId, pattern, path);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Uploads multiple files.
     */
    @PostMapping(value = "/{workspaceId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadFiles(
            @PathVariable String workspaceId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Uploading {} files to workspace {}", files.size(), workspaceId);

        List<Map.Entry<String, byte[]>> fileEntries = files.stream()
                .map(f -> {
                    try {
                        return Map.entry(f.getOriginalFilename(), f.getBytes());
                    } catch (Exception e) {
                        log.error("Failed to read file {}: {}", f.getOriginalFilename(), e.getMessage());
                        return null;
                    }
                })
                .filter(e -> e != null)
                .toList();

        List<FileUploadResponse> results = workspaceService.uploadFiles(workspaceId, fileEntries);
        return ResponseEntity.ok(results);
    }

    /**
     * Downloads multiple files.
     */
    @PostMapping("/{workspaceId}/download")
    public ResponseEntity<List<FileDownloadResponse>> downloadFiles(
            @PathVariable String workspaceId,
            @RequestBody Map<String, List<String>> request) {

        List<String> paths = request.get("paths");
        if (paths == null || paths.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Downloading {} files from workspace {}", paths.size(), workspaceId);
        List<FileDownloadResponse> results = workspaceService.downloadFiles(workspaceId, paths);
        return ResponseEntity.ok(results);
    }

    /**
     * Checks if a file exists.
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
     * Gets workspace statistics.
     */
    @GetMapping("/{workspaceId}/stats")
    public ResponseEntity<WorkspaceService.WorkspaceStats> getStats(
            @PathVariable String workspaceId) {

        WorkspaceService.WorkspaceStats stats = workspaceService.getStats(workspaceId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Clears all files in a workspace.
     */
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> clearWorkspace(@PathVariable String workspaceId) {
        log.info("Clearing workspace: {}", workspaceId);
        workspaceService.clearWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }
}