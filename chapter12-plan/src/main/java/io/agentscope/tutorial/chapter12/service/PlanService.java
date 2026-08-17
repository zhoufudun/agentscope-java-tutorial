package io.agentscope.tutorial.chapter12.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.tutorial.chapter12.model.Plan;
import io.agentscope.tutorial.chapter12.model.PlanState;
import io.agentscope.tutorial.chapter12.model.SubTask;
import io.agentscope.tutorial.chapter12.model.SubTaskState;
import io.agentscope.tutorial.chapter12.plan.PlanStorage;
import io.agentscope.tutorial.chapter12.entity.PlanRecord;
import io.agentscope.tutorial.chapter12.entity.SubTaskRecord;
import io.agentscope.tutorial.chapter12.repository.PlanRepository;
import io.agentscope.tutorial.chapter12.repository.SubTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 计划服务 - 实现基于 H2/MySQL 数据库的计划存储.
 *
 * <p>核心功能：
 * <ul>
 *   <li>实现 PlanStorage 接口，支持 PlanNotebook 的持久化需求</li>
 *   <li>提供计划的 CRUD 操作</li>
 *   <li>通过 Sinks 实现 SSE 广播计划变更</li>
 *   <li>提供历史计划和统计信息查询</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class PlanService implements PlanStorage {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlanRepository planRepository;
    private final SubTaskRepository subTaskRepository;
    private final ObjectMapper objectMapper;

    /** SSE 广播器，用于实时推送计划变更 */
    private final Sinks.Many<PlanResponse> planSink = Sinks.many().multicast().onBackpressureBuffer();

    public PlanService(PlanRepository planRepository,
                      SubTaskRepository subTaskRepository,
                      ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.subTaskRepository = subTaskRepository;
        this.objectMapper = objectMapper;
        log.info("PlanService initialized with database storage");
    }

    // ==================== PlanStorage 接口实现 ====================

    /**
     * 添加计划到数据库.
     */
    @Override
    @Transactional
    public Mono<Void> addPlan(Plan plan) {
        return Mono.fromRunnable(() -> {
            try {
                // 保存主表
                PlanRecord record = toPlanRecord(plan);
                planRepository.save(record);

                // 保存子任务表
                if (plan.getSubtasks() != null && !plan.getSubtasks().isEmpty()) {
                    for (int i = 0; i < plan.getSubtasks().size(); i++) {
                        SubTask subtask = plan.getSubtasks().get(i);
                        SubTaskRecord subtaskRecord = toSubTaskRecord(plan.getId(), i, subtask);
                        subTaskRepository.save(subtaskRecord);
                    }
                }

                log.info("Plan saved to database: {} (id={})", plan.getName(), plan.getId());
            } catch (Exception e) {
                log.error("Failed to save plan to database", e);
                throw new RuntimeException("Failed to save plan", e);
            }
        });
    }

    /**
     * 根据 ID 获取计划.
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<Plan> getPlan(String planId) {
        return Mono.fromCallable(() -> {
            Optional<PlanRecord> recordOpt = planRepository.findById(planId);
            return recordOpt.map(this::toPlan).orElse(null);
        });
    }

    /**
     * 获取所有历史计划.
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<List<Plan>> getPlans() {
        return Mono.fromCallable(() -> {
            List<PlanRecord> records = planRepository.findAllByOrderByCreatedAtDesc();
            List<Plan> plans = new ArrayList<>();
            for (PlanRecord record : records) {
                plans.add(toPlan(record));
            }
            return plans;
        });
    }

    // ==================== SSE 广播功能 ====================

    /**
     * 获取计划变更流（SSE）.
     */
    public Flux<PlanResponse> getPlanStream() {
        return Flux.concat(
                Mono.fromCallable(this::getCurrentPlanResponse),
                planSink.asFlux()
        );
    }

    /**
     * 广播计划变更.
     */
    public void broadcastPlanChange() {
        PlanResponse response = getCurrentPlanResponse();
        planSink.tryEmitNext(response);
        log.debug("Plan change broadcasted: {}",
                response.name != null ? response.name : "null");
    }

    /**
     * 获取当前计划的响应对象.
     */
    public PlanResponse getCurrentPlanResponse() {
        return new PlanResponse();
    }

    // ==================== 业务方法 ====================

    /**
     * 保存计划（同步版本，用于 Hook 回调）.
     */
    @Transactional
    public void savePlan(Plan plan) {
        PlanRecord record = toPlanRecord(plan);
        planRepository.save(record);
    }

    /**
     * 获取最近的 N 条历史计划.
     */
    @Transactional(readOnly = true)
    public List<Plan> getRecentPlans(int limit) {
        List<PlanRecord> records = planRepository.findTop10ByOrderByCreatedAtDesc();
        List<Plan> plans = new ArrayList<>();
        int count = 0;
        for (PlanRecord record : records) {
            if (count++ >= limit) break;
            plans.add(toPlan(record));
        }
        return plans;
    }

    /**
     * 获取计划统计信息.
     */
    @Transactional(readOnly = true)
    public PlanStatistics getStatistics() {
        return new PlanStatistics(
                planRepository.countTotal(),
                planRepository.countDone(),
                planRepository.countAbandoned(),
                planRepository.countInProgress(),
                planRepository.countTodo()
        );
    }

    // ==================== 实体转换方法 ====================

    /**
     * 将 Plan 转换为 PlanRecord.
     */
    private PlanRecord toPlanRecord(Plan plan) {
        PlanRecord record = new PlanRecord();
        record.setId(plan.getId());
        record.setName(plan.getName());
        record.setDescription(plan.getDescription());
        record.setExpectedOutcome(plan.getExpectedOutcome());
        record.setActualOutcome(plan.getOutcome());
        record.setState(plan.getState().getValue());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        // 序列化子任务为 JSON
        if (plan.getSubtasks() != null) {
            try {
                List<SubTaskJson> subtaskList = new ArrayList<>();
                for (SubTask subtask : plan.getSubtasks()) {
                    subtaskList.add(new SubTaskJson(
                            subtask.getName(),
                            subtask.getDescription(),
                            subtask.getExpectedOutcome(),
                            subtask.getOutcome(),
                            subtask.getState().getValue(),
                            subtask.getCreatedAt(),
                            subtask.getFinishedAt()
                    ));
                }
                record.setSubtasksJson(objectMapper.writeValueAsString(subtaskList));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize subtasks", e);
            }
        }

        // 设置完成时间
        if (plan.getState() == PlanState.DONE || plan.getState() == PlanState.ABANDONED) {
            record.setFinishedAt(LocalDateTime.now());
        }

        return record;
    }

    /**
     * 将 PlanRecord 转换为 Plan.
     */
    private Plan toPlan(PlanRecord record) {
        Plan plan = new Plan();
        plan.setId(record.getId());
        plan.setName(record.getName());
        plan.setDescription(record.getDescription());
        plan.setExpectedOutcome(record.getExpectedOutcome());
        plan.setOutcome(record.getActualOutcome());

        // 解析状态
        try {
            plan.setState(PlanState.valueOf(record.getState().toUpperCase()));
        } catch (IllegalArgumentException e) {
            plan.setState(PlanState.TODO);
        }

        // 解析子任务 JSON
        if (record.getSubtasksJson() != null && !record.getSubtasksJson().isEmpty()) {
            try {
                List<SubTaskJson> subtaskList = objectMapper.readValue(
                        record.getSubtasksJson(),
                        new TypeReference<List<SubTaskJson>>() {}
                );
                List<SubTask> subtasks = new ArrayList<>();
                for (SubTaskJson json : subtaskList) {
                    SubTask subtask = new SubTask(
                            json.name != null ? json.name : "Unnamed",
                            json.description != null ? json.description : "",
                            json.expectedOutcome != null ? json.expectedOutcome : ""
                    );
                    if (json.outcome != null) {
                        subtask.setOutcome(json.outcome);
                    }
                    if (json.state != null) {
                        try {
                            subtask.setState(SubTaskState.valueOf(json.state.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                            subtask.setState(SubTaskState.TODO);
                        }
                    }
                    subtask.setCreatedAt(json.createdAt);
                    subtask.setFinishedAt(json.finishedAt);
                    subtasks.add(subtask);
                }
                plan.setSubtasks(subtasks);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse subtasks JSON", e);
            }
        }

        return plan;
    }

    /**
     * 将 SubTask 转换为 SubTaskRecord.
     */
    private SubTaskRecord toSubTaskRecord(String planId, int index, SubTask subtask) {
        SubTaskRecord record = new SubTaskRecord(
                planId,
                index,
                subtask.getName(),
                subtask.getDescription(),
                subtask.getExpectedOutcome()
        );
        record.setState(subtask.getState().getValue());
        record.setActualOutcome(subtask.getOutcome());
        if (subtask.getState() == SubTaskState.DONE || subtask.getState() == SubTaskState.ABANDONED) {
            record.setFinishedAt(LocalDateTime.now());
        }
        return record;
    }

    // ==================== 内部类 ====================

    /**
     * 子任务 JSON 序列化 DTO.
     */
    private static class SubTaskJson {
        public String name;
        public String description;
        public String expectedOutcome;
        public String outcome;
        public String state;
        public String createdAt;
        public String finishedAt;

        public SubTaskJson() {}

        public SubTaskJson(String name, String description, String expectedOutcome,
                          String outcome, String state, String createdAt, String finishedAt) {
            this.name = name;
            this.description = description;
            this.expectedOutcome = expectedOutcome;
            this.outcome = outcome;
            this.state = state;
            this.createdAt = createdAt;
            this.finishedAt = finishedAt;
        }
    }

    /**
     * 计划响应 DTO（用于 SSE）.
     */
    public static class PlanResponse {
        public String id;
        public String name;
        public String description;
        public String state;
        public List<SubTaskResponse> subtasks;

        public PlanResponse() {
            this.subtasks = new ArrayList<>();
        }
    }

    /**
     * 子任务响应 DTO.
     */
    public static class SubTaskResponse {
        public int index;
        public String name;
        public String description;
        public String state;
    }

    /**
     * 计划统计信息.
     */
    public record PlanStatistics(
            long total,
            long done,
            long abandoned,
            long inProgress,
            long todo
    ) {}
}