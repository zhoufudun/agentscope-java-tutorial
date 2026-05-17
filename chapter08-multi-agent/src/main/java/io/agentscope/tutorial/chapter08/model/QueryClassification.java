package io.agentscope.tutorial.chapter08.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 查询分类结果
 *
 * 记录路由代理的分类决策：哪个类型的查询应该由哪个专业代理处理
 */
public record QueryClassification(
    @JsonProperty("category") String category,
    @JsonProperty("reason") String reason
) {

    @JsonCreator
    public QueryClassification {}

    /** 查询类型常量 */
    public static final String ORDER_QUERY = "order_query";
    public static final String ADDRESS_UPDATE = "address_update";
    public static final String GENERAL_QUERY = "general_query";
    public static final String UNKNOWN = "unknown";

    /** 查询类型对应的 Pipeline 节点名称 */
    public String toPipelineNode() {
        return switch (category) {
            case ORDER_QUERY -> "order_query_pipeline";
            case ADDRESS_UPDATE -> "address_update_pipeline";
            case GENERAL_QUERY -> "general_query_pipeline";
            default -> "unknown";
        };
    }
}