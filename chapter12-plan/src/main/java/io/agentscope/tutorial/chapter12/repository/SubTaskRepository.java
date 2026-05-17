package io.agentscope.tutorial.chapter12.repository;

import io.agentscope.tutorial.chapter12.entity.SubTaskRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 子任务记录仓库 - 提供子任务的持久化操作.
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface SubTaskRepository extends JpaRepository<SubTaskRecord, Long> {

    /**
     * 根据计划 ID 查询所有子任务（按索引排序）.
     *
     * @param planId 计划 ID
     * @return 子任务列表
     */
    List<SubTaskRecord> findByPlanIdOrderByTaskIndexAsc(String planId);

    /**
     * 根据计划 ID 和索引查询子任务.
     */
    Optional<SubTaskRecord> findByPlanIdAndTaskIndex(String planId, Integer taskIndex);

    /**
     * 根据计划 ID 删除所有子任务.
     */
    void deleteByPlanId(String planId);

    /**
     * 根据状态查询子任务.
     */
    List<SubTaskRecord> findByState(String state);

    /**
     * 统计指定状态的子任务数量.
     */
    long countByState(String state);
}