package io.agentscope.tutorial.chapter05.controller;

import io.agentscope.tutorial.chapter05.service.ToolService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具管理控制器
 *
 * <p>提供工具分组管理、工具列表查询等 REST API。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    /**
     * 获取工具列表
     */
    @GetMapping
    public Map<String, Object> getTools() {
        return Map.of(
            "tools", toolService.getRegisteredTools(),
            "activeGroups", toolService.getActiveGroups(),
            "totalCount", toolService.getRegisteredTools().size()
        );
    }

    /**
     * 获取活跃的分组列表
     */
    @GetMapping("/groups")
    public Map<String, Object> getGroups() {
        return Map.of(
            "activeGroups", toolService.getActiveGroups()
        );
    }

    /**
     * 激活工具分组
     *
     * @param groups 分组名称列表
     */
    @PostMapping("/activate")
    public Map<String, Object> activateGroups(@RequestBody List<String> groups) {
        toolService.activateGroups(groups);
        return Map.of(
            "success", true,
            "message", "工具分组已激活",
            "activeGroups", toolService.getActiveGroups()
        );
    }

    /**
     * 停用工具分组
     *
     * @param groups 分组名称列表
     */
    @PostMapping("/deactivate")
    public Map<String, Object> deactivateGroups(@RequestBody List<String> groups) {
        toolService.deactivateGroups(groups);
        return Map.of(
            "success", true,
            "message", "工具分组已停用",
            "activeGroups", toolService.getActiveGroups()
        );
    }

    /**
     * 工具调用演示
     */
    @GetMapping("/demo")
    public Map<String, Object> demoTools() {
        return Map.of(
            "success", true,
            "result", toolService.demoToolCalling()
        );
    }
}