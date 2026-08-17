package io.agentscope.tutorial.chapter12.plan;

import io.agentscope.tutorial.chapter12.model.Plan;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 计划存储接口（教程自有）.
 *
 * <p>【2.x 迁移说明】原接口 {@code io.agentscope.core.plan.storage.PlanStorage} 在 2.x 已被移除。
 * 由于 2.x 不再提供结构化计划模型，也就不再有对应的存储抽象；这里将其下沉为教程自有接口，
 * 由 {@link io.agentscope.tutorial.chapter12.service.PlanService} 基于 H2/MySQL 实现，
 * 用于演示“把结构化计划持久化到数据库并支持历史查询”。
 */
public interface PlanStorage {

    /**
     * 保存（新增或更新）一个计划.
     */
    Mono<Void> addPlan(Plan plan);

    /**
     * 按 ID 获取计划，不存在时返回 {@code null}.
     */
    Mono<Plan> getPlan(String planId);

    /**
     * 获取所有历史计划.
     */
    Mono<List<Plan>> getPlans();
}
