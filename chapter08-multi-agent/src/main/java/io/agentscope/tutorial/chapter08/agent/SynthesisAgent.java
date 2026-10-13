package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 合成代理（Synthesis Agent）
 *
 * <p>负责整合多个专业代理的输出结果，生成最终的用户回复。
 * 在多代理架构中，合成代理扮演着"监督者"角色，确保用户收到完整、连贯的回答。
 *
 * <h2>核心职责</h2>
 * <ol>
 *   <li>整合多个代理的输出结果</li>
 *   <li>去除重复信息</li>
 *   <li>按逻辑顺序组织内容</li>
 *   <li>生成友好的最终回复</li>
 * </ol>
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>监督者模式</b>：合成代理作为监督者，不直接处理用户请求，而是协调和整合其他代理的结果</li>
 *   <li><b>Model 直接调用</b>：与 ReActAgent 不同，合成代理直接使用 Model 进行 LLM 调用</li>
 *   <li><b>流式响应</b>：使用 model.stream() 方法获取流式响应，提高用户体验</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @see RouterAgent
 */
public class SynthesisAgent {

    private static final Logger log = LoggerFactory.getLogger(SynthesisAgent.class);

    /** 用于生成合成回复的语言模型 */
    private final Model model;

    /**
     * 构造函数
     *
     * @param model 语言模型实例，用于生成合成回复
     */
    public SynthesisAgent(Model model) {
        this.model = model;
        log.info("合成代理初始化完成");
    }

    /**
     * 合成单个代理的处理结果
     *
     * <p>将专业代理的原始输出转化为用户友好的最终回复。
     * 根据不同的服务类别（订单查询、地址更新、综合查询）采用不同的合成策略。
     *
     * @param query 用户原始查询
     * @param agentResult 专业代理的处理结果
     * @param category 代理类别（ORDER_QUERY/ADDRESS_UPDATE/GENERAL_QUERY）
     * @return 合成后的最终回复
     */
    public String synthesize(String query, String agentResult, String category) {
        log.info("开始合成结果，类别：{}", category);

        try {
            // 使用 LLM 合成结果
            // 构建合成提示词，包含原始查询、处理结果和服务类别信息
            String prompt = buildSynthesisPrompt(query, agentResult, category);

            // 调用 model.stream() 获取流式响应
            // blockLast() 会阻塞直到流结束，返回最后一个响应
            var response = model.stream(
                java.util.List.of(
                    io.agentscope.core.message.Msg.builder()
                        .role(io.agentscope.core.message.MsgRole.USER)
                        .content(io.agentscope.core.message.TextBlock.builder().text(prompt).build())
                        .build()
                ),
                null,  // 无系统提示词
                null   // 无额外配置
            ).blockLast(java.time.Duration.ofSeconds(60));

            if (response != null && response.getContent() != null) {
                String result = extractTextContent(response);
                log.info("合成完成");
                return result;
            }

        } catch (Exception e) {
            log.error("合成失败，使用原始结果", e);
        }

        // 合成失败时返回原始结果
        return agentResult;
    }

    /**
     * 合成多个代理的结果（并行场景）
     *
     * <p>当用户查询涉及多个方面时，可能需要调用多个专业代理并行处理。
     * 此方法将所有代理的结果整合成一个连贯的回复。
     *
     * @param query 用户原始查询
     * @param results 多个代理的处理结果，key 为代理名称，value 为处理结果
     * @return 合成后的最终回复
     */
    public String synthesizeMultiple(String query, java.util.Map<String, String> results) {
        log.info("开始多结果合成，结果数量：{}", results.size());

        try {
            // 将所有代理结果格式化为一个整体
            StringBuilder allResults = new StringBuilder();
            results.forEach((agent, result) -> {
                allResults.append("【").append(agent).append("】\n")
                         .append(result).append("\n\n");
            });

            // 构建多结果合成的提示词
            String prompt = """
                作为智能客户服务中心，请将以下多个服务的处理结果整合后呈现给用户。

                用户原始查询：%s

                各服务处理结果：
                %s

                请：
                1. 整合相关信息，避免重复
                2. 按逻辑顺序组织内容
                3. 突出用户最关心的重要信息
                4. 询问用户是否需要进一步帮助

                直接输出最终回复，无需额外解释。
                """.formatted(query, allResults.toString());

            // 调用模型生成合成回复
            var response = model.stream(
                java.util.List.of(
                    io.agentscope.core.message.Msg.builder()
                        .role(io.agentscope.core.message.MsgRole.USER)
                        .content(io.agentscope.core.message.TextBlock.builder().text(prompt).build())
                        .build()
                ),
                null,
                null
            ).blockLast(java.time.Duration.ofSeconds(60));

            if (response != null && response.getContent() != null) {
                return extractTextContent(response);
            }

        } catch (Exception e) {
            log.error("多结果合成失败", e);
        }

        // 失败时简单拼接返回
        return results.values().stream()
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("服务暂时不可用");
    }

