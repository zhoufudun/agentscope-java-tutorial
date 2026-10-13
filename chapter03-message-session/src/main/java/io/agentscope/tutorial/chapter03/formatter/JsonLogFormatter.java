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
package io.agentscope.tutorial.chapter03.formatter;

import io.agentscope.core.formatter.AbstractBaseFormatter;
import io.agentscope.core.message.*;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolSchema;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义 JSON 消息格式器 - 将 AgentScope Msg 转换为可读的 JSON 格式.
 *
 * <p>本类继承自 {@link AbstractBaseFormatter}，实现了消息的格式化和解析功能，
 * 主要用于日志记录、调试和对话导出。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>将 {@link Msg} 对象转换为 Map 结构，便于 JSON 序列化</li>
 *   <li>支持各种 {@link ContentBlock} 类型的转换（文本、图片、工具调用等）</li>
 *   <li>可选的美化输出和元数据包含</li>
 *   <li>将消息列表格式化为可读字符串或标准 JSON</li>
 * </ul>
 *
 * <h2>内容块类型支持</h2>
 * <ul>
 *   <li>{@link TextBlock} - 文本内容</li>
 *   <li>{@link ImageBlock} - 图片内容（URL 或 Base64）</li>
 *   <li>{@link ToolUseBlock} - 工具调用</li>
 *   <li>{@link ToolResultBlock} - 工具执行结果</li>
 *   <li>{@link ThinkingBlock} - Agent 的思考过程（日志中隐藏）</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 创建格式器（包含元数据，美化输出）
 * JsonLogFormatter formatter = new JsonLogFormatter(true, true);
 *
 * // 格式化消息列表
 * List<Msg> messages = ...;
 * String json = formatter.toJson(messages);
 *
 * // 或转换为可读字符串
 * String readable = formatter.formatMessagesToString(messages);
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 * @see AbstractBaseFormatter
 * @see Msg
 * @see ContentBlock
 */
