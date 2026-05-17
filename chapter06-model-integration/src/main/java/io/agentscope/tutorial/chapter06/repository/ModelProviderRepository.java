package io.agentscope.tutorial.chapter06.repository;

import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Model Provider Repository
 * 数据访问层 for model providers
 */
@Repository
public interface ModelProviderRepository extends JpaRepository<ModelProvider, Long> {

    /**
     * Find provider by name
     */
    Optional<ModelProvider> findByName(String name);

    /**
     * Find all enabled providers ordered by priority
     */
    List<ModelProvider> findByEnabledTrueOrderByPriorityAsc();

    /**
     * Check if provider exists
     */
    boolean existsByName(String name);
}