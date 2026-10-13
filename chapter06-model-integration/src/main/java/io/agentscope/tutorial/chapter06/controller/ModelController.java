package io.agentscope.tutorial.chapter06.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter06.entity.ChatConfig;
import io.agentscope.tutorial.chapter06.entity.ModelProvider;
import io.agentscope.tutorial.chapter06.repository.ChatConfigRepository;
import io.agentscope.tutorial.chapter06.repository.ModelProviderRepository;
import io.agentscope.tutorial.chapter06.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 模型管理控制器
 *
 * <p>提供模型管理、切换和聊天的 REST API。这是本章的核心控制器，
 * 演示了 AgentScope Java 的多模型集成能力。
 *
 * <p><b>核心功能</b>：
 * <ul>
 *   <li><b>模型管理</b> - 注册、移除、查询模型</li>
 *   <li><b>单模型聊天</b> - 指定模型进行对话</li>
 *   <li><b>多模型对比</b> - 同一问题发给多个模型比较回答</li>
 *   <li><b>配置管理</b> - 保存和加载聊天配置</li>
 * </ul>
 *
 * <p><b>API 端点分组</b>：
 * <ul>
 *   <li>/api/models/providers - 模型提供商管理</li>
 *   <li>/api/models/chat - 聊天功能</li>
 *   <li>/api/models/compare - 多模型对比</li>
 *   <li>/api/models/config - 配置管理</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Slf4j
public class ModelController {

    /** 模型服务 */
    private final ModelService modelService;

    /** 模型提供商仓库 */
    private final ModelProviderRepository providerRepository;

    /** 聊天配置仓库 */
    private final ChatConfigRepository configRepository;

    // ==================== 模型管理 API ====================

