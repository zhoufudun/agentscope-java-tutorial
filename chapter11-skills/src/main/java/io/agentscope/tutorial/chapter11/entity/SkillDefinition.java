package io.agentscope.tutorial.chapter11.entity;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.util.SkillUtil;
import jakarta.persistence.*;
import lombok.AllArgs构造函数;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgs构造函数;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 技能定义实体
 *
 * <p>对应数据库表 skill_definition，存储技能的核心定义信息。
 * 技能是 Agent 的能力扩展单元，包含技能描述、内容和资源。
 *
 * <p>主要功能：
 * <ul>
 *   <li>技能元数据存储（名称、描述、版本、作者）</li>
 *   <li>技能内容管理（SKILL.md 内容）</li>
 *   <li>技能激活状态管理</li>
 *   <li>与 AgentSkill 互相转换</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see AgentSkill
 * @see io.agentscope.core.skill.SkillBox
 */
@Entity
@Table(name = "skill_definition")
@Data
@NoArgs构造函数
@AllArgs构造函数
@Builder
public class SkillDefinition {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 技能唯一标识 (name_source 格式)
     */
    @Column(name = "skill_id", nullable = false, unique = true)
    private String skillId;

    /**
     * 技能名称
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 技能描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 技能版本
     */
    @Column(name = "version")
    @Builder.默认
    private String version = "1.0.0";

    /**
     * 技能作者
     */
    @Column(name = "author")
    private String author;

    /**
     * 技能标签（逗号分隔）
     */
    @Column(name = "tags")
    private String tags;

    /**
     * 技能内容 (SKILL.md 的完整内容)
     */
    @Column(name = "skill_content", columnDefinition = "TEXT")
    private String skillContent;

    /**
     * 技能来源
     */
    @Column(name = "source")
    @Builder.默认
    private String source = "custom";

    /**
     * 是否激活
     */
    @Column(name = "is_active")
    @Builder.默认
    private Boolean isActive = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 保存前自动更新创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (skillId == null && StringUtils.isNotBlank(name)) {
            skillId = name + "_" + source;
        }
    }

    /**
     * 更新前自动更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 将实体转换为 AgentSkill
     *
     * <p>用于将数据库中存储的技能定义转换为 AgentScope 核心技能对象。
     *
     * @param resources 技能关联的资源（路径 -> 内容）
     * @return AgentSkill 实例
     */
    public AgentSkill toAgentSkill(Map<String, String> resources) {
        if (skillContent == null || skillContent.isEmpty()) {
            throw new IllegalStateException("Skill content cannot be null or empty");
        }
        return SkillUtil.createFrom(skillContent, resources, source);
    }

    /**
     * 从 SKILL.md 格式内容创建 AgentSkill
     *
     * @return AgentSkill 实例
     */
    public AgentSkill toAgentSkill() {
        return toAgentSkill(new HashMap<>());
    }

    /**
     * 从 AgentSkill 创建实体
     *
     * @param agentSkill AgentSkill 对象
     * @return SkillDefinition 实体
     */
    public static SkillDefinition fromAgentSkill(AgentSkill agentSkill) {
        return SkillDefinition.builder()
                .skillId(agentSkill.getSkillId())
                .name(agentSkill.getName())
                .description(agentSkill.getDescription())
                .skillContent(agentSkill.getSkillContent())
                .source(agentSkill.getSource())
                .tags(extractTags(agentSkill.getMetadata()))
                .version(extractVersion(agentSkill.getMetadata()))
                .author(extractAuthor(agentSkill.getMetadata()))
                .isActive(true)
                .build();
    }

    /**
     * 从元数据提取标签
     */
    private static String extractTags(Map<String, Object> metadata) {
        Object tagsObj = metadata.get("tags");
        if (tagsObj instanceof java.util.List<?> tags) {
            return String.join(",", tags.stream()
                    .map(Object::toString)
                    .toList());
        }
        return tagsObj != null ? tagsObj.toString() : null;
    }

    /**
     * 从元数据提取版本
     */
    private static String extractVersion(Map<String, Object> metadata) {
        Object version = metadata.get("version");
        return version != null ? version.toString() : "1.0.0";
    }

    /**
     * 从元数据提取作者
     */
    private static String extractAuthor(Map<String, Object> metadata) {
        Object author = metadata.get("author");
        return author != null ? author.toString() : null;
    }

    /**
     * 获取标签数组
     */
    public String[] getTagsArray() {
        if (StringUtils.isBlank(tags)) {
            return new String[0];
        }
        return tags.split(",");
    }

    /**
     * 检查技能是否已激活
     */
    public boolean isActivated() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 检查技能是否有资源
     */
    public boolean hasResources() {
        return resources != null && !resources.isEmpty();
    }

    /**
     * 关联的资源
     */
    @Transient
    private Map<String, String> resources = new HashMap<>();

    /**
     * 设置资源
     */
    public void setResources(Map<String, String> resources) {
        this.resources = resources != null ? new HashMap<>(resources) : new HashMap<>();
    }

    /**
     * 获取资源
     */
    public Map<String, String> getResources() {
        return new HashMap<>(resources);
    }
}