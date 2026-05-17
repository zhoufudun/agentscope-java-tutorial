package io.agentscope.tutorial.chapter12.repository;

import io.agentscope.tutorial.chapter12.entity.PlanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 计划记录仓库 - 提供计划的持久化操作.
 *
 * <p>继承 Spring Data JPA 的 JpaRepository，提供常用的 CRUD 操作。
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface PlanRepository extends JpaRepository<PlanRecord, String> {

    /**
     * 根据状态查询计划列表.
     *
     * @param state 计划状态
     * @return 符合条件的计划列表
     */
    List<PlanRecord> findByState(String state);

    /**
     * 根据状态查询计划列表（按创建时间倒序）.
     */
    List<PlanRecord> findByStateOrderByCreatedAtDesc(String state);

    /**
     * 根据名称模糊查询计划.
     *
     * @param namePattern 名称匹配模式
     * @return 符合条件的计划列表
     */
    List<PlanRecord> findByNameContainingIgnoreCase(String namePattern);

    /**
     * 查询所有计划，按创建时间倒序排列.
     *
     * @return 所有计划列表
     */
    List<PlanRecord> findAllByOrderByCreatedAtDesc();

    /**
     * 查询最近的 N 条计划.
     */
    List<PlanRecord> findTop10ByOrderByCreatedAtDesc();

    /**
     * 删除指定时间之前创建的计划（清理历史）.
     *
     * @param beforeTime 删除此时间之前的计划
     * @return 删除的计划数量
     */
    long deleteByCreatedAtBefore(java.time.LocalDateTime beforeTime);

    /**
     * 统计指定状态的计划数量.
     *
     * @param state 计划状态
     * @return 计划数量
     */
    long countByState(String state);

    /**
     * 获取计划统计信息.
     */
    @Query("SELECT COUNT(p) FROM PlanRecord p")
    long countTotal();

    @Query("SELECT COUNT(p) FROM PlanRecord p WHERE p.state = 'DONE'")
    long countDone();

    @Query("SELECT COUNT(p) FROM PlanRecord p WHERE p.state = 'ABANDONED'")
    long countAbandoned();

    @Query("SELECT COUNT(p) FROM PlanRecord p WHERE p.state = 'IN_PROGRESS'")
    long countInProgress();

    @Query("SELECT COUNT(p) FROM PlanRecord p WHERE p.state = 'TODO'")
    long countTodo();
}