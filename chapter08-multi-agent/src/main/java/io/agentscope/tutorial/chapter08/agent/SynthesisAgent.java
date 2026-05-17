package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 合成代理（Synthesis Agent）
 *
 * 负责将多个专业代理的处理结果合成为最终回复。
 *
 * 核心职责：
 * 1. 整合多个代理的输出结果
 * 2. 去除重复信息
 * 3. 按逻辑顺序组织内容
 * 4. 生成友好的最终回复
 */
public class SynthesisAgent {

    private static final Logger log = LoggerFactory.getLogger(SynthesisAgent.class);

    private final Model model;

    public SynthesisAgent(Model model) {
        this.model = model;
        log.info("合成代理初始化完成");
    }

    /**
     * 合成单个代理的结果
     *
     * @param query 原始用户查询
     * @param agentResult 专业代理的处理结果
     * @param category 代理类别
     * @return 合成后的最终回复
     */
    public String synthesize(String query, String agentResult, String category) {
        log.info("开始合成结果，类别：{}", category);

        try {
            // 使用 LLM 合成结果
            String prompt = buildSynthesisPrompt(query, agentResult, category);

            var response = model.generate(
                java.util.List.of(
                    io.agentscope.core.message.Msg.of(
                        io.agentscope.core.message.MsgRole.USER,
                        io.agentscope.core.message.TextBlock.of(prompt)
                    )
                )
            ).block();

            if (response != null && response.getContent() != null) {
                String result = extractTextContent(response);
                log.info("结果合成完成");
                return result;
            }

        } catch (Exception e) {
            log.error("结果合成失败，使用原始结果", e);
        }

        // 合成失败时返回原始结果
        return agentResult;
    }

    /**
     * 合成多个代理的结果（并行场景）
     *
     * @param query 原始用户查询
     * @param results 多个代理的处理结果
     * @return 合成后的最终回复
     */
    public String synthesizeMultiple(String query, java.util.Map<String, String> results) {
        log.info("开始合成多个结果，数量：{}", results.size());

        try {
            StringBuilder allResults = new StringBuilder();
            results.forEach((agent, result) -> {
                allResults.append("【").append(agent).append("】\n")
                         .append(result).append("\n\n");
            });

            String prompt = """
                作为智能客户服务中心，请将以下多个服务的处理结果整合后呈现给用户。

                用户原始查询：%s

                各服务处理结果：
                %s

                请：
                1. 整合相关信息，避免重复
                2. 按逻辑顺序组织内容
                3. 突出最重要和用户最关注的信息
                4. 询问用户是否需要进一步帮助

                直接输出最终回复，不要有额外说明。
                """.formatted(query, allResults.toString());

            var response = model.generate(
                java.util.List.of(
                    io.agentscope.core.message.Msg.of(
                        io.agentscope.core.message.MsgRole.USER,
                        io.agentscope.core.message.TextBlock.of(prompt)
                    )
                )
            ).block();

            if (response != null && response.getContent() != null) {
                return extractTextContent(response);
            }

        } catch (Exception e) {
            log.error("多结果合成失败", e);
        }

        // 返回简单的拼接结果
        return results.values().stream()
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("服务暂时不可用");
    }

    /**
     * 构建合成提示词
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

            直接输出最终回复，不要有额外说明。
            """.formatted(query, serviceName, agentResult, focusNote);
    }

    /**
     * 获取服务名称
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
     * 获取关注点说明
     */
    private String getFocusNote(String category) {
        return switch (category) {
            case "ORDER_QUERY" -> """
                - 清晰展示订单状态
                - 对于运输中订单，强调预计送达时间
                - 如有异常，给出建议""";
            case "ADDRESS_UPDATE" -> """
                - 展示更新前后的地址对比
                - 确认更新成功
                - 提醒用户核对收货信息""";
            case "GENERAL_QUERY" -> """
                - 清晰展示查询结果
                - 突出优惠有效期等重要信息
                - 给出下一步行动建议""";
            default -> "- 用友好专业的语气回复";
        };
    }

    /**
     * 从响应中提取文本内容
     */
    private String extractTextContent(Object response) {
        if (response instanceof io.agentscope.core.message.Msg msg) {
            return msg.getTextContent();
        }
        return response != null ? response.toString() : "";
    }
}