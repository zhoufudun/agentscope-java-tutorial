package io.agentscope.tutorial.chapter11.controller;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.tutorial.chapter11.entity.SkillDefinition;
import io.agentscope.tutorial.chapter11.service.SkillService;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 技能管理控制器
 *
 * <p>提供技能的 RESTful API，包括：
 * <ul>
 *   <li>技能 CRUD 操作</li>
 *   <li>技能激活/停用</li>
 *   <li>技能搜索和过滤</li>
 *   <li>技能统计信息</li>
 * </ul>
 *
 * <p>API 端点：
 * <ul>
 *   <li>GET /skills - 获取所有技能</li>
 *   <li>GET /skills/{id} - 获取技能详情</li>
 *   <li>POST /skills - 创建技能</li>
 *   <li>PUT /skills/{id} - 更新技能</li>
 *   <li>DELETE /skills/{id} - 删除技能</li>
 *   <li>POST /skills/{id}/activate - 激活技能</li>
 *   <li>POST /skills/{id}/deactivate - 停用技能</li>
 *   <li>GET /skills/search - 搜索技能</li>
 *   <li>GET /skills/stats - 获取统计信息</li>
 *   <li>GET /skills/prompt - 获取技能提示词</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/skills")
@RequiredArgs构造函数
@Slf4j
public class SkillController {

    /**
     * 技能服务
     */
    private final SkillService skillService;

