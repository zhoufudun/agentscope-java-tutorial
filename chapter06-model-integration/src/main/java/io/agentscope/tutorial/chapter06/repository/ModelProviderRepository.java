package io.agentscope.tutorial.chapter06.repository;

import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型提供商数据访问层
 *
 * <p>提供模型提供商实体的数据库操作。
 * 用于管理支持的 AI 服务提供商信息。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Repository
public interface ModelProviderRepository extends JpaRepository<ModelProvider, Long> {

    /**
     * 按名称查找提供商
     *
     * @param name 提供商标识名（如 dashscope、openai）
     * @return 匹配的提供商（如果存在）
     */
    Optional<ModelProvider> findByName(String name);

    /**
     * 查找所有已启用的提供商，按优先级排序
     *
     * <p>优先级数值越小越靠前，用于 UI 展示和默认选择。
     *
     * @return 已启用的提供商列表，按优先级升序排列
     */
    List<ModelProvider> findByEnabledTrueOrderByPriorityAsc();

    /**
     * 检查提供商是否存在
     *
     * <p>用于初始化时避免重复创建。
     *
     * @param name 提供商标识名
     * @return 是否存在
     */
    boolean existsByName(String name);
}