public class JsonLogFormatter
        extends AbstractBaseFormatter<Map<String, Object>, Map<String, Object>, Map<String, Object>> {

    /**
     * 是否包含完整元数据.
     */
    private final boolean includeMetadata;

    /**
     * 是否美化输出（换行和缩进）.
     */
    private final boolean prettyPrint;

    /**
     * 创建默认格式器（不包含元数据，不美化）.
     */
    public JsonLogFormatter() {
        this(false, false);
    }

    /**
     * 创建自定义格式器.
     *
     * @param includeMetadata 是否在输出中包含消息的元数据
     * @param prettyPrint     是否美化输出（添加换行和缩进）
     */
    public JsonLogFormatter(boolean includeMetadata, boolean prettyPrint) {
        this.includeMetadata = includeMetadata;
        this.prettyPrint = prettyPrint;
    }

    /**
     * 格式化消息列表.
     *
     * <p>继承自 {@link AbstractBaseFormatter}，将 Msg 列表转换为 Map 列表。
     *
     * @param msgs 消息列表
     * @return Map 列表，每个 Map 代表一条消息
     */
    @Override
    protected List<Map<String, Object>> doFormat(List<Msg> msgs) {
        return msgs.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * 将单条 Msg 转换为 Map 结构.
     *
     * <p>转换后的 Map 包含以下字段：
     * <ul>
     *   <li>id - 消息ID</li>
     *   <li>role - 消息角色</li>
     *   <li>name - 发送者名称</li>
     *   <li>timestamp - 时间戳</li>
     *   <li>content - 内容块列表</li>
     *   <li>metadata - 元数据（可选）</li>
     * </ul>
     *
     * @param msg AgentScope 消息对象
     * @return 转换后的 Map，可用于 JSON 序列化或日志记录
     */
    public Map<String, Object> convertToMap(Msg msg) {
        // 使用 LinkedHashMap 保持字段顺序
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("role", msg.getRole().name());
        map.put("name", msg.getName());
        map.put("timestamp", msg.getTimestamp());

        // 转换所有内容块
        List<Map<String, Object>> contentList = new ArrayList<>();
        for (ContentBlock block : msg.getContent()) {
            contentList.add(convertBlockToMap(block));
        }
        map.put("content", contentList);

        // 可选：包含元数据
        if (includeMetadata && msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
            map.put("metadata", msg.getMetadata());
        }

        return map;
    }

    /**
     * 转换单个内容块为 Map.
     *
     * <p>根据内容块的类型进行不同的处理：
     * <ul>
     *   <li>TextBlock：提取文本内容</li>
     *   <li>ImageBlock：提取图片 URL 或 Base64 长度</li>
     *   <li>ToolUseBlock：提取工具名称、ID 和输入参数</li>
     *   <li>ToolResultBlock：提取执行结果和错误状态</li>
     *   <li>ThinkingBlock：隐藏具体思考内容（日志安全）</li>
     * </ul>
     *
     * @param block 内容块
     * @return 转换后的 Map
     */
    private Map<String, Object> convertBlockToMap(ContentBlock block) {
        Map<String, Object> blockMap = new LinkedHashMap<>();

        if (block instanceof TextBlock textBlock) {
            // 文本块：直接输出文本
            blockMap.put("type", "text");
            blockMap.put("text", textBlock.getText());
        } else if (block instanceof ImageBlock imageBlock) {
            // 图片块：输出来源信息
            blockMap.put("type", "image");
            blockMap.put("mediaType", "image/*");
            Source source = imageBlock.getSource();
            if (source instanceof URLSource urlSource) {
                blockMap.put("url", urlSource.getUrl());
            } else if (source instanceof Base64Source base64Source) {
                // 不输出完整的 Base64 数据，只显示长度
                blockMap.put("data", "[Base64 data, length=" + base64Source.getData().length() + "]");
            }
        } else if (block instanceof ToolUseBlock toolBlock) {
            // 工具调用块：输出工具信息
            blockMap.put("type", "tool_use");
            blockMap.put("id", toolBlock.getId());
            blockMap.put("name", toolBlock.getName());
            blockMap.put("input", toolBlock.getInput());
        } else if (block instanceof ToolResultBlock resultBlock) {
            // 工具结果块：输出执行结果
            blockMap.put("type", "tool_result");
            blockMap.put("toolCallId", resultBlock.getId());
            blockMap.put("name", resultBlock.getName());
            blockMap.put("output", convertBlockListToText(resultBlock.getOutput()));
            // 检查是否有错误标记
            if (resultBlock.getMetadata() != null) {
                blockMap.put("isError", resultBlock.getMetadata().containsKey("error"));
            }
        } else if (block instanceof ThinkingBlock thinkingBlock) {
            // 思考块：隐藏具体内容（可能包含敏感推理过程）
            blockMap.put("type", "thinking");
            blockMap.put("thinking", "[Thinking content hidden from log]");
        } else {
            // 其他类型：输出类名
            blockMap.put("type", block.getClass().getSimpleName());
        }

        return blockMap;
    }

    /**
     * 将内容块列表转换为文本列表.
     *
     * <p>只提取 TextBlock 类型的内容。
     *
     * @param blocks 内容块列表
     * @return 文本列表
     */
    private List<String> convertBlockListToText(List<ContentBlock> blocks) {
        if (blocks == null) return List.of();
        return blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.toList());
    }

    /**
     * 解析模型响应.
     *
     * <p>实现 {@link AbstractBaseFormatter} 的抽象方法，将 Map 格式的响应转换为 ChatResponse。
     *
     * @param response 响应 Map
     * @param startTime 请求开始时间（用于计算延迟）
     * @return ChatResponse 对象
     */
    @Override
    public ChatResponse parseResponse(Map<String, Object> response, Instant startTime) {
        Object content = response.get("content");
        String textContent = content instanceof String ? (String) content : String.valueOf(content);
        // 创建 TextBlock 作为响应内容
        TextBlock textBlock = TextBlock.builder().text(textContent != null ? textContent : "").build();
        return ChatResponse.builder().content(List.of(textBlock)).build();
    }

    /**
     * 应用生成选项到参数构建器.
     *
     * <p>实现 {@link AbstractBaseFormatter} 的抽象方法。
     *
     * @param paramsBuilder 参数构建器 Map
     * @param options 本次请求的选项
     * @param defaultOptions 默认选项
     */
    @Override
    public void applyOptions(Map<String, Object> paramsBuilder,
                              GenerateOptions options,
                              GenerateOptions defaultOptions) {
        if (options != null) {
            if (options.getTemperature() != null) {
                paramsBuilder.put("temperature", options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                paramsBuilder.put("max_tokens", options.getMaxTokens());
            }
        }
    }

    /**
     * 应用工具定义到参数构建器.
     *
     * <p>实现 {@link AbstractBaseFormatter} 的抽象方法。
     *
     * @param paramsBuilder 参数构建器 Map
     * @param tools 工具 schema 列表
     */
    @Override
    public void applyTools(Map<String, Object> paramsBuilder, List<ToolSchema> tools) {
        if (tools == null || tools.isEmpty()) {
            paramsBuilder.put("tools", List.of());
            return;
        }
        List<Map<String, Object>> toolList = tools.stream()
                .map(this::convertToolSchemaToMap)
                .collect(Collectors.toList());
        paramsBuilder.put("tools", toolList);
    }

    /**
     * 转换工具 schema 为 Map 格式.
     *
     * @param schema 工具 schema
     * @return Map 格式的工具定义
     */
    private Map<String, Object> convertToolSchemaToMap(ToolSchema schema) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "function");
        map.put("function", Map.of(
                "name", schema.getName(),
                "description", schema.getDescription() != null ? schema.getDescription() : "",
                "parameters", schema.getParameters() != null ? schema.getParameters() : Map.of()
        ));
        return map;
    }

    /**
     * 将消息列表格式化为可读字符串.
     *
     * <p>根据 prettyPrint 配置选择美化或紧凑格式。
     *
     * @param messages 消息列表
     * @return 格式化的字符串
     */
    public String formatMessagesToString(List<Msg> messages) {
        List<Map<String, Object>> maps = doFormat(messages);
        if (prettyPrint) {
            return formatPretty(maps);
        } else {
            return formatCompact(maps);
        }
    }

    /**
     * 美化输出格式.
     *
     * <p>添加换行和缩进，适合日志显示。
     *
     * @param maps Map 列表
     * @return 美化后的字符串
     */
    private String formatPretty(List<Map<String, Object>> maps) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < maps.size(); i++) {
            sb.append("  ").append(formatMapToString(maps.get(i)));
            if (i < maps.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 紧凑输出格式.
     *
     * <p>无换行和缩进，适合日志行。
     *
     * @param maps Map 列表
     * @return 紧凑格式的字符串
     */
    private String formatCompact(List<Map<String, Object>> maps) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < maps.size(); i++) {
            sb.append(formatMapToString(maps.get(i)));
            if (i < maps.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将单个 Map 转换为可读字符串.
     *
     * <p>对列表和嵌套 Map 进行简化显示。
     *
     * @param map 要格式化的 Map
     * @return 可读字符串
     */
    private String formatMapToString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String s) {
                sb.append("\"").append(s.replace("\"", "\\\"")).append("\"");
            } else if (value instanceof List<?> list) {
                sb.append("[").append(list.size()).append(" items]");
            } else if (value instanceof Map<?, ?> m) {
                sb.append("{").append(m.size()).append(" fields}");
            } else {
                sb.append(String.valueOf(value));
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转换为标准 JSON 字符串.
     *
     * <p>使用 Jackson ObjectMapper 进行 JSON 序列化。
     * 如果 Jackson 不可用，回退到简单格式。
     *
     * @param messages 消息列表
     * @return JSON 字符串
     */
    public String toJson(List<Msg> messages) {
        List<Map<String, Object>> maps = doFormat(messages);
        try {
            // 使用 Jackson 进行标准 JSON 序列化
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(maps);
        } catch (Exception e) {
            // 回退到简单格式
            return formatMessagesToString(messages);
        }
    }
}