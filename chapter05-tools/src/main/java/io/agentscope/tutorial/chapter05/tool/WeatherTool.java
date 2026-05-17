package io.agentscope.tutorial.chapter05.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天气查询工具
 *
 * <p>模拟天气查询服务，支持全球主要城市的天气查询。
 * 在实际应用中，可替换为真实天气 API（如 OpenWeatherMap、中国气象局 API 等）。
 *
 * <p>注意：这是一个模拟实现，返回的数据是虚构的，仅用于演示目的。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class WeatherTool {

    // 模拟的天气数据（实际应用中替换为 API 调用）
    private static final Map<String, WeatherInfo> WEATHER_DB = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    static {
        // 初始化模拟天气数据
        WEATHER_DB.put("北京", new WeatherInfo("北京", 15, "晴", "优", "北风 3-4级", 45));
        WEATHER_DB.put("上海", new WeatherInfo("上海", 18, "多云", "良", "东风 2-3级", 60));
        WEATHER_DB.put("广州", new WeatherInfo("广州", 25, "晴", "优", "南风 2级", 70));
        WEATHER_DB.put("深圳", new WeatherInfo("深圳", 26, "晴", "优", "南风 1-2级", 75));
        WEATHER_DB.put("杭州", new WeatherInfo("杭州", 16, "阴", "良", "东北风 2级", 65));
        WEATHER_DB.put("成都", new WeatherInfo("成都", 14, "小雨", "良", "北风 1-2级", 80));
        WEATHER_DB.put("武汉", new WeatherInfo("武汉", 17, "多云", "良", "东风 3级", 55));
        WEATHER_DB.put("西安", new WeatherInfo("西安", 12, "晴", "优", "西风 2级", 40));
        WEATHER_DB.put("南京", new WeatherInfo("南京", 15, "阴", "良", "东风 2-3级", 60));
        WEATHER_DB.put("重庆", new WeatherInfo("重庆", 20, "晴", "良", "西风 1级", 50));
        WEATHER_DB.put("东京", new WeatherInfo("东京", 20, "晴", "优", "东南风 2级", 50));
        WEATHER_DB.put("纽约", new WeatherInfo("纽约", 12, "阴", "中", "西北风 3级", 65));
        WEATHER_DB.put("伦敦", new WeatherInfo("伦敦", 10, "小雨", "良", "西风 4级", 85));
        WEATHER_DB.put("巴黎", new WeatherInfo("巴黎", 14, "多云", "良", "西南风 3级", 70));
        WEATHER_DB.put("新加坡", new WeatherInfo("新加坡", 31, "雷阵雨", "中", "东南风 2级", 90));
    }

    // ==================== 天气查询 ====================

    /**
     * 查询当前天气
     *
     * <p>返回指定城市的当前天气信息，包括温度、天气状况、空气质量等。
     *
     * @param city 城市名称（支持中英文）
     * @param unit 温度单位（celsius/fahrenheit），默认 celsius
     * @return 天气信息字符串
     */
    @Tool(
        name = "get_weather",
        description = "查询指定城市的当前天气信息。包括温度、天气状况、空气质量、风力等详细信息。"
    )
    public String getWeather(
        @ToolParam(
            name = "city",
            description = "城市名称（支持中英文，如'北京'、'Shanghai'）",
            required = true
        ) String city,
        @ToolParam(
            name = "unit",
            description = "温度单位：celsius（摄氏度）或 fahrenheit（华氏度）",
            required = false
        ) String unit
    ) {
        validateNotEmpty(city, "city");

        WeatherInfo weather = WEATHER_DB.get(city);
        if (weather == null) {
            // 尝试模糊匹配
            return findSimilarCity(city);
        }

        return formatWeatherResponse(weather, unit);
    }

    /**
     * 查询天气预报
     *
     * @param city 城市名称
     * @param days 预报天数（1-7），默认 3
     * @return 天气预报字符串
     */
    @Tool(
        name = "get_forecast",
        description = "查询指定城市未来几天的天气预报"
    )
    public String getForecast(
        @ToolParam(name = "city", description = "城市名称", required = true) String city,
        @ToolParam(
            name = "days",
            description = "预报天数（1-7天）",
            required = false
        ) Integer days
    ) {
        validateNotEmpty(city, "city");

        int forecastDays = (days != null && days >= 1 && days <= 7) ? days : 3;

        WeatherInfo baseWeather = WEATHER_DB.get(city);
        if (baseWeather == null) {
            return findSimilarCity(city);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("📅 %s 天气预报（未来 %d 天）：\n\n", city, forecastDays));

        LocalDateTime now = LocalDateTime.now();
        String[] conditions = {"晴", "多云", "阴", "小雨", "雷阵雨"};

        for (int i = 0; i < forecastDays; i++) {
            LocalDateTime date = now.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM月dd日 E"));

            // 模拟数据波动
            int tempVariation = RANDOM.nextInt(6) - 3;
            double temp = baseWeather.temperature + tempVariation;
            String condition = conditions[RANDOM.nextInt(conditions.length)];

            result.append(String.format("【%s】%s 🌡️ %.0f~%.0f°C 💧 %d%%\n",
                dateStr,
                condition,
                temp - 3,
                temp + 3,
                baseWeather.humidity + RANDOM.nextInt(20) - 10));
        }

        return result.toString();
    }

    /**
     * 查询空气质量
     *
     * @param city 城市名称
     * @return 空气质量信息
     */
    @Tool(
        name = "get_air_quality",
        description = "查询指定城市的空气质量指数（AQI）和详细空气质量报告"
    )
    public String getAirQuality(
        @ToolParam(name = "city", description = "城市名称", required = true) String city
    ) {
        validateNotEmpty(city, "city");

        WeatherInfo weather = WEATHER_DB.get(city);
        if (weather == null) {
            return findSimilarCity(city);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("🌫️ %s 空气质量报告\n\n", city));
        result.append(String.format("• 空气质量等级：%s\n", weather.airQuality));
        result.append(String.format("• 湿度：%d%%\n", weather.humidity));
        result.append(String.format("• 风力：%s\n", weather.wind));

        // AQI 等级说明
        String aqiDesc = switch (weather.airQuality) {
            case "优" -> "空气质量令人满意，基本无空气污染";
            case "良" -> "空气质量可接受，某些污染物可能对极少数敏感人群有影响";
            case "中" -> "易感人群症状有轻度加剧，健康人群出现刺激症状";
            case "差" -> "进一步加剧，易感人群症状可能加剧，对心脏、呼吸系统有影响";
            default -> "数据获取中";
        };
        result.append(String.format("• 说明：%s\n", aqiDesc));

        return result.toString();
    }

    /**
     * 获取穿衣建议
     *
     * <p>根据天气情况给出穿衣建议
     *
     * @param city 城市名称
     * @param unit 温度单位（可选）
     * @return 穿衣建议
     */
    @Tool(
        name = "get_dressing_advice",
        description = "根据当前天气情况给出穿衣建议，帮助用户合理搭配衣物"
    )
    public String getDressingAdvice(
        @ToolParam(name = "city", description = "城市名称", required = true) String city,
        @ToolParam(
            name = "unit",
            description = "温度单位（可选，默认摄氏度）",
            required = false
        ) String unit
    ) {
        validateNotEmpty(city, "city");

        WeatherInfo weather = WEATHER_DB.get(city);
        if (weather == null) {
            return findSimilarCity(city);
        }

        double temp = weather.temperature;
        if ("fahrenheit".equalsIgnoreCase(unit)) {
            temp = temp * 9 / 5 + 32;
        }

        StringBuilder advice = new StringBuilder();
        advice.append(String.format("👔 %s 穿衣建议（当前 %.0f°C）：\n\n", city, temp));

        // 根据温度给出建议
        if (temp < 0) {
            advice.append("🥶 极寒天气，建议穿：\n");
            advice.append("   - 羽绒服/厚棉服\n");
            advice.append("   - 保暖内衣、毛衣\n");
            advice.append("   - 棉裤、羽绒服内胆\n");
            advice.append("   - 围巾、手套、帽子、棉鞋\n");
        } else if (temp < 10) {
            advice.append("❄️ 寒冷天气，建议穿：\n");
            advice.append("   - 大衣或薄羽绒服\n");
            advice.append("   - 毛衣、保暖内衣\n");
            advice.append("   - 牛仔裤、保暖裤\n");
            advice.append("   - 围巾、手套\n");
        } else if (temp < 15) {
            advice.append("🌤️ 较冷天气，建议穿：\n");
            advice.append("   - 夹克、薄外套\n");
            advice.append("   - 薄毛衣、长袖\n");
            advice.append("   - 牛仔裤、休闲裤\n");
            advice.append("   - 可带围巾\n");
        } else if (temp < 20) {
            advice.append("🍃 凉爽天气，建议穿：\n");
            advice.append("   - 长袖衬衫、薄外套\n");
            advice.append("   - 牛仔裤、休闲裤\n");
            advice.append("   - 舒适的鞋子\n");
        } else if (temp < 25) {
            advice.append("☀️ 温暖天气，建议穿：\n");
            advice.append("   - T恤、短袖\n");
            advice.append("   - 薄裤子、裙子\n");
            advice.append("   - 运动鞋\n");
            advice.append("   - 注意防晒\n");
        } else if (temp < 30) {
            advice.append("🌡️ 较热天气，建议穿：\n");
            advice.append("   - 短袖、短裤\n");
            advice.append("   - 轻薄衣物\n");
            advice.append("   - 凉鞋、拖鞋\n");
            advice.append("   - 注意防暑降温\n");
        } else {
            advice.append("🔥 炎热天气，建议穿：\n");
            advice.append("   - 超薄短袖、短裤\n");
            advice.append("   - 透气性好的衣物\n");
            advice.append("   - 凉鞋、人字拖\n");
            advice.append("   - 强烈建议：多喝水，避免长时间户外活动\n");
        }

        // 根据天气状况补充建议
        if (weather.condition.contains("雨")) {
            advice.append("\n🌧️ 今日有雨，记得带伞！\n");
        }
        if (weather.condition.contains("雪")) {
            advice.append("\n❄️ 有雪，注意防滑！\n");
        }
        if (weather.wind.contains("大风")) {
            advice.append("\n💨 风力较大，注意防风！\n");
        }

        return advice.toString();
    }

    // ==================== 辅助方法 ====================

    private void validateNotEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("参数 '" + paramName + "' 不能为空");
        }
    }

    private String formatWeatherResponse(WeatherInfo weather, String unit) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("🌤️ %s 当前天气\n\n", weather.city));
        result.append(String.format("• 温度：%.0f°C", weather.temperature));
        if ("fahrenheit".equalsIgnoreCase(unit)) {
            double fahrenheit = weather.temperature * 9 / 5 + 32;
            result.append(String.format("（%.0f°F）", fahrenheit));
        }
        result.append("\n");
        result.append(String.format("• 天气：%s\n", weather.condition));
        result.append(String.format("• 空气质量：%s\n", weather.airQuality));
        result.append(String.format("• 湿度：%d%%\n", weather.humidity));
        result.append(String.format("• 风力：%s\n", weather.wind));

        // 更新时间戳
        String updateTime = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        result.append(String.format("\n🕐 更新时间：%s", updateTime));

        return result.toString();
    }

    private String findSimilarCity(String city) {
        // 尝试模糊匹配
        for (Map.Entry<String, WeatherInfo> entry : WEATHER_DB.entrySet()) {
            if (entry.getKey().toLowerCase().contains(city.toLowerCase()) ||
                city.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return formatWeatherResponse(entry.getValue(), null);
            }
        }
        return String.format("😢 抱歉，暂不支持查询 '%s' 的天气。\n支持的热门城市：北京、上海、广州、深圳、杭州、成都、武汉、西安、南京、重庆、东京、纽约、伦敦、巴黎、新加坡", city);
    }

    // ==================== 内部类：天气信息 ====================

    /**
     * 天气信息数据结构
     */
    private static class WeatherInfo {
        final String city;
        final double temperature;
        final String condition;
        final String airQuality;
        final String wind;
        final int humidity;

        WeatherInfo(String city, double temperature, String condition,
                    String airQuality, String wind, int humidity) {
            this.city = city;
            this.temperature = temperature;
            this.condition = condition;
            this.airQuality = airQuality;
            this.wind = wind;
            this.humidity = humidity;
        }
    }
}