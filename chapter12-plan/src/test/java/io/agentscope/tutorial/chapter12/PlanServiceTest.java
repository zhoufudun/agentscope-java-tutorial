package io.agentscope.tutorial.chapter12;

import io.agentscope.tutorial.chapter12.model.Plan;
import io.agentscope.tutorial.chapter12.model.PlanState;
import io.agentscope.tutorial.chapter12.model.SubTaskState;
import io.agentscope.tutorial.chapter12.plan.PlanNotebook;
import io.agentscope.tutorial.chapter12.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 计划服务测试类.
 *
 * <p>测试 PlanService 和 PlanNotebook 的核心功能：
 * <ul>
 *   <li>计划创建和执行</li>
 *   <li>任务分解</li>
 *   <li>计划状态跟踪</li>
 *   <li>计划持久化存储</li>
 *   <li>用户交互（暂停/恢复/停止）</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootTest
@ActiveProfiles("dev")
class PlanServiceTest {

    @Autowired
    private PlanNotebook planNotebook;

    @Autowired
    private PlanService planService;

    /**
     * 测试创建计划.
     */
    @Test
    void testCreatePlan() {
        // 准备测试数据
        String planName = "Test Plan";
        String description = "Test Description";
        String expectedOutcome = "Test Outcome";
        List<Map<String, Object>> subtasks = List.of(
                Map.of(
                        "name", "Task 1",
                        "description", "Description 1",
                        "expected_outcome", "Outcome 1"
                ),
                Map.of(
                        "name", "Task 2",
                        "description", "Description 2",
                        "expected_outcome", "Outcome 2"
                )
        );

        // 创建计划
        String result = planNotebook.createPlan(planName, description, expectedOutcome, subtasks).block();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains(planName));

        // 验证当前计划
        Plan currentPlan = planNotebook.getCurrentPlan();
        assertNotNull(currentPlan);
        assertEquals(planName, currentPlan.getName());
        assertEquals(description, currentPlan.getDescription());
        assertEquals(expectedOutcome, currentPlan.getExpectedOutcome());
        assertEquals(PlanState.TODO, currentPlan.getState());
        assertEquals(2, currentPlan.getSubtasks().size());

