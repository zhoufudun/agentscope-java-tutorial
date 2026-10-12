package io.agentscope.tutorial.chapter06.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型提供商实体类
 *
 * <p>存储不同模型提供商的配置信息。每个提供商代表一个 AI 服务平台，
 * 如阿里云百炼、OpenAI、Ollama 等。
 *
 * <p><b>核心概念</b>：
 * <ul>
 *   <li><b>提供商</b> - AI 服务的来源平台</li>
 *   <li><b>Base URL</b> - API 端点地址</li>
 *   <li><b>优先级</b> - 提供商的显示和选择顺序</li>
 * </ul>
 *
 * <p><b>安全建议</b>：
 * <ul>
 *   <li>API Key 应加密存储，生产环境推荐使用密钥管理服务</li>
 *   <li>不要在日志中打印 API Key</li>
 *   <li>定期轮换 API Key</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Entity
@Table(name = "model_provider")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProvider {

    /**
     * 主键 ID，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 提供商标识名称
     * <p>系统内唯一标识，如 dashscope、openai、ollama、gemini。
     * 用于在代码中引用提供商。
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 显示名称
     * <p>用于 UI 展示的友好名称，如"阿里云百炼 (DashScope)"
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * API 密钥
     * <p><b>安全警告</b>：生产环境应加密存储。
     * 此处使用明文仅作为教学演示。
     */
    @Column(length = 500)
    private String apiKey;

    /**
     * API 基础 URL
     * <p>模型服务的端点地址，如：
     * <ul>
     *   <li>DashScope: https://dashscope.aliyuncs.com/api/v1</li>
     *   <li>OpenAI: https://api.openai.com/v1</li>
     *   <li>Ollama: http://localhost:11434</li>
     * </ul>
     */
    @Column(length = 255)
    private String baseUrl;

    /**
     * 是否启用
     * <p>禁用的提供商不会出现在可选列表中
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 优先级
     * <p>数值越小优先级越高，用于：
     * <ul>
     *   <li>UI 展示排序</li>
     *   <li>默认提供商选择</li>
     *   <li>故障转移顺序</li>
     * </ul>
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    /**
     * 备注说明
     * <p>描述该提供商的特点和适用场景
     */
    @Column(length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 最后更新时间
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();

    /**
     * 更新前自动设置更新时间
     */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}