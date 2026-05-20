package io.agentscope.tutorial.chapter06.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chat Configuration Entity
 * Stores user's chat configuration and history
 */
@Entity
@Table(name = "chat_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration name
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Model provider type
     */
    @Column(nullable = false, length = 50)
    private String providerType;

    /**
     * Model name
     */
    @Column(nullable = false, length = 100)
    private String modelName;

    /**
     * Temperature parameter for generation
     */
    @Column
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * Maximum tokens for generation
     */
    @Column
    @Builder.Default
    private Integer maxTokens = 2000;

    /**
     * System prompt
     */
    @Column(length = 4000)
    private String systemPrompt;

    /**
     * Whether streaming is enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean streamEnabled = true;

    /**
     * Whether this is the default configuration
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

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