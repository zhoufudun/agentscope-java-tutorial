package io.agentscope.tutorial.chapter11.repository;

import io.agentscope.tutorial.chapter11.entity.SkillDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 技能定义数据访问层
 *
 * <p>提供技能实体的持久化操作，包括：
 * <ul>
 *   <li>基本 CRUD 操作</li>
 *   <li>按名称/来源查询</li>
 *   <li>激活状态过滤</li>
 *   <li>批量操作</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see SkillDefinition
 */
@Repository
public interface SkillRepository extends JpaRepository<SkillDefinition, Long> {

    /**
     * 根据技能 ID 查询
     *
     * @param skillId 技能唯一标识 (name_source)
     * @return 技能定义（可选）
     */
    Optional<SkillDefinition> findBySkillId(String skillId);

    /**
     * 根据技能名称查询
     *
     * @param name 技能名称
     * @return 技能定义列表
     */
    List<SkillDefinition> findByName(String name);

    /**
     * 根据来源查询
     *
     * @param source 技能来源
     * @return 技能定义列表
     */
    List<SkillDefinition> findBySource(String source);

    /**
     * 查询所有已激活的技能
     *
     * @return 已激活的技能列表
     */
    List<SkillDefinition> findByIsActiveTrue();

    /**
     * 根据标签查询技能
     *
     * @param tag 标签
     * @return 匹配的技能列表
     */
    @Query("SELECT s FROM SkillDefinition s WHERE s.tags LIKE %:tag%")
    List<SkillDefinition> findByTag(@Param("tag") String tag);

    /**
     * 搜索技能（名称或描述模糊匹配）
     *
     * @param keyword 搜索关键字
     * @return 匹配的技能列表
     */
    @Query("SELECT s FROM SkillDefinition s WHERE " +
           "s.name LIKE %:keyword% OR s.description LIKE %:keyword% OR s.tags LIKE %:keyword%")
    List<SkillDefinition> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 检查技能是否存在
     *
     * @param skillId 技能 ID
     * @return 是否存在
     */
    boolean existsBySkillId(String skillId);

    /**
     * 根据名称和来源查找技能
     *
     * @param name   技能名称
     * @param source 技能来源
     * @return 技能定义（可选）
     */
    Optional<SkillDefinition> findByNameAndSource(String name, String source);

    /**
     * 获取所有技能，按更新时间降序
     *
     * @return 按更新时间排序的技能列表
     */
    List<SkillDefinition> findAllByOrderByUpdatedAtDesc();

    /**
     * 获取最近创建的 N 个技能
     *
     * @param limit 数量限制
     * @return 最近创建的技能列表
     */
    List<SkillDefinition> findTop10ByOrderByCreatedAtDesc();

    /**
     * 根据技能名称删除
     *
     * @param name 技能名称
     */
    void deleteByName(String name);

    /**
     * 批量激活技能
     *
     * @param skillIds 技能 ID 列表
     */
    @Query("UPDATE SkillDefinition s SET s.isActive = true WHERE s.skillId IN :skillIds")
    void activateSkills(@Param("skillIds") List<String> skillIds);

    /**
     * 批量停用技能
     *
     * @param skillIds 技能 ID 列表
     */
    @Query("UPDATE SkillDefinition s SET s.isActive = false WHERE s.skillId IN :skillIds")
    void deactivateSkills(@Param("skillIds") List<String> skillIds);

    /**
     * 获取已激活技能数量
     *
     * @return 已激活技能数量
     */
    long countByIsActiveTrue();
}