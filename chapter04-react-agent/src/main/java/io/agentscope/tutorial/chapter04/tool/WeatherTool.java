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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

/**
 * 天气查询工具集
 *
 * 本工具类提供模拟的天气查询功能，包括：
 * - 获取当前天气
 * - 获取天气预报
 * - 获取天气指数
 *
 * @author AgentScope Tutorial
 */
public class WeatherTool {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取指定城市的当前天气
     *
     * @param city 城市名称（中文或英文均可）
     * @return 当前天气信息，包括温度、天气状况、湿度等
     */
    @Tool(
            name = "get_current_weather",
            description = "获取指定城市的当前天气信息。适用于询问某个地方的实时天气状况。"
    )
    public Mono<String> getCurrentWeather(
            @ToolParam(name = "city", description = "城市名称，例如：北京、上海、Beijing")
            String city) {

        return Mono.fromCallable(() -> {
            // 模拟网络延迟
            simulateDelay(500, 1000);

            // 生成模拟天气数据
            String[] conditions = {"晴", "多云", "阴", "小雨", "雷阵雨"};
            String condition = conditions[RANDOM.nextInt(conditions.length)];
            int temperature = RANDOM.nextInt(35) + 5;  // 5-40°C
            int humidity = RANDOM.nextInt(60) + 40;    // 40-100%
            int windSpeed = RANDOM.nextInt(20) + 1;   // 1-21 m/s

            return String.format(
                    "【%s 当前天气】\n" +
                            "温度: %d°C\n" +
                            "天气: %s\n" +
                            "湿度: %d%%\n" +
                            "风速: %d m/s\n" +
                            "更新时间: %s",
                    city, temperature, condition, humidity, windSpeed,
                    LocalDateTime.now().format(DATE_FORMAT)
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取指定城市未来几天的天气预报
     *
     * @param city 城市名称
     * @param days 预报天数（1-7天）
     * @return 天气预报信息
     */
    @Tool(
            name = "get_weather_forecast",
            description = "获取指定城市未来几天的天气预报。适用于询问接下来几天的天气变化。"
    )
    public Mono<String> getWeatherForecast(
            @ToolParam(name = "city", description = "城市名称")
            String city,
            @ToolParam(name = "days", description = "预报天数，最大7天")
            int days) {

        return Mono.fromCallable(() -> {
            simulateDelay(800, 1500);

            // 限制天数范围
            days = Math.max(1, Math.min(days, 7));

            StringBuilder forecast = new StringBuilder();
            forecast.append(String.format("【%s 天气预报（未来%d天）】\n\n", city, days));

            String[] conditions = {"晴", "多云", "阴", "小雨", "中雨", "雷阵雨"};
            LocalDateTime date = LocalDateTime.now();

            for (int i = 0; i < days; i++) {
                date = date.plusDays(1);
                String condition = conditions[RANDOM.nextInt(conditions.length)];
                int highTemp = RANDOM.nextInt(15) + 20;  // 20-35°C
                int lowTemp = highTemp - RANDOM.nextInt(10) - 5;  // 比高温低5-15度

                forecast.append(String.format(
                        "日期: %s\n" +
                                "  天气: %s\n" +
                                "  温度: %d°C ~ %d°C\n\n",
                        date.format(DateTimeFormatter.ofPattern("MM月dd日 E")),
                        condition, lowTemp, highTemp
                ));
            }

            return forecast.toString();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取天气相关的生活指数
     *
     * @param city 城市名称
     * @param indexType 指数类型（可选值：穿衣、运动、旅游、感冒、紫外线）
     * @return 生活指数信息
     */
    @Tool(
            name = "get_weather_index",
            description = "获取天气相关的生活指数，帮助安排日常活动。"
    )
    public Mono<String> getWeatherIndex(
            @ToolParam(name = "city", description = "城市名称")
            String city,
            @ToolParam(name = "index_type",
                    description = "指数类型，可选值：穿衣、运动、旅游、感冒、紫外线")
            String indexType) {

        return Mono.fromCallable(() -> {
            simulateDelay(600, 1200);

            // 根据指数类型生成建议
            return switch (indexType) {
                case "穿衣" -> String.format(
                        "【%s 穿衣指数】\n" +
                                "建议指数: %s\n" +
                                "建议: 今日气温适中，建议穿着薄外套或长袖T恤，\n" +
                                "   早晚温差较大，可携带薄款外套备用。",
                        city, getRandomLevel()
                );
                case "运动" -> String.format(
                        "【%s 运动指数】\n" +
                                "建议指数: %s\n" +
                                "建议: %s",
                        city, getRandomLevel(),
                        RANDOM.nextBoolean() ? "适合户外运动" : "建议室内运动"
                );
                case "旅游" -> String.format(
                        "【%s 旅游指数】\n" +
                                "建议指数: %s\n" +
                                "建议: %s",
                        city, getRandomLevel(),
                        RANDOM.nextBoolean() ? "非常适合旅游" : "建议延期出行"
                );
                case "感冒" -> String.format(
                        "【%s 感冒指数】\n" +
                                "风险指数: %s\n" +
                                "建议: %s",
                        city, getRandomLevel(),
                        RANDOM.nextBoolean() ? "不易感冒" : "注意保暖，预防感冒"
                );
                case "紫外线" -> String.format(
                        "【%s 紫外线指数】\n" +
                                "强度指数: %s\n" +
                                "建议: %s",
                        city, getRandomLevel(),
                        RANDOM.nextBoolean() ? "请注意防晒" : "紫外线较弱，可不防晒"
                );
                default -> "错误: 不支持的指数类型 '" + indexType + "'";
            };
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取支持的指数类型列表
     *
     * @return 支持的所有指数类型
     */
    @Tool(
            name = "get_supported_index_types",
            description = "获取支持的天气指数类型列表，便于后续查询。"
    )
    public String getSupportedIndexTypes() {
        return "支持的天气指数类型：\n" +
                "  - 穿衣: 穿衣指数\n" +
                "  - 运动: 运动指数\n" +
                "  - 旅游: 旅游指数\n" +
                "  - 感冒: 感冒指数\n" +
                "  - 紫外线: 紫外线指数";
    }

    // ========== 私有辅助方法 ==========

    private String getRandomLevel() {
        String[] levels = {"一级（极适宜）", "二级（适宜）", "三级（较适宜）",
                "四级（较不适宜）", "五级（不适宜）"};
        return levels[RANDOM.nextInt(levels.length)];
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}