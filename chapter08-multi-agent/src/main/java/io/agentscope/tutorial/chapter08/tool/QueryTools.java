package io.agentscope.tutorial.chapter08.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 综合查询工具类（模拟实现）
 *
 * <p>提供商品搜索、优惠活动查询、地址更新等综合功能。
 * 这些工具展示了 AgentScope 中工具调用的多种场景。
 *
 * <h2>AgentScope 学习要点</h2>
 * <ul>
 *   <li><b>工具分组</b>：可以在一个类中定义多个相关的工具方法</li>
 *   <li><b>参数验证</b>：工具描述应清晰说明参数格式和约束</li>
 *   <li><b>返回格式</b>：工具返回的文本应结构化，便于 LLM 理解和处理</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
public class QueryTools {

    private static final Logger log = LoggerFactory.getLogger(QueryTools.class);

    /**
     * 默认构造函数
     */
    public QueryTools() {}

    // ==================== 商品搜索 ====================

    /**
     * 搜索商品
     *
     * <p>演示如何实现一个商品搜索工具。实际应用中会对接商品服务 API。
     *
     * @param keyword 搜索关键词
     * @return 商品列表
     */
    @Tool(
        name = "search_products",
        description = "搜索商品或商品信息。当用户询问商品推荐或价格时使用此工具。"
    )
    public static String searchProducts(
            @ToolParam(name = "keyword", description = "搜索关键词")
            String keyword) {

        log.info("搜索商品，关键词：{}", keyword);

        return """
            搜索结果（关键词：%s）：
            1. [热销] 智能蓝牙耳机 Pro - ¥299.00 - 98%% 好评
            2. [新品] 无线充电器 15W - ¥89.00 - 95%% 好评
            3. [特价] iPhone 15 手机壳 - ¥39.00 - 92%% 好评
            """.formatted(keyword);
    }

    // ==================== 优惠查询 ====================

    /**
     * 查询可用优惠活动
     *
     * <p>演示如何查询用户可用的优惠活动。
     *
     * @param userId 用户 ID
     * @return 可用优惠活动列表
     */
    @Tool(
        name = "query_promotions",
        description = "查询当前可用的优惠活动。当用户询问折扣或特价信息时使用此工具。"
    )
    public static String queryPromotions(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询优惠活动，用户 ID：{}", userId);

        return """
            当前可用优惠：
            1. 新用户首单立减 ¥50（限未下单用户）
            2. 满200减30（有效期至 2024-01-31）
            3. 会员日9折券（每月18日可用）
            4. 积分兑换（100积分 = ¥1）
            5. [限时] 冬季好物低至8折
            """;
    }

    // ==================== 账户信息 ====================

    /**
     * 查询账户信息
     *
     * <p>演示如何查询用户的账户相关信息，包括积分、优惠券、会员等级等。
     *
     * @param userId 用户 ID
     * @return 账户相关信息
     */
    @Tool(
        name = "query_account_info",
        description = "查询用户账户信息，包括积分、优惠券、会员等级等。当用户询问积分或优惠券时使用此工具。"
    )
    public static String queryAccountInfo(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询账户信息，用户 ID：{}", userId);

        return """
            账户信息：
            会员等级：黄金会员
            当前积分：2,580 积分
            可用优惠券：3 张
              - 满200减20券（有效期至 2024-01-31）
              - 满100减10券（有效期至 2024-02-15）
              - 会员日9折券（每月18日可用）
            """;
    }

    // ==================== 地址管理 ====================

    /**
     * 获取当前收货地址
     *
     * <p>演示如何查询用户的默认收货地址。通常在地址更新前先调用此方法。
     *
     * @param userId 用户 ID
     * @return 当前收货地址信息
     */
    @Tool(
        name = "get_shipping_address",
        description = "获取用户当前的默认收货地址。在更新地址前先查询当前地址。"
    )
    public static String getShippingAddress(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询收货地址，用户 ID：{}", userId);

        return """
            当前收货地址：
            收货人：张三
            手机号：138****5678
            省/市/区：浙江省 杭州市 西湖区
            详细地址：文三路123号 XX小区1幢1单元1001室
            邮编：310000
            """;
    }

    /**
     * 更新收货地址
     *
     * <p>演示如何实现一个带多个参数的工具。
     * LLM 会从用户的自然语言描述中提取所有必要的地址信息。
     *
     * @param userId 用户 ID
     * @param province 省份
     * @param city 城市
     * @param district 区/县
     * @param detailAddress 详细地址
     * @param recipient 收货人姓名
     * @param phone 手机号
     * @return 更新结果
     */
    @Tool(
        name = "update_shipping_address",
        description = "更新用户的收货地址。需要提供完整的地址信息，包括省、市、区和详细地址。"
    )
    public static String updateShippingAddress(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId,
            @ToolParam(name = "province", description = "省份，例如：浙江省")
            String province,
            @ToolParam(name = "city", description = "城市，例如：杭州市")
            String city,
            @ToolParam(name = "district", description = "区/县，例如：西湖区")
            String district,
            @ToolParam(name = "detail_address", description = "详细地址，包括街道、小区、门牌号")
            String detailAddress,
            @ToolParam(name = "recipient", description = "收货人姓名")
            String recipient,
            @ToolParam(name = "phone", description = "手机号码")
            String phone) {

        log.info("更新收货地址，用户 ID：{}，新地址：{} {} {} {}",
            userId, province, city, district, detailAddress);

        return """
            地址更新成功！
            新的收货地址：
            收货人：%s
            手机号：%s
            省/市/区：%s %s %s
            详细地址：%s
            更新时间：2024-01-18 15:30:00
            """.formatted(recipient, phone, province, city, district, detailAddress);
    }
}