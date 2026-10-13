package io.agentscope.tutorial.chapter06.repository;

import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天配置数据访问层
 *
 * <p>继承 {@link JpaRepository}，提供聊天配置的 CRUD 操作和自定义查询。
 * Spring Data JPA 会自动根据方法名生成查询实现。
 *
 * <p><b>命名查询规则</b>：
 * <ul>
 *   <li>{@code findBy<属性>} - 根据属性查询</li>
 *   <li>{@code findBy<属性>True/False} - 根据布尔属性查询</li>
 *   <li>{@code findBy<属性>OrderBy<属性>} - 查询并排序</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, Long> {

    /**
     * 查询所有非默认配置
     *
     * @return 非默认配置列表
     */
    List<ChatConfig> findByIsDefaultFalse();

    /**
     * 按提供商类型查询配置
     *
     * @param providerType 提供商类型（如 dashscope、openai）
     * @return 该提供商的所有配置
     */
    List<ChatConfig> findByProviderType(String providerType);

    /**
     * 查找默认配置
     *
     * <p>系统中同时只有一个默认配置，用于快速启动对话。
     *
     * @return 默认配置（如果存在）
     */
    Optional<ChatConfig> findByIsDefaultTrue();

    /**
     * 按名称查找配置
     *
     * @param name 配置名称
     * @return 匹配的配置（如果存在）
     */
    Optional<ChatConfig> findByName(String name);
}