    /**
     * 构建合成提示词
     *
     * <p>根据服务类别生成针对性的提示词，确保合成结果符合该类别的特点。
     *
     * @param query 用户原始查询
     * @param agentResult 代理处理结果
     * @param category 服务类别
     * @return 完整的提示词
     */
    private String buildSynthesisPrompt(String query, String agentResult, String category) {
        String serviceName = getServiceName(category);
        String focusNote = getFocusNote(category);

        return """
            作为智能客户服务中心，请将以下处理结果以友好的方式呈现给用户。

            用户原始查询：%s

            处理服务：%s

            处理结果：
            %s

            注意事项：
            %s

            请用简洁、有条理的方式回复用户，突出关键信息，并询问是否需要其他帮助。

            直接输出最终回复，无需额外解释。
            """.formatted(query, serviceName, agentResult, focusNote);
    }

    /**
     * 获取服务名称
     *
     * @param category 服务类别代码
     * @return 用户友好的服务名称
     */
    private String getServiceName(String category) {
        return switch (category) {
            case "ORDER_QUERY" -> "订单查询服务";
            case "ADDRESS_UPDATE" -> "地址更新服务";
            case "GENERAL_QUERY" -> "综合查询服务";
            default -> "客户服务";
        };
    }

    /**
     * 获取合成时的注意事项
     *
     * <p>根据不同服务类别，提供特定的注意事项，指导 LLM 生成更合适的回复。
     *
     * @param category 服务类别
     * @return 注意事项说明
     */
    private String getFocusNote(String category) {
        return switch (category) {
            case "ORDER_QUERY" -> """
                - 清晰展示订单状态
                - 对于运输中的订单，强调预计送达时间
                - 如有异常情况，给出适当建议""";
            case "ADDRESS_UPDATE" -> """
                - 展示更新前后的地址对比
                - 确认更新成功
                - 提醒用户确认收货信息""";
            case "GENERAL_QUERY" -> """
                - 清晰展示查询结果
                - 突出重要信息如优惠有效期
                - 提供下一步建议""";
            default -> "- 以友好、专业的方式回复";
        };
    }

    /**
     * 从响应中提取文本内容
     *
     * <p>AgentScope 2.x 中，{@code model.stream(...)} 返回 {@link io.agentscope.core.model.ChatResponse}
     * 元素，其内容是 {@link io.agentscope.core.message.ContentBlock} 列表。
     * 我们拼接所有 {@link io.agentscope.core.message.TextBlock} 的文本来获取纯文本回答。
     * 同时保留对 {@link io.agentscope.core.message.Msg} 的支持以提高兼容性。
     *
     * @param response 模型响应对象
     * @return 提取的文本内容
     */
    private String extractTextContent(Object response) {
        if (response instanceof io.agentscope.core.message.Msg msg) {
            return msg.getTextContent();
        }
        if (response instanceof io.agentscope.core.model.ChatResponse chatResponse) {
            if (chatResponse.getContent() == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (io.agentscope.core.message.ContentBlock block : chatResponse.getContent()) {
                if (block instanceof io.agentscope.core.message.TextBlock textBlock) {
                    sb.append(textBlock.getText());
                }
            }
            return sb.toString();
        }
        return response != null ? response.toString() : "";
    }
}