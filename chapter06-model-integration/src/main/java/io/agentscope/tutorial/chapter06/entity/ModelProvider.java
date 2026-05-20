package io.agentscope.tutorial.chapter06.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model Provider Entity
 * Stores information about different model providers
 */
@Entity
@Table(name = "model_provider")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Provider name (e.g., dashscope, openai, ollama, gemini)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Display name
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * API Key (encrypted storage recommended)
     */
    @Column(length = 500)
    private String apiKey;

    /**
     * Base URL for API calls
     */
    @Column(length = 255)
    private String baseUrl;

    /**
     * Whether this provider is enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Priority (lower number = higher priority)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    /**
     * Additional remarks or description
     */
    @Column(length = 500)
    private String remark;

    /**
     * Creation timestamp
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * Last update timestamp
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}