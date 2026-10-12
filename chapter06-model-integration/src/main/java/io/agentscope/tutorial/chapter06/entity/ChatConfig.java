package io.agentscope.tutorial.chapter06.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天配置实体类
 *
 * <p>存储用户的聊天配置和参数设置。每个配置代表一组预设的聊天参数，
 * 便于用户快速切换不同的对话风格和模型设置。
 *
 * <p><b>主要用途</b>：
 * <ul>
 *   <li>保存常用的模型和参数组合</li>
 *   <li>为不同场景（如技术问答、创意写作）创建专用配置</li>
 *   <li>管理默认配置，简化日常使用</li>
 * </ul>
 *
 * <p><b>示例配置</b>：
 * <pre>{@code
 * ChatConfig config = ChatConfig.builder()
 *     .name("技术问答配置")
 *     .providerType("dashscope")
 *     .modelName("qwen-plus")
 *     .temperature(0.3)  // 低温度，更精确
 *     .maxTokens(4000)
 *     .systemPrompt("你是一个专业的技术顾问...")
 *     .build();
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Entity
@Table(name = "chat_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConfig {

    /**
     * 主键 ID，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置名称
     * <p>用于标识该配置，如"DashScope 默认配置"、"创意写作配置"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 模型提供商类型
     * <p>可选值：dashscope、openai、ollama、gemini
     */
    @Column(nullable = false, length = 50)
    private String providerType;

    /**
     * 模型名称
     * <p>如 qwen-plus、gpt-4o、llama3.2 等
     */
    @Column(nullable = false, length = 100)
    private String modelName;

    /**
     * 生成温度参数
     * <p>控制输出的随机性：
     * <ul>
     *   <li>0.0 - 完全确定性</li>
     *   <li>0.3 - 适合技术问答</li>
     *   <li>0.7 - 平衡（默认）</li>
     *   <li>1.0 - 最大创造性</li>
     * </ul>
     */
    @Column
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * 最大生成 token 数
     * <p>限制单次回复的长度，避免过长或超出配额
     */
    @Column
    @Builder.Default
    private Integer maxTokens = 2000;

    /**
     * 系统提示词
     * <p>定义 AI 的角色、能力边界和回复风格。
     * 好的系统提示词能显著提升回复质量。
     */
    @Column(length = 4000)
    private String systemPrompt;

    /**
     * 是否启用流式输出
     * <p>启用后支持打字机效果，提升交互体验
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean streamEnabled = true;

    /**
     * 是否为默认配置
     * <p>系统中同时只能有一个默认配置
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

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