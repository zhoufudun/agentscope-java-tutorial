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
 * 自定义 JSON 消息格式器
 *
 * 将 AgentScope Msg 转换为友好可读的 JSON 格式
 * 用于日志记录和调试
 *
 * @author AgentScope Tutorial
 */
public class JsonLogFormatter
        extends AbstractBaseFormatter<Map<String, Object>, Map<String, Object>, Map<String, Object>> {

    /** 是否包含完整元数据 */
    private final boolean includeMetadata;

    /** 是否美化输出 */
    private final boolean prettyPrint;

    public JsonLogFormatter() {
        this(false, false);
    }

    /**
     * 创建格式器
     *
     * @param includeMetadata 是否包含元数据
     * @param prettyPrint     是否美化输出
     */
    public JsonLogFormatter(boolean includeMetadata, boolean prettyPrint) {
        this.includeMetadata = includeMetadata;
        this.prettyPrint = prettyPrint;
    }

    @Override
    protected List<Map<String, Object>> doFormat(List<Msg> msgs) {
        return msgs.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * 将 Msg 转换为 Map（用于日志和存储）
     *
     * @param msg AgentScope 消息对象
     * @return 转换后的 Map
     */
    public Map<String, Object> convertToMap(Msg msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("role", msg.getRole().name());
        map.put("name", msg.getName());
        map.put("timestamp", msg.getTimestamp());

        // 转换内容块
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
     * 转换内容块为 Map
     */
    private Map<String, Object> convertBlockToMap(ContentBlock block) {
        Map<String, Object> blockMap = new LinkedHashMap<>();

        if (block instanceof TextBlock textBlock) {
            blockMap.put("type", "text");
            blockMap.put("text", textBlock.getText());
        } else if (block instanceof ImageBlock imageBlock) {
            blockMap.put("type", "image");
            blockMap.put("mediaType", "image/*");
            Source source = imageBlock.getSource();
            if (source instanceof URLSource urlSource) {
                blockMap.put("url", urlSource.getUrl());
            } else if (source instanceof Base64Source base64Source) {
                blockMap.put("data", "[Base64 data, length=" + base64Source.getData().length() + "]");
            }
        } else if (block instanceof ToolUseBlock toolBlock) {
            blockMap.put("type", "tool_use");
            blockMap.put("id", toolBlock.getId());
            blockMap.put("name", toolBlock.getName());
            blockMap.put("input", toolBlock.getInput());
        } else if (block instanceof ToolResultBlock resultBlock) {
            blockMap.put("type", "tool_result");
            blockMap.put("toolCallId", resultBlock.getId());
            blockMap.put("name", resultBlock.getName());
            blockMap.put("output", convertBlockListToText(resultBlock.getOutput()));
            // 检查是否包含错误标记
            if (resultBlock.getMetadata() != null) {
                blockMap.put("isError", resultBlock.getMetadata().containsKey("error"));
            }
        } else if (block instanceof ThinkingBlock thinkingBlock) {
            blockMap.put("type", "thinking");
            blockMap.put("thinking", "[Thinking content hidden from log]");
        } else {
            blockMap.put("type", block.getClass().getSimpleName());
        }

        return blockMap;
    }

    /**
     * 将内容块列表转换为文本列表
     */
    private List<String> convertBlockListToText(List<ContentBlock> blocks) {
        if (blocks == null) return List.of();
        return blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.toList());
    }

    @Override
    public ChatResponse parseResponse(Map<String, Object> response, Instant startTime) {
        Object content = response.get("content");
        String textContent = content instanceof String ? (String) content : String.valueOf(content);
        // 使用 TextBlock 作为 ContentBlock
        TextBlock textBlock = TextBlock.builder().text(textContent != null ? textContent : "").build();
        return ChatResponse.builder().content(List.of(textBlock)).build();
    }

    @Override
    public void applyOptions(Map<String, Object> paramsBuilder,
                              GenerateOptions options,
                              GenerateOptions defaultOptions) {
        // 应用生成选项到参数构建器
        if (options != null) {
            if (options.getTemperature() != null) {
                paramsBuilder.put("temperature", options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                paramsBuilder.put("max_tokens", options.getMaxTokens());
            }
        }
    }

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
     * 转换工具 schema 为 Map
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
     * 将消息列表格式化为可读字符串
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
     * 美化输出格式
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
     * 紧凑输出格式
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
     * 将 Map 转换为可读字符串
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
     * 转换为 JSON 字符串
     */
    public String toJson(List<Msg> messages) {
        List<Map<String, Object>> maps = doFormat(messages);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(maps);
        } catch (Exception e) {
            return formatMessagesToString(messages);
        }
    }
}