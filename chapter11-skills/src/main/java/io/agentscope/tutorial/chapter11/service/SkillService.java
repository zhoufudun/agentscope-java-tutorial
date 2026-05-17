package io.agentscope.tutorial.chapter11.service;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.SkillUtil;
import io.agentscope.tutorial.chapter11.entity.SkillDefinition;
import io.agentscope.tutorial.chapter11.repository.SkillRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能管理服务
 *
 * <p>提供技能的业务逻辑处理，包括：
 * <ul>
 *   <li>技能注册和更新</li>
 *   <li>技能激活和停用</li>
 *   <li>技能版本管理</li>
 *   <li>SkillBox 集成</li>
 *   <li>技能搜索和过滤</li>
 *   <li>技能执行上下文管理</li>
 * </ul>
 *
 * <p>核心功能：
 * <ul>
 *   <li>技能注册：将 AgentSkill 持久化到数据库并注册到 SkillBox</li>
 *   <li>技能激活：控制技能在 Agent 执行时的可用性</li>
 *   <li>技能版本：维护技能的历史版本，支持回滚</li>
 *   <li>技能执行：记录技能使用和执行结果</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 注册技能
 * AgentSkill skill = new AgentSkill("my-skill", "描述", "内容", null);
 * skillService.registerSkill(skill);
 *
 * // 激活技能
 * skillService.activateSkill("my-skill_custom");
 *
 * // 获取技能提示词
 * String prompt = skillService.getSkillPrompt();
 *
 * // 搜索技能
 * List<SkillDefinition> results = skillService.searchSkills("data");
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @see AgentSkill
 * @see SkillBox
 * @see SkillDefinition
 */
@Service
@RequiredArgs构造函数
@Slf4j
public class SkillService {

    /**
     * 技能仓库
     */
    private final SkillRepository skillRepository;

    /**
     * SkillBox（延迟注入，避免循环依赖）
     */
    private SkillBox skillBox;

    /**
     * 设置 SkillBox
     *
     * @param skillBox SkillBox 实例
     */
    @jakarta.annotation.Resource
    public void setSkillBox(SkillBox skillBox) {
        this.skillBox = skillBox;
    }

    // ==================== 技能注册管理 ====================

    /**
     * 注册技能
     *
     * <p>将 AgentSkill 注册到数据库和 SkillBox。
     * 如果技能已存在，则更新；如果不存在，则创建。
     *
     * <p><b>版本管理：</b>
     * <ul>
     *   <li>首次注册：创建版本 1.0.0</li>
     *   <li>更新注册：创建新版本快照</li>
     * </ul>
     *
     * @param agentSkill AgentSkill 实例
     * @return 注册后的 SkillDefinition
     */
    @Transactional
    public SkillDefinition registerSkill(AgentSkill agentSkill) {
        if (agentSkill == null) {
            throw new IllegalArgumentException("AgentSkill cannot be null");
        }

        String skillId = agentSkill.getSkillId();
        log.info("Registering skill: {}", skillId);

        // 查询是否已存在
        Optional<SkillDefinition> existing = skillRepository.findBySkillId(skillId);
        SkillDefinition skillDef;

        if (existing.isPresent()) {
            // 更新现有技能
            skillDef = existing.get();
            skillDef.setDescription(agentSkill.getDescription());
            skillDef.setSkillContent(agentSkill.getSkillContent());
            skillDef.setTags(extractTags(agentSkill.getMetadata()));
            skillDef.setVersion(incrementVersion(skillDef.getVersion()));
            skillDef.setResources(agentSkill.getResources());
            log.info("Updating existing skill: {}", skillId);
        } else {
            // 创建新技能
            skillDef = SkillDefinition.fromAgentSkill(agentSkill);
            skillDef.setResources(agentSkill.getResources());
            log.info("Creating new skill: {}", skillId);
        }

        // 保存到数据库
        skillDef = skillRepository.save(skillDef);

        // 注册到 SkillBox
        if (skillBox != null) {
            AgentSkill registeredSkill = skillDef.toAgentSkill(skillDef.getResources());
            skillBox.registerSkill(registeredSkill);
            log.info("Skill registered to SkillBox: {}", skillId);
        }

        return skillDef;
    }

    /**
     * 批量注册技能
     *
     * @param skills AgentSkill 列表
     * @return 注册成功的技能定义列表
     */
    @Transactional
    public List<SkillDefinition> registerSkills(List<AgentSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        return skills.stream()
                .map(this::registerSkill)
                .collect(Collectors.toList());
    }

    /**
     * 从 SKILL.md 内容注册技能
     *
     * @param skillMdContent SKILL.md 内容（包含 YAML frontmatter）
     * @param resources      资源文件（路径 -> 内容）
     * @param source         来源标识
     * @return 注册后的 SkillDefinition
     */
    @Transactional
    public SkillDefinition registerSkillFromContent(
            String skillMdContent,
            Map<String, String> resources,
            String source) {
        AgentSkill skill = SkillUtil.createFrom(skillMdContent, resources, source);
        return registerSkill(skill);
    }