    /**
     * 获取所有支持的模型提供商
     *
     * <p>返回所有已启用的提供商及其注册的模型列表。
     * 结果按优先级排序（priority 越小越靠前）。
     *
     * <p><b>响应示例</b>：
     * <pre>{@code
     * [
     *   {
     *     "name": "dashscope",
     *     "displayName": "阿里云百炼 (DashScope)",
     *     "models": ["qwen-plus", "qwen-max", "qwen-turbo"]
     *   }
     * ]
     * }</pre>
     *
     * @return 提供商列表，包含各提供商的详细信息和模型
     */
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> getProviders() {
        List<ModelProvider> providers = providerRepository.findByEnabledTrueOrderByPriorityAsc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ModelProvider provider : providers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", provider.getId());
            map.put("name", provider.getName());
            map.put("displayName", provider.getDisplayName());
            map.put("enabled", provider.getEnabled());
            map.put("priority", provider.getPriority());
            map.put("baseUrl", provider.getBaseUrl());
            map.put("remark", provider.getRemark());

            // 获取该提供商的所有已注册模型
            List<String> modelNames = modelService.getModelNamesByProvider(provider.getName());
            map.put("models", modelNames);

            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取指定提供商的所有模型
     *
     * @param provider 提供商名称（dashscope/openai/ollama/gemini）
     * @return 该提供商的模型名称列表
     */
    @GetMapping("/providers/{provider}/models")
    public ResponseEntity<List<String>> getModelsByProvider(@PathVariable String provider) {
        List<String> modelNames = modelService.getModelNamesByProvider(provider);
        return ResponseEntity.ok(modelNames);
    }

    /**
     * 注册新模型
     *
     * <p>动态注册一个新的模型到系统中。注册后即可用于聊天。
     *
     * <p><b>请求示例</b>：
     * <pre>{@code
     * POST /api/models/register
     * {
     *   "provider": "dashscope",
     *   "modelName": "qwen-vl-plus"
     * }
     * }</pre>
     *
     * @param request 包含 provider 和 modelName 的请求体
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerModel(
            @RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String modelName = request.get("modelName");

        if (provider == null || modelName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "provider 和 modelName 不能为空"
            ));
        }

        try {
            // 根据提供商类型调用对应的注册方法
            switch (provider.toLowerCase()) {
                case "dashscope" -> modelService.registerDashScopeModel(modelName);
                case "ollama" -> modelService.registerOllamaModel(modelName);
                case "gemini" -> modelService.registerGeminiModel(modelName);
                case "openai" -> modelService.registerOpenAIModel(
                        modelName,
                        "https://api.openai.com/v1",
                        new io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter()
                );
                default -> {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "不支持的提供商: " + provider
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "模型注册成功",
                    "modelKey", provider + ":" + modelName
            ));
        } catch (Exception e) {
            log.error("模型注册失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "注册失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 移除已注册的模型
     *
     * @param request 包含 provider 和 modelName 的请求体
     * @return 操作结果
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeModel(
            @RequestBody Map<String, String> request) {
        String provider = request.get("provider");
        String modelName = request.get("modelName");

        if (provider == null || modelName == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "provider 和 modelName 不能为空"
            ));
        }

        modelService.removeModel(provider, modelName);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "模型移除成功"
        ));
    }

    // ==================== 聊天 API ====================

    /**
     * 单模型聊天（非流式）
     *
     * <p>向指定模型发送消息并等待完整响应。
     * 适用于不需要实时显示的场景。
     *
     * <p><b>请求示例</b>：
     * <pre>{@code
     * POST /api/models/chat
     * {
     *   "provider": "dashscope",
     *   "modelName": "qwen-plus",
     *   "message": "什么是 AgentScope？",
     *   "temperature": 0.7,
     *   "maxTokens": 2000
     * }
     * }</pre>
     *
     * @param request 聊天请求
     * @return 模型回复和使用统计
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request) {
        // 解析请求参数
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = request.containsKey("maxTokens")
                ? ((Number) request.get("maxTokens")).intValue() : 2000;

        // 参数校验
        if (provider == null || modelName == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "provider、modelName 和 message 不能为空"
            ));
        }

        // 获取模型实例
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "未找到模型: " + provider + ":" + modelName
            ));
        }

        try {
            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();

            // 构建生成选项
            GenerateOptions options = GenerateOptions.builder()
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            // 执行聊天（使用 stream API 但阻塞等待完整结果）
            ChatResponse response = model.stream(List.of(userMsg), null, options)
                    .blockLast(java.time.Duration.ofSeconds(60));

            if (response != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "model", modelName,
                        "provider", provider,
                        "response", extractText(response),
                        "usage", response.getUsage()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未收到响应"
                ));
            }
        } catch (Exception e) {
            log.error("聊天请求失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "请求失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 流式聊天
     *
     * <p>使用响应式流实现实时输出。适用于需要即时反馈的交互场景。
     *
     * @param request 聊天请求
     * @return 响应式事件流
     */
    @PostMapping("/chat/stream")
    public Flux<Map<String, Object>> streamChat(
            @RequestBody Map<String, Object> request) {
        String provider = (String) request.get("provider");
        String modelName = (String) request.get("modelName");
        String message = (String) request.get("message");
        Double temperature = request.containsKey("temperature")
                ? ((Number) request.get("temperature")).doubleValue() : 0.7;

        // 获取模型
        Model model = modelService.getModel(provider, modelName);
        if (model == null) {
            return Flux.just(Map.of(
                    "error", true,
                    "message", "未找到模型"
            ));
        }

        // 构建消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        GenerateOptions options = GenerateOptions.builder()
                .temperature(temperature)
                .build();

        // 返回流式响应
        return model.stream(List.of(userMsg), null, options)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("content", extractText(response));
                    result.put("done", isResponseDone(response));
                    if (response.getUsage() != null) {
                        result.put("usage", response.getUsage());
                    }
                    return result;
                });
    }

    // ==================== 模型对比 API ====================

    /**
     * 多模型对比
     *
     * <p>将同一问题发送给多个模型，比较它们的回答。
     * 用于评估不同模型的能力差异。
     *
     * <p><b>请求示例</b>：
     * <pre>{@code
     * POST /api/models/compare
     * {
     *   "models": [
     *     {"provider": "dashscope", "modelName": "qwen-plus"},
     *     {"provider": "openai", "modelName": "gpt-4o"}
     *   ],
     *   "message": "解释什么是机器学习",
     *   "systemPrompt": "你是一个技术专家"
     * }
     * }</pre>
     *
     * @param request 对比请求，包含模型列表和问题
     * @return 各模型的回答和耗时统计
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareModels(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> modelList = (List<Map<String, String>>) request.get("models");
        String message = (String) request.get("message");
        String systemPrompt = (String) request.getOrDefault("systemPrompt",
                "You are a helpful AI assistant.");

        // 参数校验
        if (modelList == null || modelList.isEmpty() || message == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "models 和 message 不能为空"
            ));
        }

        List<Map<String, Object>> results = new ArrayList<>();

        // 遍历每个模型进行调用
        for (Map<String, String> modelConfig : modelList) {
            String provider = modelConfig.get("provider");
            String modelName = modelConfig.get("modelName");

            Map<String, Object> result = new HashMap<>();
            result.put("provider", provider);
            result.put("modelName", modelName);

            try {
                long startTime = System.currentTimeMillis();

                Model model = modelService.getModel(provider, modelName);
                if (model == null) {
                    result.put("success", false);
                    result.put("error", "未找到模型");
                    results.add(result);
                    continue;
                }

                // 构建消息列表（包含系统提示和用户消息）
                List<Msg> messages = List.of(
                        Msg.builder()
                                .role(MsgRole.SYSTEM)
                                .content(TextBlock.builder().text(systemPrompt).build())
                                .build(),
                        Msg.builder()
                                .role(MsgRole.USER)
                                .content(TextBlock.builder().text(message).build())
                                .build()
                );

                GenerateOptions options = GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(1000)
                        .build();

                // 执行调用
                ChatResponse response = model.stream(messages, null, options)
                        .blockLast(java.time.Duration.ofSeconds(60));

                long duration = System.currentTimeMillis() - startTime;

                if (response != null) {
                    result.put("success", true);
                    result.put("response", extractText(response));
                    result.put("duration", duration);  // 耗时（毫秒）
                    if (response.getUsage() != null) {
                        result.put("usage", response.getUsage());
                    }
                } else {
                    result.put("success", false);
                    result.put("error", "未收到响应");
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            results.add(result);
        }

        return ResponseEntity.ok(Map.of(
                "question", message,
                "results", results
        ));
    }

    // ==================== 配置管理 API ====================

    /**
     * 保存聊天配置
     *
     * <p>将聊天参数保存为配置，便于复用。
     *
     * @param config 配置对象
     * @return 保存结果
     */
    @PostMapping("/config/save")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody ChatConfig config) {
        try {
            if (config.getId() == null) {
                config.setCreateTime(java.time.LocalDateTime.now());
            }
            config.setUpdateTime(java.time.LocalDateTime.now());

            ChatConfig saved = configRepository.save(config);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", saved
            ));
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取所有聊天配置
     *
     * @return 配置列表
     */
    @GetMapping("/config/list")
    public ResponseEntity<List<ChatConfig>> listConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    /**
     * 获取默认配置
     *
     * @return 默认配置（如果存在）
     */
    @GetMapping("/config/default")
    public ResponseEntity<Optional<ChatConfig>> getDefaultConfig() {
        return ResponseEntity.ok(configRepository.findByIsDefaultTrue());
    }

    /**
     * 设置默认配置
     *
     * <p>将指定配置设为默认，同时取消其他配置的默认标记。
     *
     * @param id 配置 ID
     * @return 操作结果
     */
    @PostMapping("/config/set-default/{id}")
    public ResponseEntity<Map<String, Object>> setDefaultConfig(@PathVariable Long id) {
        // 清除当前的默认配置
        configRepository.findByIsDefaultTrue().ifPresent(config -> {
            config.setIsDefault(false);
            configRepository.save(config);
        });

        // 设置新的默认配置
        configRepository.findById(id).ifPresent(config -> {
            config.setIsDefault(true);
            configRepository.save(config);
        });

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 删除配置
     *
     * @param id 配置 ID
     * @return 操作结果
     */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        configRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 ChatResponse 中提取文本内容
     *
     * @param response 聊天响应
     * @return 文本内容
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (io.agentscope.core.message.ContentBlock block : response.getContent()) {
            if (block instanceof io.agentscope.core.message.TextBlock textBlock) {
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }

    /**
     * 判断响应是否完成
     *
     * @param response 聊天响应
     * @return 是否完成
     */
    private boolean isResponseDone(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return false;
        }
        return response.getContent().stream()
                .anyMatch(block -> block instanceof io.agentscope.core.message.TextBlock);
    }
}