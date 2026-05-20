package io.agentscope.tutorial.chapter08.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synthesis Agent
 *
 * Responsible for synthesizing final responses from multiple specialized agents.
 *
 * Core responsibilities:
 * 1. Integrate output results from multiple agents
 * 2. Remove duplicate information
 * 3. Organize content in logical order
 * 4. Generate friendly final responses
 */
public class SynthesisAgent {

    private static final Logger log = LoggerFactory.getLogger(SynthesisAgent.class);

    private final Model model;

    public SynthesisAgent(Model model) {
        this.model = model;
        log.info("Synthesis agent initialized");
    }

    /**
     * Synthesize single agent result
     *
     * @param query Original user query
     * @param agentResult Specialized agent's processing result
     * @param category Agent category
     * @return Synthesized final response
     */
    public String synthesize(String query, String agentResult, String category) {
        log.info("Starting synthesis, category: {}", category);

        try {
            // Use LLM to synthesize result
            String prompt = buildSynthesisPrompt(query, agentResult, category);

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
                String result = extractTextContent(response);
                log.info("Synthesis completed");
                return result;
            }

        } catch (Exception e) {
            log.error("Synthesis failed, using original result", e);
        }

        // Return original result on synthesis failure
        return agentResult;
    }

    /**
     * Synthesize multiple agents' results (parallel scenario)
     *
     * @param query Original user query
     * @param results Multiple agents' processing results
     * @return Synthesized final response
     */
    public String synthesizeMultiple(String query, java.util.Map<String, String> results) {
        log.info("Starting multi-result synthesis, count: {}", results.size());

        try {
            StringBuilder allResults = new StringBuilder();
            results.forEach((agent, result) -> {
                allResults.append("【").append(agent).append("】\n")
                         .append(result).append("\n\n");
            });

            String prompt = """
                As an intelligent customer service center, please present the following multi-service processing results to the user.

                Original user query: %s

                Processing results from each service:
                %s

                Please:
                1. Integrate related information, avoid duplication
                2. Organize content in logical order
                3. Highlight the most important and user-focused information
                4. Ask if user needs further assistance

                Output the final response directly, no additional explanations.
                """.formatted(query, allResults.toString());

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
            log.error("Multi-result synthesis failed", e);
        }

        // Return simple concatenation on failure
        return results.values().stream()
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("Service temporarily unavailable");
    }

    /**
     * Build synthesis prompt
     */
    private String buildSynthesisPrompt(String query, String agentResult, String category) {
        String serviceName = getServiceName(category);
        String focusNote = getFocusNote(category);

        return """
            As an intelligent customer service center, please present the following processing results to the user in a friendly manner.

            Original user query: %s

            Processing service: %s

            Processing result:
            %s

            Notes:
            %s

            Please respond to the user in a concise and organized manner, highlighting key information and asking if other assistance is needed.

            Output the final response directly, no additional explanations.
            """.formatted(query, serviceName, agentResult, focusNote);
    }

    /**
     * Get service name
     */
    private String getServiceName(String category) {
        return switch (category) {
            case "ORDER_QUERY" -> "Order Query Service";
            case "ADDRESS_UPDATE" -> "Address Update Service";
            case "GENERAL_QUERY" -> "General Query Service";
            default -> "Customer Service";
        };
    }

    /**
     * Get focus notes
     */
    private String getFocusNote(String category) {
        return switch (category) {
            case "ORDER_QUERY" -> """
                - Clearly show order status
                - For in-transit orders, emphasize estimated delivery time
                - Provide suggestions if any anomalies""";
            case "ADDRESS_UPDATE" -> """
                - Show before/after address comparison
                - Confirm update success
                - Remind user to verify shipping information""";
            case "GENERAL_QUERY" -> """
                - Clearly show query results
                - Highlight important information like promotion validity
                - Provide next-step suggestions""";
            default -> "- Respond in a friendly and professional manner";
        };
    }

    /**
     * Extract text content from response
     */
    private String extractTextContent(Object response) {
        if (response instanceof io.agentscope.core.message.Msg msg) {
            return msg.getTextContent();
        }
        return response != null ? response.toString() : "";
    }
}