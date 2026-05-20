/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter19.assistant.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 日程管理 Agent 服务
 *
 * <p>负责：
 * <ul>
 *   <li>创建日程</li>
 *   <li>查询日程</li>
 *   <li>修改日程</li>
 *   <li>删除日程</li>
 *   <li>发送日程邀请</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class ScheduleAgent {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleAgent.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 模拟日程数据存储
    private final Map<String, List<Schedule>> userSchedules = new ConcurrentHashMap<>();

    // 生成ID计数器
    private long idCounter = 1;

    public ScheduleAgent() {
        initializeSampleData();
    }

    /**
     * 初始化示例数据
     */
    private void initializeSampleData() {
        LocalDateTime now = LocalDateTime.now();

        // 添加示例日程
        Schedule s1 = new Schedule(
                idCounter++,
                "user001",
                "部门周例会",
                "讨论本周工作进展和下周计划",
                now.plusDays(1).withHour(10).withMinute(0),
                now.plusDays(1).withHour(11).withMinute(0),
                List.of("zhangsan@company.com", "lisi@company.com")
        );

        Schedule s2 = new Schedule(
                idCounter++,
                "user001",
                "项目评审会",
                "新产品功能评审",
                now.plusDays(2).withHour(14).withMinute(0),
                now.plusDays(2).withHour(16).withMinute(0),
                List.of("pm@company.com", "tech@company.com")
        );

        Schedule s3 = new Schedule(
                idCounter++,
                "user001",
                "一对一沟通",
                "与新员工沟通入职情况",
                now.plusDays(3).withHour(15).withMinute(0),
                now.plusDays(3).withHour(15).withMinute(30),
                List.of("newemployee@company.com")
        );

        userSchedules.put("user001", new ArrayList<>(List.of(s1, s2, s3)));
    }

    /**
     * 创建日程
     *
     * @param title 标题
     * @param description 描述
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 创建结果
     */
    public String createSchedule(String title, String description,
                                  String startTime, String endTime) {
        return createSchedule(title, description, startTime, endTime, "current_user", null);
    }

    /**
     * 创建日程（完整版）
     */
    public String createSchedule(String title, String description,
                                  String startTime, String endTime,
                                  String userId, List<String> attendees) {
        logger.info("创建日程: {} for user {}", title, userId);

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, FORMATTER);

            Schedule schedule = new Schedule(
                    idCounter++,
                    userId,
                    title,
                    description,
                    start,
                    end,
                    attendees != null ? attendees : new ArrayList<>()
            );

            userSchedules.computeIfAbsent(userId, k -> new ArrayList<>()).add(schedule);

            StringBuilder result = new StringBuilder();
            result.append("日程创建成功！\n\n");
            result.append(String.format("标题：%s\n", schedule.title));
            result.append(String.format("时间：%s - %s\n",
                    schedule.startTime.format(FORMATTER),
                    schedule.endTime.format(FORMATTER)));
            if (description != null && !description.isEmpty()) {
                result.append(String.format("描述：%s\n", description));
            }
            if (attendees != null && !attendees.isEmpty()) {
                result.append(String.format("参与人：%s\n", String.join(", ", attendees)));
            }
            result.append(String.format("日程ID：%d", schedule.id));

            return result.toString();

        } catch (Exception e) {
            logger.error("创建日程失败", e);
            return "创建日程失败：" + e.getMessage() + "\n" +
                    "时间格式应为：yyyy-MM-dd HH:mm";
        }
    }

    /**
     * 查询用户日程
     *
     * @param userId 用户ID
     * @param date 日期（可选，如 "2024-01-15"）
     * @return 日程列表
     */
    public String querySchedules(String userId, String date) {
        logger.info("查询日程: {} on {}", userId, date);

        List<Schedule> schedules = userSchedules.getOrDefault(userId, new ArrayList<>());

        // 如果指定了日期，过滤该日期的日程
        if (date != null && !date.isEmpty()) {
            schedules = schedules.stream()
                    .filter(s -> s.startTime.toLocalDate().toString().equals(date))
                    .collect(Collectors.toList());
        } else {
            // 只返回未来和今天的日程
            LocalDateTime now = LocalDateTime.now();
            schedules = schedules.stream()
                    .filter(s -> !s.startTime.isBefore(now.toLocalDate().atStartOfDay()))
                    .sorted(Comparator.comparing(s -> s.startTime))
                    .collect(Collectors.toList());
        }

        if (schedules.isEmpty()) {
            return date != null
                    ? String.format("您在 %s 没有日程安排。", date)
                    : "您近期没有日程安排。";
        }

        StringBuilder result = new StringBuilder();
        result.append("您的日程安排：\n\n");

        for (int i = 0; i < schedules.size(); i++) {
            Schedule s = schedules.get(i);
            result.append(String.format("%d. %s\n", i + 1, s.title));
            result.append(String.format("   时间：%s - %s\n",
                    s.startTime.format(FORMATTER),
                    s.endTime.format(FORMATTER)));
            if (s.description != null && !s.description.isEmpty()) {
                result.append(String.format("   描述：%s\n", s.description));
            }
            if (s.attendees != null && !s.attendees.isEmpty()) {
                result.append(String.format("   参与人：%s\n", String.join(", ", s.attendees)));
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 更新日程
     *
     * @param scheduleId 日程ID
     * @param updates 更新内容
     * @return 更新结果
     */
    public String updateSchedule(long scheduleId, Map<String, String> updates) {
        logger.info("更新日程: {}", scheduleId);

        for (List<Schedule> schedules : userSchedules.values()) {
            for (Schedule schedule : schedules) {
                if (schedule.id == scheduleId) {
                    if (updates.containsKey("title")) {
                        schedule.title = updates.get("title");
                    }
                    if (updates.containsKey("description")) {
                        schedule.description = updates.get("description");
                    }
                    if (updates.containsKey("startTime")) {
                        schedule.startTime = LocalDateTime.parse(updates.get("startTime"), FORMATTER);
                    }
                    if (updates.containsKey("endTime")) {
                        schedule.endTime = LocalDateTime.parse(updates.get("endTime"), FORMATTER);
                    }

                    return "日程更新成功！";
                }
            }
        }

        return "未找到指定的日程。";
    }

    /**
     * 删除日程
     *
     * @param scheduleId 日程ID
     * @param userId 用户ID
     * @return 删除结果
     */
    public String deleteSchedule(long scheduleId, String userId) {
        logger.info("删除日程: {} for user {}", scheduleId, userId);

        List<Schedule> schedules = userSchedules.get(userId);
        if (schedules == null) {
            return "未找到指定的日程。";
        }

        boolean removed = schedules.removeIf(s -> s.id == scheduleId);
        return removed ? "日程已删除。" : "未找到指定的日程。";
    }

    /**
     * 发送日程邀请
     *
     * @param scheduleId 日程ID
     * @return 发送结果
     */
    public String sendInvitation(long scheduleId) {
        logger.info("发送日程邀请: {}", scheduleId);

        for (List<Schedule> schedules : userSchedules.values()) {
            for (Schedule schedule : schedules) {
                if (schedule.id == scheduleId) {
                    if (schedule.attendees == null || schedule.attendees.isEmpty()) {
                        return "该日程没有参与人，无需发送邀请。";
                    }

                    // 模拟发送邀请
                    String result = String.format("已向以下人员发送日程邀请：\n%s\n\n" +
                                    "日程：%s\n时间：%s - %s",
                            String.join("\n", schedule.attendees),
                            schedule.title,
                            schedule.startTime.format(FORMATTER),
                            schedule.endTime.format(FORMATTER));

                    return result;
                }
            }
        }

        return "未找到指定的日程。";
    }

    /**
     * 日程数据类
     */
    private static class Schedule {
        long id;
        String userId;
        String title;
        String description;
        LocalDateTime startTime;
        LocalDateTime endTime;
        List<String> attendees;

        Schedule(long id, String userId, String title, String description,
                 LocalDateTime startTime, LocalDateTime endTime, List<String> attendees) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
            this.attendees = attendees;
        }
    }
}