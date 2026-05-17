package io.agentscope.tutorial.chapter05;

import io.agentscope.tutorial.chapter05.tool.CalculatorTool;
import io.agentscope.tutorial.chapter05.tool.DateTimeTool;
import io.agentscope.tutorial.chapter05.tool.WeatherTool;
import io.agentscope.tutorial.chapter05.tool.SearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具系统单元测试
 */
class ToolTest {

    private CalculatorTool calculator;
    private DateTimeTool dateTime;
    private WeatherTool weather;
    private SearchTool search;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorTool();
        dateTime = new DateTimeTool();
        weather = new WeatherTool();
        search = new SearchTool();
    }

    // ==================== CalculatorTool Tests ====================

    @Nested
    @DisplayName("计算器工具测试")
    class CalculatorTests {

        @Test
        @DisplayName("加法运算")
        void testAdd() {
            String result = calculator.add(10.0, 5.0);
            assertTrue(result.contains("15.00"));
        }

        @Test
        @DisplayName("减法运算")
        void testSubtract() {
            String result = calculator.subtract(10.0, 5.0);
            assertTrue(result.contains("5.00"));
        }

        @Test
        @DisplayName("乘法运算")
        void testMultiply() {
            String result = calculator.multiply(10.0, 5.0);
            assertTrue(result.contains("50.00"));
        }

        @Test
        @DisplayName("除法运算")
        void testDivide() {
            String result = calculator.divide(10.0, 2.0);
            assertTrue(result.contains("5.00"));
        }

        @Test
        @DisplayName("除数为零时返回错误")
        void testDivideByZero() {
            String result = calculator.divide(10.0, 0.0);
            assertTrue(result.contains("Error"));
            assertTrue(result.contains("零"));
        }

        @Test
        @DisplayName("阶乘计算 - 5! = 120")
        void testFactorial5() {
            String result = calculator.factorial(5);
            assertTrue(result.contains("120"));
        }

        @Test
        @DisplayName("阶乘计算 - 0! = 1")
        void testFactorial0() {
            String result = calculator.factorial(0);
            assertTrue(result.contains("1"));
        }

        @Test
        @DisplayName("阶乘计算 - 负数报错")
        void testFactorialNegative() {
            String result = calculator.factorial(-1);
            assertTrue(result.contains("Error"));
            assertTrue(result.contains("负数"));
        }

        @Test
        @DisplayName("阶乘计算 - 数字太大")
        void testFactorialTooLarge() {
            String result = calculator.factorial(21);
            assertTrue(result.contains("Error"));
            assertTrue(result.contains("太大"));
        }

        @Test
        @DisplayName("质数判断 - 17 是质数")
        void testIsPrimeTrue() {
            String result = calculator.isPrime(17);
            assertTrue(result.contains("质数"));
        }

        @Test
        @DisplayName("质数判断 - 15 不是质数")
        void testIsPrimeFalse() {
            String result = calculator.isPrime(15);
            // "不是质数" 包含 "是质数" 子串，所以改用检查 "不是质数" 和 "可以被"
            assertTrue(result.contains("不是质数"));
            assertTrue(result.contains("可以被"));
        }

        @Test
        @DisplayName("平方根计算")
        void testSqrt() {
            String result = calculator.sqrt(16.0);
            assertTrue(result.contains("4"));
        }

        @Test
        @DisplayName("平方根计算 - 负数报错")
        void testSqrtNegative() {
            String result = calculator.sqrt(-16.0);
            assertTrue(result.contains("Error"));
        }

        @Test
        @DisplayName("幂运算计算")
        void testPower() {
            String result = calculator.power(2.0, 10.0);
            assertTrue(result.contains("1024"));
        }

        @Test
        @DisplayName("平均值计算")
        void testAverage() {
            String result = calculator.average("[1, 2, 3, 4, 5]");
            assertTrue(result.contains("3.00"));
            assertTrue(result.contains("5"));
        }

        @Test
        @DisplayName("平均值计算 - 空列表")
        void testAverageEmpty() {
            String result = calculator.average("[]");
            assertTrue(result.contains("Error"));
        }
    }

    // ==================== DateTimeTool Tests ====================

    @Nested
    @DisplayName("日期时间工具测试")
    class DateTimeTests {

        @Test
        @DisplayName("获取当前时间")
        void testGetCurrentDateTime() {
            String result = dateTime.getCurrentDateTime(null, null);
            assertNotNull(result);
            assertTrue(result.contains("当前时间"));
        }

        @Test
        @DisplayName("指定时区获取时间")
        void testGetCurrentDateTimeWithTimezone() {
            String result = dateTime.getCurrentDateTime("Asia/Shanghai", null);
            assertTrue(result.contains("Asia/Shanghai"));
        }

        @Test
        @DisplayName("无效时区返回错误")
        void testInvalidTimezone() {
            String result = dateTime.getCurrentDateTime("Invalid/Timezone", null);
            assertTrue(result.contains("Error"));
        }

        @Test
        @DisplayName("计算日期差")
        void testDateDiff() {
            String result = dateTime.dateDiff("2026-01-01", "2026-01-15", null);
            assertTrue(result.contains("14"));
            assertTrue(result.contains("天"));
        }

        @Test
        @DisplayName("日期计算 - 加天数")
        void testDateAdd() {
            String result = dateTime.dateAdd("2026-05-17", 7, "days");
            assertTrue(result.contains("2026-05-24"));
        }

        @Test
        @DisplayName("日期计算 - 减天数")
        void testDateSubtract() {
            String result = dateTime.dateAdd("2026-05-17", -5, "days");
            assertTrue(result.contains("2026-05-12"));
        }

        @Test
        @DisplayName("获取星期几")
        void testGetWeekday() {
            String result = dateTime.getWeekday("2026-05-17");
            assertTrue(result.contains("星期日") || result.contains("周日") || result.contains("日"));
        }

        @Test
        @DisplayName("格式化时间戳")
        void testFormatTimestamp() {
            String result = dateTime.formatTimestamp(1700000000L, null, null);
            assertNotNull(result);
            assertTrue(result.contains("时间戳"));
        }

        @Test
        @DisplayName("获取时区列表")
        void testListTimezones() {
            String result = dateTime.listTimezones();
            assertTrue(result.contains("Asia/Shanghai"));
            assertTrue(result.contains("America/New_York"));
        }
    }

    // ==================== WeatherTool Tests ====================

    @Nested
    @DisplayName("天气工具测试")
    class WeatherTests {

        @Test
        @DisplayName("查询北京天气")
        void testGetWeatherBeijing() {
            String result = weather.getWeather("北京", null);
            assertTrue(result.contains("北京"));
            assertTrue(result.contains("温度"));
            assertTrue(result.contains("天气"));
        }

        @Test
        @DisplayName("查询天气 - 华氏度")
        void testGetWeatherFahrenheit() {
            String result = weather.getWeather("北京", "fahrenheit");
            assertTrue(result.contains("北京"));
            assertTrue(result.contains("°F") || result.contains("F"));
        }

        @Test
        @DisplayName("查询不支持的城市")
        void testGetWeatherUnknownCity() {
            String result = weather.getWeather("不存在城市", null);
            assertTrue(result.contains("抱歉") || result.contains("不支持"));
        }

        @Test
        @DisplayName("查询天气预报")
        void testGetForecast() {
            String result = weather.getForecast("北京", 3);
            assertTrue(result.contains("北京"));
            assertTrue(result.contains("天气预报"));
        }

        @Test
        @DisplayName("查询空气质量")
        void testGetAirQuality() {
            String result = weather.getAirQuality("北京");
            assertTrue(result.contains("北京"));
            assertTrue(result.contains("空气质量"));
        }

        @Test
        @DisplayName("获取穿衣建议")
        void testGetDressingAdvice() {
            String result = weather.getDressingAdvice("北京", null);
            assertTrue(result.contains("北京"));
            assertTrue(result.contains("穿衣建议"));
        }
    }

    // ==================== SearchTool Tests ====================

    @Nested
    @DisplayName("搜索工具测试")
    class SearchTests {

        @Test
        @DisplayName("基本搜索")
        void testSearch() {
            String result = search.search("java", 5);
            assertTrue(result.contains("搜索结果"));
            assertTrue(result.contains("java"));
        }

        @Test
        @DisplayName("搜索结果限制")
        void testSearchWithLimit() {
            String result = search.search("python", 2);
            assertTrue(result.contains("搜索结果"));
        }

        @Test
        @DisplayName("搜索空关键词")
        void testSearchEmptyQuery() {
            assertThrows(IllegalArgumentException.class, () -> {
                search.search("", 10);
            });
        }

        @Test
        @DisplayName("搜索图片")
        void testSearchImages() {
            String result = search.searchImages("风景", 5);
            assertTrue(result.contains("图片搜索结果"));
            assertTrue(result.contains("风景"));
        }

        @Test
        @DisplayName("搜索新闻")
        void testSearchNews() {
            String result = search.searchNews("科技", 7);
            assertTrue(result.contains("新闻搜索结果"));
            assertTrue(result.contains("科技"));
        }

        @Test
        @DisplayName("获取搜索建议")
        void testGetSearchSuggestions() {
            String result = search.getSearchSuggestions("java");
            assertTrue(result.contains("搜索建议"));
            assertTrue(result.contains("java"));
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("综合测试")
    class IntegrationTests {

        @Test
        @DisplayName("多工具组合使用")
        void testMultipleTools() {
            // 计算
            String calcResult = calculator.add(100.0, 50.0);
            assertTrue(calcResult.contains("150.00"));

            // 日期
            String dateResult = dateTime.getWeekday("2026-05-17");
            assertNotNull(dateResult);

            // 天气
            String weatherResult = weather.getWeather("上海", null);
            assertTrue(weatherResult.contains("上海"));
        }

        @Test
        @DisplayName("参数验证测试")
        void testParameterValidation() {
            // 空参数
            assertThrows(IllegalArgumentException.class, () -> {
                calculator.add(null, 5.0);
            });

            // 负数阶乘
            String result = calculator.factorial(-5);
            assertTrue(result.contains("Error"));
        }
    }
}