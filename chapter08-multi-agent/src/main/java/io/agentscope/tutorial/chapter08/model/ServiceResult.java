package io.agentscope.tutorial.chapter08.model;

import java.util.List;
import java.util.Map;

/**
 * 服务中心返回结果
 *
 * 包含完整的处理结果：原始查询、分类决策、各 Pipeline 输出和最终汇总
 */
public record ServiceResult(
    String originalQuery,           // 原始查询
    QueryClassification classify,   // 分类结果
    Map<String, String> pipelineOutputs,  // 各 Pipeline 的输出
    String finalAnswer,            // 最终汇总答案
    List<String> agentsInvoked    // 被调用的 Agent 列表
) {

    /**
     * 便捷构造函数
     */
    public static ServiceResult of(
            String query,
            QueryClassification classify,
            String finalAnswer) {
        return new ServiceResult(
            query,
            classify,
            Map.of(),
            finalAnswer,
            List.of(classify.category())
        );
    }

    /**
     * 添加 Pipeline 输出
     */
    public ServiceResult withPipelineOutput(String pipeline, String output) {
        return new ServiceResult(
            originalQuery,
            classify,
            Map.of(pipeline, output),
            finalAnswer,
            agentsInvoked
        );
    }
}