    // ==================== 技能查询 ====================

    /**
     * 获取所有技能
     *
     * @return 技能定义列表
     */
    public List<SkillDefinition> getAllSkills() {
        return skillRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * 获取已激活的技能
     *
     * @return 已激活的技能列表
     */
    public List<SkillDefinition> getActiveSkills() {
        return skillRepository.findByIsActiveTrue();
    }

    /**
     * 根据 ID 获取技能
     *
     * @param id 技能数据库 ID
     * @return 技能定义（可选）
     */
    public Optional<SkillDefinition> getSkillById(Long id) {
        return skillRepository.findById(id);
    }

    /**
     * 根据 skillId 获取技能
     *
     * @param skillId 技能唯一标识 (name_source)
     * @return 技能定义（可选）
     */
    public Optional<SkillDefinition> getSkillBySkillId(String skillId) {
        return skillRepository.findBySkillId(skillId);
    }

    /**
     * 搜索技能
     *
     * @param keyword 搜索关键字（匹配名称、描述、标签）
     * @return 匹配的技能列表
     */
    public List<SkillDefinition> searchSkills(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllSkills();
        }
        return skillRepository.searchByKeyword(keyword.trim());
    }

    /**
     * 根据标签获取技能
     *
     * @param tag 标签
     * @return 匹配的技能列表
     */
    public List<SkillDefinition> getSkillsByTag(String tag) {
        return skillRepository.findByTag(tag);
    }

    /**
     * 根据来源获取技能
     *
     * @param source 来源
     * @return 技能列表
     */
    public List<SkillDefinition> getSkillsBySource(String source) {
        return skillRepository.findBySource(source);
    }

    // ==================== 技能激活管理 ====================

    /**
     * 激活技能
     *
     * <p>激活技能后，Agent 可以使用该技能的能力。
     * 激活状态会同步到 SkillBox，影响工具组的可用性。
     *
     * @param skillId 技能 ID
     * @return 激活后的技能定义
     */
    @Transactional
    public SkillDefinition activateSkill(String skillId) {
        SkillDefinition skill = skillRepository.findBySkillId(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));

        skill.setIsActive(true);
        skill = skillRepository.save(skill);

        // 同步到 SkillBox
        if (skillBox != null) {
            skillBox.setSkillActive(skillId, true);
            log.info("Skill activated in SkillBox: {}", skillId);
        }