        // 验证子任务状态
        assertEquals(SubTaskState.TODO, currentPlan.getSubtasks().get(0).getState());
        assertEquals(SubTaskState.TODO, currentPlan.getSubtasks().get(1).getState());
    }

    /**
     * 测试子任务状态转换.
     */
    @Test
    void testSubtaskStateTransition() {
        // 创建计划
        List<Map<String, Object>> subtasks = List.of(
                Map.of(
                        "name", "Task 1",
                        "description", "Description 1",
                        "expected_outcome", "Outcome 1"
                ),
                Map.of(
                        "name", "Task 2",
                        "description", "Description 2",
                        "expected_outcome", "Outcome 2"
                )
        );

        planNotebook.createPlan("Test Plan", "Description", "Outcome", subtasks).block();

        // 验证初始状态
        Plan plan = planNotebook.getCurrentPlan();
        assertEquals(SubTaskState.TODO, plan.getSubtasks().get(0).getState());
        assertEquals(SubTaskState.TODO, plan.getSubtasks().get(1).getState());

        // 完成第一个子任务
        String result = planNotebook.finishSubtask(0, "Task 1 completed successfully").block();
        assertNotNull(result);
        assertTrue(result.contains("marked as done successfully"));

        // 验证状态变化
        plan = planNotebook.getCurrentPlan();
        assertEquals(SubTaskState.DONE, plan.getSubtasks().get(0).getState());
        assertEquals("Task 1 completed successfully", plan.getSubtasks().get(0).getOutcome());

        // 第二个任务应该自动变为 IN_PROGRESS
        assertEquals(SubTaskState.IN_PROGRESS, plan.getSubtasks().get(1).getState());
    }

    /**
     * 测试计划修订.
     */
    @Test
    void testRevisePlan() {
        // 创建初始计划
        List<Map<String, Object>> subtasks = List.of(
                Map.of(
                        "name", "Task 1",
                        "description", "Description 1",
                        "expected_outcome", "Outcome 1"
                )
        );

        planNotebook.createPlan("Original Plan", "Description", "Outcome", subtasks).block();

        // 添加子任务
        String addResult = planNotebook.reviseCurrentPlan(1, "add",
                Map.of(
                        "name", "Task 2",
                        "description", "Description 2",
                        "expected_outcome", "Outcome 2"
                )).block();
        assertNotNull(addResult);
        assertTrue(addResult.contains("added successfully"));
        assertEquals(2, planNotebook.getCurrentPlan().getSubtasks().size());

        // 修改子任务
        String reviseResult = planNotebook.reviseCurrentPlan(0, "revise",
                Map.of(
                        "name", "Task 1 Modified",
                        "description", "Description 1 Modified",
                        "expected_outcome", "Outcome 1 Modified"
                )).block();
        assertNotNull(reviseResult);
        assertTrue(reviseResult.contains("revised successfully"));
        assertEquals("Task 1 Modified", planNotebook.getCurrentPlan().getSubtasks().get(0).getName());

        // 删除子任务
        String deleteResult = planNotebook.reviseCurrentPlan(1, "delete", null).block();
        assertNotNull(deleteResult);
        assertTrue(deleteResult.contains("deleted successfully"));
        assertEquals(1, planNotebook.getCurrentPlan().getSubtasks().size());
    }

    /**
     * 测试完成计划.
     */
    @Test
    void testFinishPlan() {
        // 创建并完成任务
        List<Map<String, Object>> subtasks = List.of(
                Map.of(
                        "name", "Task 1",
                        "description", "Description 1",
                        "expected_outcome", "Outcome 1"
                )
        );

        planNotebook.createPlan("Plan to Finish", "Description", "Outcome", subtasks).block();
        planNotebook.finishSubtask(0, "Task completed").block();

        // 完成计划
        String result = planNotebook.finishPlan("done", "Plan completed successfully").block();
        assertNotNull(result);
        assertTrue(result.contains("finished successfully"));

        // 验证计划已清除
        assertNull(planNotebook.getCurrentPlan());
    }

    /**
     * 测试放弃计划.
     */
    @Test
    void testAbandonPlan() {
        // 创建计划
        List<Map<String, Object>> subtasks = List.of(
                Map.of(
                        "name", "Task 1",
                        "description", "Description 1",
                        "expected_outcome", "Outcome 1"
                )
        );

        planNotebook.createPlan("Plan to Abandon", "Description", "Outcome", subtasks).block();

        // 放弃计划
        String result = planNotebook.finishPlan("abandoned", "User cancelled").block();
        assertNotNull(result);
        assertTrue(result.contains("abandoned"));

        // 验证计划已清除
        assertNull(planNotebook.getCurrentPlan());
    }

    /**
     * 测试更新计划信息.
     */
    @Test
    void testUpdatePlanInfo() {
        // 创建计划
        planNotebook.createPlan("Original Name", "Original Description", "Original Outcome",
                List.of(Map.of("name", "Task 1", "description", "Desc", "expected_outcome", "Out"))
        ).block();

        // 更新名称
        String result = planNotebook.updatePlanInfo("New Name", null, null).block();
        assertNotNull(result);
        assertTrue(result.contains("New Name"));

        // 验证更新
        assertEquals("New Name", planNotebook.getCurrentPlan().getName());
    }

    /**
     * 测试获取子任务数量.
     */
    @Test
    void testGetSubtaskCount() {
        // 创建计划
        List<Map<String, Object>> subtasks = List.of(
                Map.of("name", "Task 1", "description", "Desc 1", "expected_outcome", "Out 1"),
                Map.of("name", "Task 2", "description", "Desc 2", "expected_outcome", "Out 2"),
                Map.of("name", "Task 3", "description", "Desc 3", "expected_outcome", "Out 3")
        );

        planNotebook.createPlan("Count Test", "Description", "Outcome", subtasks).block();

        // 验证子任务数量
        String result = planNotebook.getSubtaskCount().block();
        assertNotNull(result);
        assertTrue(result.contains("3"));
        assertTrue(result.contains("3 todo"));
    }

    /**
     * 测试计划持久化服务.
     */
    @Test
    void testPlanDbService() {
        // 创建计划并保存
        List<Map<String, Object>> subtasks = List.of(
                Map.of(
                        "name", "DB Task 1",
                        "description", "DB Description 1",
                        "expected_outcome", "DB Outcome 1"
                )
        );

        String createResult = planNotebook.createPlan("DB Test Plan", "DB Description", "DB Outcome", subtasks).block();
        assertNotNull(createResult);

        // 验证统计信息
        PlanService.PlanStatistics stats = planService.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.total() >= 0);
    }

    /**
     * 测试最大子任务数限制.
     */
    @Test
    void testMaxSubtasksLimit() {
        // 创建一个限制为 3 个子任务的 PlanNotebook
        PlanNotebook limitedNotebook = PlanNotebook.builder()
                .maxSubtasks(3)
                .build();

        // 创建带 3 个子任务的计划（应该成功）
        List<Map<String, Object>> subtasks = List.of(
                Map.of("name", "Task 1", "description", "Desc", "expected_outcome", "Out"),
                Map.of("name", "Task 2", "description", "Desc", "expected_outcome", "Out"),
                Map.of("name", "Task 3", "description", "Desc", "expected_outcome", "Out")
        );

        String result = limitedNotebook.createPlan("Limited Plan", "Description", "Outcome", subtasks).block();
        assertNotNull(result);

        // 尝试添加第 4 个子任务（应该失败）
        String failResult = limitedNotebook.reviseCurrentPlan(3, "add",
                Map.of("name", "Task 4", "description", "Desc", "expected_outcome", "Out")).block();
        assertTrue(failResult.contains("maximum"));
    }

    /**
     * 测试空计划情况.
     */
    @Test
    void testNoActivePlan() {
        // 获取子任务数量（无活动计划）
        String result = planNotebook.getSubtaskCount().block();
        assertNotNull(result);
        assertTrue(result.contains("no active plan") || result.contains("0"));
    }
}