package io.agentscope.tutorial.chapter05.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 日期时间工具集
 *
 * <p>提供日期时间查询、转换、计算等功能。
 * 支持多种时区和日期格式。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class DateTimeTool {

    // ==================== 时间查询 ====================

    /**
     * 获取当前日期时间
     *
     * @param timezone 时区（如 "Asia/Shanghai", "America/New_York", "Europe/London"）
     * @param format 日期格式（如 "yyyy-MM-dd HH:mm:ss"）
     * @return 格式化后的日期时间字符串
     */
    @Tool(
        name = "get_current_datetime",
        description = "获取指定时区的当前日期时间，支持自定义格式输出"
    )
    public String getCurrentDateTime(
        @ToolParam(
            name = "timezone",
            description = "时区标识（如 'Asia/Shanghai', 'America/New_York', 'Europe/London'）",
            required = false
        ) String timezone,
        @ToolParam(
            name = "format",
            description = "输出格式（如 'yyyy-MM-dd HH:mm:ss'），省略则使用默认格式",
            required = false
        ) String format
    ) {
        ZoneId zoneId = ZoneId.system默认();
        if (timezone != null && !timezone.isBlank()) {
            try {
                zoneId = ZoneId.of(timezone);
            } catch (Exception e) {
                return String.format("Error: 无效的时区 '%s'。请使用标准的 IANA 时区标识，如 'Asia/Shanghai'。", timezone);
            }
        }

        LocalDateTime now = LocalDateTime.now(zoneId);
        DateTimeFormatter formatter;
        if (format != null && !format.isBlank()) {
            try {
                formatter = DateTimeFormatter.ofPattern(format);
            } catch (Exception e) {
                return String.format("Error: 无效的日期格式 '%s'。", format);
            }
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }

        return String.format("📅 当前时间（%s）：%s", zoneId.getId(), now.format(formatter));
    }

    /**
     * 计算日期差
     *
     * @param startDate 起始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @param unit 计算单位（days/weeks/months/years）
     * @return 日期差结果
     */
    @Tool(
        name = "date_diff",
        description = "计算两个日期之间的差值，支持按天、周、月、年计算"
    )
    public String dateDiff(
        @ToolParam(name = "start_date", description = "起始日期（格式：yyyy-MM-dd）", required = true) String startDate,
        @ToolParam(name = "end_date", description = "结束日期（格式：yyyy-MM-dd）", required = true) String endDate,
        @ToolParam(
            name = "unit",
            description = "计算单位：days（天）/ weeks（周）/ months（月）/ years（年）",
            required = false
        ) String unit
    ) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            long days = ChronoUnit.DAYS.between(start, end);

            String result = String.format("📊 日期差计算\n");
            result += String.format("• 起始日期：%s\n", startDate);
            result += String.format("• 结束日期：%s\n", endDate);
            result += String.format("• 相差天数：%d 天\n", days);

            if (unit == null || unit.isBlank() || "days".equalsIgnoreCase(unit)) {
                return result;
            }

            switch (unit.toLowerCase()) {
                case "weeks" -> result += String.format("• 相差周数：%.1f 周\n", days / 7.0);
                case "months" -> result += String.format("• 相差月数：%.1f 月\n", days / 30.0);
                case "years" -> result += String.format("• 相差年数：%.2f 年\n", days / 365.0);
                default -> result += "Error: 无效的单位 '" + unit + "'。请使用 days/weeks/months/years。";
            }

            return result;
        } catch (Exception e) {
            return String.format("Error: 日期格式错误，请使用 yyyy-MM-dd 格式，如 '2024-05-20'。详细：%s", e.getMessage());
        }
    }

    /**
     * 日期计算
     *
     * @param date 基准日期（格式：yyyy-MM-dd）
     * @param amount 加减数量（正数加，负数减）
     * @param unit 单位（days/weeks/months/years）
     * @return 计算后的日期
     */
    @Tool(
        name = "date_add",
        description = "对指定日期进行加/减运算，得到新的日期"
    )
    public String dateAdd(
        @ToolParam(name = "date", description = "基准日期（格式：yyyy-MM-dd）", required = true) String date,
        @ToolParam(name = "amount", description = "加/减的数量（正数加，负数减）", required = true) Integer amount,
        @ToolParam(name = "unit", description = "单位：days / weeks / months / years", required = true) String unit
    ) {
        try {
            LocalDate baseDate = LocalDate.parse(date);
            LocalDate resultDate;

            switch (unit.toLowerCase()) {
                case "days" -> resultDate = baseDate.plusDays(amount);
                case "weeks" -> resultDate = baseDate.plusWeeks(amount);
                case "months" -> resultDate = baseDate.plusMonths(amount);
                case "years" -> resultDate = baseDate.plusYears(amount);
                default -> {
                    return String.format("Error: 无效的单位 '%s'。请使用 days/weeks/months/years。", unit);
                }
            }

            return String.format("📅 日期计算\n• 基准日期：%s\n• 运算：%s %s\n• 结果：%s",
                date,
                (amount >= 0 ? "+" : "") + amount,
                unit,
                resultDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            return String.format("Error: %s", e.getMessage());
        }
    }

    /**
     * 获取星期几
     *
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 星期信息
     */
    @Tool(
        name = "get_weekday",
        description = "查询指定日期是星期几"
    )
    public String getWeekday(
        @ToolParam(name = "date", description = "日期（格式：yyyy-MM-dd）", required = true) String date
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            String weekday = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE);
            int dayNumber = dayOfWeek.getValue(); // 1=Monday, 7=Sunday

            StringBuilder result = new StringBuilder();
            result.append(String.format("📅 %s 是 %s\n", date, weekday));

            // 判断是否是周末
            if (dayNumber == 6 || dayNumber == 7) {
                result.append("🎉 今天是周末！\n");
            } else if (dayNumber == 1) {
                result.append("📌 今天是周一（周一综合征？保持好心情！）\n");
            }

            return result.toString();
        } catch (Exception e) {
            return String.format("Error: 日期格式错误，请使用 yyyy-MM-dd 格式。%s", e.getMessage());
        }
    }

    /**
     * 格式化时间戳
     *
     * @param timestamp Unix 时间戳（秒或毫秒）
     * @param format 输出格式
     * @param timezone 时区
     * @return 格式化后的时间
     */
    @Tool(
        name = "format_timestamp",
        description = "将 Unix 时间戳转换为可读的日期时间格式"
    )
    public String formatTimestamp(
        @ToolParam(name = "timestamp", description = "Unix 时间戳（秒或毫秒）", required = true) Long timestamp,
        @ToolParam(
            name = "format",
            description = "输出格式（如 'yyyy-MM-dd HH:mm:ss'）",
            required = false
        ) String format,
        @ToolParam(
            name = "timezone",
            description = "时区",
            required = false
        ) String timezone
    ) {
        try {
            // 判断是秒还是毫秒
            long millis = timestamp > 1_000_000_000_000L ? timestamp : timestamp * 1000;

            ZoneId zoneId = ZoneId.system默认();
            if (timezone != null && !timezone.isBlank()) {
                zoneId = ZoneId.of(timezone);
            }

            Instant instant = Instant.ofEpochMilli(millis);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);

            DateTimeFormatter formatter = format != null && !format.isBlank()
                ? DateTimeFormatter.ofPattern(format)
                : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return String.format("⏰ 时间戳 %d 对应时间：%s", timestamp, dateTime.format(formatter));
        } catch (Exception e) {
            return String.format("Error: 无效的时间戳或格式错误。%s", e.getMessage());
        }
    }

    /**
     * 获取时区列表（常用）
     *
     * @return 常用时区列表
     */
    @Tool(
        name = "list_timezones",
        description = "返回常用的时区列表供选择"
    )
    public String listTimezones() {
        String[] timezones = {
            "Asia/Shanghai",    // 北京时间
            "Asia/Tokyo",       // 东京时间
            "Asia/Hong_Kong",   // 香港时间
            "Asia/Singapore",   // 新加坡时间
            "Asia/Seoul",       // 首尔时间
            "America/New_York",  // 纽约时间（东部）
            "America/Los_Angeles", // 洛杉矶时间（太平洋）
            "America/Chicago",   // 芝加哥时间（中部）
            "Europe/London",    // 伦敦时间
            "Europe/Paris",     // 巴黎时间
            "Europe/Berlin",    // 柏林时间
            "Australia/Sydney", // 悉尼时间
            "Pacific/Auckland"  // 奥克兰时间
        };

        StringBuilder result = new StringBuilder();
        result.append("🌍 常用时区列表：\n\n");

        ZoneId systemZone = ZoneId.system默认();
        LocalDateTime now = LocalDateTime.now();

        for (String tz : timezones) {
            LocalDateTime tzTime = LocalDateTime.now(ZoneId.of(tz));
            String timeStr = tzTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            String diff = calculateOffset(now, tzTime);

            String marker = tz.equals(systemZone.getId()) ? " ⭐（系统时区）" : "";
            result.append(String.format("• %-20s %s %s%s\n", tz, timeStr, diff, marker));
        }

        return result.toString();
    }

    private String calculateOffset(LocalDateTime local, LocalDateTime target) {
        long diffMinutes = java.time.Duration.between(local, target).toMinutes();
        if (diffMinutes == 0) return "(同区)";
        return String.format("(UTC%+d)", diffMinutes / 60);
    }
}