        log.info("Skill activated: {}", skillId);
        return skill;
    }

    /**
     * 停用技能
     *
     * <p>停用技能后，Agent 无法使用该技能。
     *
     * @param skillId 技能 ID
     * @return 停用后的技能定义
     */
    @Transactional
    public SkillDefinition deactivateSkill(String skillId) {
        SkillDefinition skill = skillRepository.findBySkillId(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));

        skill.setIsActive(false);
        skill = skillRepository.save(skill);

        // 同步到 SkillBox
        if (skillBox != null) {
            skillBox.setSkillActive(skillId, false);
            log.info("Skill deactivated in SkillBox: {}", skillId);
        }

        log.info("Skill deactivated: {}", skillId);
        return skill;
    }

    /**
     * 批量激活技能
     *
     * @param skillIds 技能 ID 列表
     */
    @Transactional
    public void activateSkills(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        skillRepository.activateSkills(skillIds);

        // 同步到 SkillBox
        if (skillBox != null) {
            skillIds.forEach(id -> skillBox.setSkillActive(id, true));
        }

        log.info("Batch activated {} skills", skillIds.size());
    }

    /**
     * 批量停用技能
     *
     * @param skillIds 技能 ID 列表
     */
    @Transactional
    public void deactivateSkills(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        skillRepository.deactivateSkills(skillIds);

        // 同步到 SkillBox
        if (skillBox != null) {
            skillIds.forEach(id -> skillBox.setSkillActive(id, false));
        }

        log.info("Batch deactivated {} skills", skillIds.size());
    }

    /**
     * 获取技能激活状态
     *
     * @param skillId 技能 ID
     * @return 是否已激活
     */
    public boolean isSkillActive(String skillId) {
        if (skillBox != null) {
            return skillBox.isSkillActive(skillId);
        }
        return skillRepository.findBySkillId(skillId)
                .map(SkillDefinition::isActivated)
                .orElse(false);
    }

    // ==================== 技能删除和更新 ====================

    /**
     * 删除技能
     *
     * @param skillId 技能 ID
     */
    @Transactional
    public void deleteSkill(String skillId) {
        if (!skillRepository.existsBySkillId(skillId)) {
            throw new IllegalArgumentException("Skill not found: " + skillId);
        }

        skillRepository.deleteByName(
                skillId.contains("_") ? skillId.substring(0, skillId.lastIndexOf('_')) : skillId
        );

        // 从 SkillBox 移除
        if (skillBox != null) {
            skillBox.removeSkill(skillId);
        }

        log.info("Skill deleted: {}", skillId);
    }

    /**
     * 更新技能内容
     *
     * @param skillId        技能 ID
     * @param skillMdContent 新的 SKILL.md 内容
     * @param resources      新的资源文件
     * @return 更新后的技能定义
     */
    @Transactional
    public SkillDefinition updateSkill(
            String skillId,
            String skillMdContent,
            Map<String, String> resources) {
        AgentSkill newSkill = SkillUtil.createFrom(skillMdContent, resources, extractSource(skillId));

        // 构建更新后的技能
        AgentSkill updated = newSkill.toBuilder()
                .putMetadata("name", extractName(skillId))
                .putMetadata("source", extractSource(skillId))
                .build();

        return registerSkill(updated);
    }

    // ==================== SkillBox 集成 ====================

    /**
     * 获取技能系统提示词
     *
     * <p>此提示词包含所有已激活技能的信息，
     * 应在 Agent 系统提示词中使用。
     *
     * @return 技能系统提示词
     */
    public String getSkillPrompt() {
        if (skillBox == null) {
            log.warn("SkillBox not initialized, returning empty prompt");
            return "";
        }
        return skillBox.getSkillPrompt();
    }

    /**
     * 获取所有已注册技能的 ID
     *
     * @return 技能 ID 集合
     */
    public Set<String> getAllSkillIds() {
        if (skillBox == null) {
            return Collections.emptySet();
        }
        return skillBox.getAllSkillIds();
    }

    /**
     * 获取技能实例
     *
     * @param skillId 技能 ID
     * @return AgentSkill（可选）
     */
    public Optional<AgentSkill> getAgentSkill(String skillId) {
        if (skillBox == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(skillBox.getSkill(skillId));
    }

    /**
     * 同步 SkillBox 工具组状态
     *
     * <p>根据数据库中技能的激活状态，
     * 同步更新 SkillBox 中工具组的可用性。
     */
    public void syncToolGroupStates() {
        if (skillBox == null) {
            log.warn("SkillBox not initialized, skipping sync");
            return;
        }

        // 重置所有技能激活状态
        skillBox.deactivateAllSkills();

        // 根据数据库状态激活
        List<SkillDefinition> activeSkills = getActiveSkills();
        activeSkills.forEach(skill -> {
            if (skillBox.exists(skill.getSkillId())) {
                skillBox.setSkillActive(skill.getSkillId(), true);
            }
        });

        // 同步工具组状态
        skillBox.syncToolGroupStates();

        log.info("Synced {} active skills to SkillBox", activeSkills.size());
    }

    /**
     * 上传技能文件到工作目录
     *
     * <p>将技能的关联资源文件上传到代码执行工作目录，
     * 使技能可以访问这些资源。
     */
    public void uploadSkillFiles() {
        if (skillBox == null) {
            log.warn("SkillBox not initialized, skipping file upload");
            return;
        }
        skillBox.uploadSkillFiles();
    }

    // ==================== 统计和监控 ====================

    /**
     * 获取技能统计信息
     *
     * @return 技能统计
     */
    public SkillStats getStats() {
        List<SkillDefinition> allSkills = getAllSkills();
        List<SkillDefinition> activeSkills = getActiveSkills();

        return SkillStats.builder()
                .totalSkills(allSkills.size())
                .activeSkills(activeSkills.size())
                .inactiveSkills(allSkills.size() - activeSkills.size())
                .totalSkillIds(getAllSkillIds())
                .build();
    }

    /**
     * 重置所有技能激活状态
     *
     * <p>在每次 Agent 调用前调用，确保清洁的技能状态。
     */
    public void resetAllSkillStates() {
        if (skillBox != null) {
            skillBox.deactivateAllSkills();
            log.debug("Reset all skill states");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 skillId 提取技能名称
     */
    private String extractName(String skillId) {
        if (skillId == null) return "";
        int lastUnderscore = skillId.lastIndexOf('_');
        return lastUnderscore > 0 ? skillId.substring(0, lastUnderscore) : skillId;
    }

    /**
     * 从 skillId 提取来源
     */
    private String extractSource(String skillId) {
        if (skillId == null) return "custom";
        int lastUnderscore = skillId.lastIndexOf('_');
        return lastUnderscore > 0 ? skillId.substring(lastUnderscore + 1) : "custom";
    }

    /**
     * 递增版本号
     */
    private String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.0.0";
        }
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major + "." + minor + "." + (patch + 1);
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    /**
     * 从元数据提取标签
     */
    private String extractTags(Map<String, Object> metadata) {
        if (metadata == null) return null;
        Object tags = metadata.get("tags");
        if (tags instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }
        return tags != null ? tags.toString() : null;
    }

    /**
     * 技能统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class SkillStats {
        private long totalSkills;
        private long activeSkills;
        private long inactiveSkills;
        private Set<String> totalSkillIds;
    }
}