    /**
     * 获取所有技能
     *
     * @param activeOnly 只返回已激活的技能
     * @return 技能列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillDTO>>> getAllSkills(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        List<SkillDefinition> skills = activeOnly
                ? skillService.getActiveSkills()
                : skillService.getAllSkills();

        List<SkillDTO> dtos = skills.stream()
                .map(SkillDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * 获取技能详情
     *
     * @param id 技能数据库 ID
     * @return 技能详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillDetailDTO>> getSkillById(@PathVariable Long id) {
        return skillService.getSkillById(id)
                .map(skill -> ResponseEntity.ok(ApiResponse.success(SkillDetailDTO.fromEntity(skill))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据 skillId 获取技能
     *
     * @param skillId 技能唯一标识
     * @return 技能详情
     */
    @GetMapping("/by-skill-id/{skillId}")
    public ResponseEntity<ApiResponse<SkillDetailDTO>> getSkillBySkillId(@PathVariable String skillId) {
        return skillService.getSkillBySkillId(skillId)
                .map(skill -> ResponseEntity.ok(ApiResponse.success(SkillDetailDTO.fromEntity(skill))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建技能
     *
     * @param request 创建请求
     * @return 创建的技能
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SkillDetailDTO>> createSkill(@RequestBody CreateSkillRequest request) {
        log.info("Creating skill: {}", request.getName());

        try {
            SkillDefinition skill = skillService.registerSkillFromContent(
                    request.getSkillContent(),
                    request.getResources(),
                    request.getSource() != null ? request.getSource() : "custom"
            );

            return ResponseEntity.ok(ApiResponse.success(SkillDetailDTO.fromEntity(skill)));
        } catch (Exception e) {
            log.error("Failed to create skill: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create skill: " + e.getMessage()));
        }
    }

    /**
     * 更新技能
     *
     * @param id      技能数据库 ID
     * @param request 更新请求
     * @return 更新后的技能
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillDetailDTO>> updateSkill(
            @PathVariable Long id,
            @RequestBody UpdateSkillRequest request) {

        return skillService.getSkillById(id)
                .map(existing -> {
                    try {
                        SkillDefinition updated = skillService.updateSkill(
                                existing.getSkillId(),
                                request.getSkillContent(),
                                request.getResources()
                        );
                        return ResponseEntity.ok(ApiResponse.success(SkillDetailDTO.fromEntity(updated)));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                .<ApiResponse<SkillDetailDTO>>body(ApiResponse.error(e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除技能
     *
     * @param skillId 技能 ID
     * @return 删除结果
     */
    @DeleteMapping("/{skillId}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable String skillId) {
        try {
            skillService.deleteSkill(skillId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 激活技能
     *
     * @param skillId 技能 ID
     * @return 激活后的技能
     */
    @PostMapping("/{skillId}/activate")
    public ResponseEntity<ApiResponse<SkillDetailDTO>> activateSkill(@PathVariable String skillId) {
        try {
            SkillDefinition skill = skillService.activateSkill(skillId);
            return ResponseEntity.ok(ApiResponse.success(SkillDetailDTO.fromEntity(skill)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 停用技能
     *
     * @param skillId 技能 ID
     * @return 停用后的技能
     */
    @PostMapping("/{skillId}/deactivate")
    public ResponseEntity<ApiResponse<SkillDetailDTO>> deactivateSkill(@PathVariable String skillId) {
        try {
            SkillDefinition skill = skillService.deactivateSkill(skillId);
            return ResponseEntity.ok(ApiResponse.success(SkillDetailDTO.fromEntity(skill)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 搜索技能
     *
     * @param keyword 搜索关键字
     * @param tag     标签过滤
     * @param source  来源过滤
     * @return 匹配的技能列表
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SkillDTO>>> searchSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String source) {

        List<SkillDefinition> skills;
        if (keyword != null && !keyword.isBlank()) {
            skills = skillService.searchSkills(keyword);
        } else if (tag != null && !tag.isBlank()) {
            skills = skillService.getSkillsByTag(tag);
        } else if (source != null && !source.isBlank()) {
            skills = skillService.getSkillsBySource(source);
        } else {
            skills = skillService.getAllSkills();
        }

        List<SkillDTO> dtos = skills.stream()
                .map(SkillDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * 获取技能统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SkillStatsDTO>> getStats() {
        SkillService.SkillStats stats = skillService.getStats();
        return ResponseEntity.ok(ApiResponse.success(SkillStatsDTO.fromStats(stats)));
    }

    /**
     * 获取技能提示词
     *
     * <p>此提示词应包含在 Agent 的系统提示词中，
     * 使 Agent 知道有哪些技能可用。
     *
     * @return 技能提示词
     */
    @GetMapping("/prompt")
    public ResponseEntity<ApiResponse<String>> getSkillPrompt() {
        String prompt = skillService.getSkillPrompt();
        return ResponseEntity.ok(ApiResponse.success(prompt));
    }

    /**
     * 批量激活技能
     *
     * @param skillIds 技能 ID 列表
     * @return 激活结果
     */
    @PostMapping("/batch-activate")
    public ResponseEntity<ApiResponse<Void>> batchActivateSkills(@RequestBody List<String> skillIds) {
        try {
            skillService.activateSkills(skillIds);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量停用技能
     *
     * @param skillIds 技能 ID 列表
     * @return 停用结果
     */
    @PostMapping("/batch-deactivate")
    public ResponseEntity<ApiResponse<Void>> batchDeactivateSkills(@RequestBody List<String> skillIds) {
        try {
            skillService.deactivateSkills(skillIds);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 同步 SkillBox 状态
     *
     * <p>根据数据库中的激活状态，同步 SkillBox 的工具组状态。
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> syncSkillBox() {
        skillService.syncToolGroupStates();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== DTO 类 ====================

    /**
     * API 响应包装
     */
    @lombok.Data
    @lombok.Builder
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String error;
        private long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .error(message)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * 技能列表 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SkillDTO {
        private Long id;
        private String skillId;
        private String name;
        private String description;
        private String version;
        private String author;
        private String[] tags;
        private boolean active;
        private String source;

        public static SkillDTO fromEntity(SkillDefinition entity) {
            return SkillDTO.builder()
                    .id(entity.getId())
                    .skillId(entity.getSkillId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .version(entity.getVersion())
                    .author(entity.getAuthor())
                    .tags(entity.getTagsArray())
                    .active(entity.isActivated())
                    .source(entity.getSource())
                    .build();
        }
    }

    /**
     * 技能详情 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SkillDetailDTO {
        private Long id;
        private String skillId;
        private String name;
        private String description;
        private String version;
        private String author;
        private String[] tags;
        private String skillContent;
        private String source;
        private boolean active;
        private String createdAt;
        private String updatedAt;
        private Map<String, String> resources;

        public static SkillDetailDTO fromEntity(SkillDefinition entity) {
            return SkillDetailDTO.builder()
                    .id(entity.getId())
                    .skillId(entity.getSkillId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .version(entity.getVersion())
                    .author(entity.getAuthor())
                    .tags(entity.getTagsArray())
                    .skillContent(entity.getSkillContent())
                    .source(entity.getSource())
                    .active(entity.isActivated())
                    .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                    .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                    .resources(entity.getResources())
                    .build();
        }
    }

    /**
     * 技能统计 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SkillStatsDTO {
        private long totalSkills;
        private long activeSkills;
        private long inactiveSkills;
        private java.util.Set<String> registeredSkillIds;

        public static SkillStatsDTO fromStats(SkillService.SkillStats stats) {
            return SkillStatsDTO.builder()
                    .totalSkills(stats.getTotalSkills())
                    .activeSkills(stats.getActiveSkills())
                    .inactiveSkills(stats.getInactiveSkills())
                    .registeredSkillIds(stats.getTotalSkillIds())
                    .build();
        }
    }

    /**
     * 创建技能请求
     */
    @lombok.Data
    public static class CreateSkillRequest {
        private String name;
        private String description;
        private String skillContent;
        private Map<String, String> resources;
        private String source;
    }

    /**
     * 更新技能请求
     */
    @lombok.Data
    public static class UpdateSkillRequest {
        private String skillContent;
        private Map<String, String> resources;
    }
}