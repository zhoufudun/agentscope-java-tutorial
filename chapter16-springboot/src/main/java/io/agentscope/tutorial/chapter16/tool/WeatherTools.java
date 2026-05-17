/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter16.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 天气查询工具.
 *
 * <p>演示如何创建自定义工具并注册到 Agent。
 * 实际应用中应调用真实天气 API（如和风天气、心知天气等）。
 *
 * <p>使用方式：
 * <pre>{@code
 * Agent: "北京今天天气怎么样？"
 * -> 调用 get_weather(city="北京")
 * <- 返回天气信息
 * }</pre>
 */
public class WeatherTools {

    private static final Logger log = LoggerFactory.getLogger(WeatherTools.class);

    /**
     * 获取指定城市的天气信息
     *
     * @param city 城市名称（中文）
     * @return 天气信息
     */
    @Tool(
            name = "get_weather",
            description = "获取指定城市的当前天气信息，包括温度、湿度、风力等。当用户询问某个城市的天气时调用此工具。"
    )
    public String getWeather(
            @ToolParam(name = "city", description = "城市名称（中文），如：北京、上海、纽约") String city) {

        log.info("查询天气: city={}", city);

        // 模拟天气数据（实际应用中应调用外部 API）
        return getMockWeatherData(city);
    }

    /**
     * 获取模拟天气数据
     *
     * <p>在实际应用中，替换为真实天气 API 调用。
     * 推荐使用：心知天气 API、和风天气 API、中国天气网 API 等。
     */
    private String getMockWeatherData(String city) {
        return String.format("""
            {
              "city": "%s",
              "weather": "晴",
              "temperature": "25°C",
              "humidity": "45%%",
              "wind": "东南风 3级",
              "aqi": "良",
              "tips": "今日天气良好，适合外出活动"
            }
            """, city);
    }
}