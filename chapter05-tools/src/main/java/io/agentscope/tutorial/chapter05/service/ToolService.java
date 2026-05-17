package io.agentscope.tutorial.chapter05.service;

import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.tutorial.chapter05.tool.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工具注册服务
 *
 * <p>负责初始化和管理 AgentScope 工具系统。
 * 在应用启动时自动注册所有工具，并配置工具分组。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Service
public class ToolService {

    private final Toolkit toolkit;

    public ToolService() {
        // 创建 Toolkit，配置为顺序执行
        ToolkitConfig config = ToolkitConfig.builder()
            .parallel(false)
            .allowToolDeletion(true)
            .build();

        this.toolkit = new Toolkit(config);

        // 初始化注册
        initializeTools();
    }

    /**
     * 获取工具包实例
     */
    public Toolkit getToolkit() {
        return toolkit;
    }

    /**
     * 初始化所有工具
     */
    private void initializeTools() {
        System.out.println("=== AgentScope 工具系统初始化 ===");

        // 1. 创建工具分组
        createToolGroups();

        // 2. 注册内置工具
        registerBuiltInTools();

        // 3. 注册自定义工具
        registerCustomTools();

        // 4. 注册元工具（使 Agent 可以动态管理工具分组）
        toolkit.registerMetaTool();

        // 5. 打印注册摘要
        printRegistrationSummary();
    }

    /**
     * 创建工具分组
     */
    private void createToolGroups() {
        // 默认激活的分组
        toolkit.createToolGroup("datetime", "日期时间工具", true);
        toolkit.createToolGroup("calculator", "计算器工具", true);
        toolkit.createToolGroup("builtin", "内置工具", true);

        // 默认不激活的分组（Agent 需要时可动态激活）
        toolkit.createToolGroup("weather", "天气查询工具", false);
        toolkit.createToolGroup("search", "搜索工具", false);
        toolkit.createToolGroup("http", "HTTP 请求工具", false);

        System.out.println("工具分组创建完成");
    }

    /**
     * 注册内置工具
     */
    private void registerBuiltInTools() {
        // 注册内置工具到 builtin 分组
        toolkit.registration()
            .tool(new BuiltinTools())
            .group("builtin")
            .apply();

        System.out.println("内置工具注册完成");
    }

    /**
     * 注册自定义工具
     */
    private void registerCustomTools() {
        // 注册计算器工具到 calculator 分组
        toolkit.registration()
            .tool(new CalculatorTool())
            .group("calculator")
            .apply();

        // 注册日期时间工具到 datetime 分组
        toolkit.registration()
            .tool(new DateTimeTool())
            .group("datetime")
            .apply();

        // 注册天气工具到 weather 分组
        toolkit.registration()
            .tool(new WeatherTool())
            .group("weather")
            .apply();

        // 注册搜索工具到 search 分组
        toolkit.registration()
            .tool(new SearchTool())
            .group("search")
            .apply();

        // 注册 HTTP 工具到 http 分组
        toolkit.registration()
            .tool(new HttpTool())
            .group("http")
            .apply();

        System.out.println("自定义工具注册完成");
    }

    /**
     * 激活工具分组
     *
     * @param groupNames 分组名称列表
     */
    public void activateGroups(List<String> groupNames) {
        toolkit.updateToolGroups(groupNames, true);
    }

    /**
     * 停用工具分组
     *
     * @param groupNames 分组名称列表
     */
    public void deactivateGroups(List<String> groupNames) {
        toolkit.updateToolGroups(groupNames, false);
    }

    /**
     * 获取当前活跃的分组
     */
    public List<String> getActiveGroups() {
        return toolkit.getActiveGroups();
    }

    /**
     * 获取已注册的工具列表
     */
    public List<String> getRegisteredTools() {
        return toolkit.getToolNames().stream().toList();
    }

    /**
     * 打印注册摘要
     */
    private void printRegistrationSummary() {
        System.out.println("\n=== 工具注册摘要 ===");
        System.out.println("总工具数: " + toolkit.getToolNames().size());
        System.out.println("活跃分组: " + toolkit.getActiveGroups());
        System.out.println("已注册工具: " + toolkit.getToolNames());
        System.out.println("元工具: reset_equipped_tools（已启用）");
        System.out.println("======================\n");
    }

    /**
     * 演示工具调用
     */
    public String demoToolCalling() {
        StringBuilder result = new StringBuilder();
        result.append("=== 工具调用演示 ===\n\n");

        // 演示计算器工具
        result.append("【计算器工具演示】\n");
        CalculatorTool calculator = new CalculatorTool();
        result.append("• add(10, 5) = ").append(calculator.add(10.0, 5.0)).append("\n");
        result.append("• factorial(5) = ").append(calculator.factorial(5)).append("\n");
        result.append("• is_prime(17) = ").append(calculator.isPrime(17)).append("\n\n");

        // 演示天气工具
        result.append("【天气工具演示】\n");
        WeatherTool weather = new WeatherTool();
        result.append(weather.getWeather("北京", null)).append("\n\n");

        // 演示日期时间工具
        result.append("【日期时间工具演示】\n");
        DateTimeTool dateTime = new DateTimeTool();
        result.append(dateTime.getCurrentDateTime("Asia/Shanghai", null)).append("\n");
        result.append(dateTime.getWeekday("2026-05-17")).append("\n");

        return result.toString();
    }
}