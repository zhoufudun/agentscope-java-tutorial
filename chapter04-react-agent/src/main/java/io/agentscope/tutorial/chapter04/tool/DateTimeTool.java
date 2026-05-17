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
package io.agentscope.tutorial.chapter04.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * 日期时间工具集
 *
 * 提供日期时间相关的查询和计算功能：
 * - 获取当前时间（支持不同时区）
 * - 日期计算
 * - 格式化日期
 *
 * @author AgentScope Tutorial
 */
public class DateTimeTool {

    private static final Random RANDOM = new Random();

    /**
     * 获取当前时间信息
     *
     * @param timezone 时区名称（如：Asia/Shanghai, America/New_York）
     * @param format 时间格式（如：yyyy-MM-dd HH:mm:ss）
     * @return 当前时间信息
     */
    @Tool(
            name = "get_current_time",
            description = "获取当前的日期和时间信息。适用于需要获取当前时间戳的场景。"
    )
    public Mono<String> getCurrentTime(
            @ToolParam(name = "timezone",
                    description = "时区名称，如：Asia/Shanghai, America/New_York, Europe/London")
            String timezone,
            @ToolParam(name = "format",
                    description = "时间格式，如：yyyy-MM-dd HH:mm:ss，默认返回完整信息")
            String format) {

        return Mono.fromCallable(() -> {
            simulateDelay(100, 200);

            try {
                ZoneId zoneId = ZoneId.of(timezone);
                LocalDateTime now = LocalDateTime.now(zoneId);

                if (format == null || format.isBlank()) {
                    return String.format(
                            "【当前时间信息】\n" +
                                    "时区: %s\n" +
                                    "日期: %s\n" +
                                    "时间: %s\n" +
                                    "星期: %s\n" +
                                    "年份: %d年第%d天",
                            timezone,
                            now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                            now.format(DateTimeFormatter.ofPattern("HH时mm分ss秒")),
                            now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL,
                                    java.util.Locale.CHINA),
                            now.getYear(),
                            now.getDayOfYear()
                    );
                }

                return now.format(DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                return String.format("错误: %s", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 计算两个日期之间的天数
     *
     * @param startDate 起始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 天数差
     */
    @Tool(
            name = "calculate_days_between",
            description = "计算两个日期之间的天数差。"
    )
    public Mono<String> calculateDaysBetween(
            @ToolParam(name = "start_date", description = "起始日期，格式：yyyy-MM-dd")
            String startDate,
            @ToolParam(name = "end_date", description = "结束日期，格式：yyyy-MM-dd")
            String endDate) {

        return Mono.fromCallable(() -> {
            simulateDelay(100, 200);

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate start = LocalDate.parse(startDate, formatter);
                LocalDate end = LocalDate.parse(endDate, formatter);

                long days = Math.abs(ChronoUnit.DAYS.between(start, end));

                return String.format(
                        "日期差计算结果：\n" +
                                "起始日期: %s\n" +
                                "结束日期: %s\n" +
                                "相差天数: %d 天",
                        startDate, endDate, days
                );
            } catch (Exception e) {
                return String.format("错误: 日期格式无效，请使用 yyyy-MM-dd 格式。%s", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取日期对应的星期几
     *
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 星期几
     */
    @Tool(
            name = "get_day_of_week",
            description = "获取指定日期是星期几。"
    )
    public Mono<String> getDayOfWeek(
            @ToolParam(name = "date", description = "日期，格式：yyyy-MM-dd")
            String date) {

        return Mono.fromCallable(() -> {
            simulateDelay(50, 100);

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate localDate = LocalDate.parse(date, formatter);

                String dayOfWeek = localDate.getDayOfWeek()
                        .getDisplayName(java.time.format.TextStyle.FULL,
                                java.util.Locale.CHINA);

                return String.format(
                        "日期 %s 是：%s",
                        date, dayOfWeek
                );
            } catch (Exception e) {
                return String.format("错误: 日期格式无效，请使用 yyyy-MM-dd 格式。%s", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取当前时间（同步版本，简单场景使用）
     *
     * @param format 时间格式
     * @return 格式化后的时间字符串
     */
    @Tool(
            name = "get_current_time_simple",
            description = "获取当前时间的简单方法，不使用时区参数。"
    )
    public String getCurrentTimeSimple(
            @ToolParam(name = "format",
                    description = "时间格式，如：yyyy-MM-dd HH:mm:ss")
            String format) {

        LocalDateTime now = LocalDateTime.now();

        if (format == null || format.isBlank()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        try {
            return now.format(DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
            return "错误: 无效的时间格式";
        }
    }

    // ========== 私有辅助方法 ==========

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}