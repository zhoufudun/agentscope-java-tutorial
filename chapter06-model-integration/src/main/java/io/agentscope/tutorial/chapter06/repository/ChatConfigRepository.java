package io.agentscope.tutorial.chapter06.repository;

import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Chat Configuration Repository
 * 数据访问层 for chat configurations
 */
@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, Long> {

    /**
     * Find all non-default configurations
     */
    List<ChatConfig> findByIs默认False();

    /**
     * Find configurations by provider type
     */
    List<ChatConfig> findByProviderType(String providerType);

    /**
     * Find default configuration
     */
    Optional<ChatConfig> findByIs默认True();

    /**
     * Find configuration by name
     */
    Optional<ChatConfig> findByName(String name);
}