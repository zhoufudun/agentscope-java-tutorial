package io.agentscope.tutorial.chapter08.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用查询工具（模拟实现）
 *
 * 提供产品查询、优惠查询、地址更新等通用功能
 */
public class QueryTools {

    private static final Logger log = LoggerFactory.getLogger(QueryTools.class);

    private QueryTools() {}

    // ==================== 产品搜索 ====================

    /**
     * 搜索产品
     *
     * @param keyword 搜索关键词
     * @return 产品列表
     */
    @Tool(
        name = "search_products",
        description = "搜索商品或产品信息。" +
                      "适用于用户询问"有什么推荐的产品"或"XX产品多少钱"等问题。"
    )
    public static String searchProducts(
            @ToolParam(name = "keyword", description = "搜索关键词")
            String keyword) {

        log.info("搜索产品，关键词：{}", keyword);

        return """
            搜索结果（关键词：%s）：
            1. 【热销】智能蓝牙耳机 Pro - ¥299.00 - 好评率 98%%
            2. 【新品】无线充电器 15W - ¥89.00 - 好评率 95%%
            3. 【特惠】手机保护套 适用 iPhone 15 - ¥39.00 - 好评率 92%%
            """.formatted(keyword);
    }

    // ==================== 优惠查询 ====================

    /**
     * 查询优惠政策
     *
     * @param userId 用户 ID
     * @return 可用的优惠政策
     */
    @Tool(
        name = "query_promotions",
        description = "查询当前可用的优惠活动。" +
                      "适用于用户询问"有什么优惠"或"能打折吗"等问题。"
    )
    public static String queryPromotions(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询优惠活动，用户ID：{}", userId);

        return """
            当前可用的优惠活动：
            1. 新用户首单立减 ¥50（限未下单新用户）
            2. 满 ¥200 减 ¥30（有效期至 2024-01-31）
            3. 会员日 9 折券（每月 18 日会员日）
            4. 积分兑换抵现（100 积分 = ¥1）
            5. 【限时】冬季保暖用品专区 8 折起
            """;
    }

    // ==================== 账户信息 ====================

    /**
     * 查询账户信息
     *
     * @param userId 用户 ID
     * @return 账户相关信息
     */
    @Tool(
        name = "query_account_info",
        description = "查询用户的账户信息，包括积分、优惠券、会员等级等。" +
                      "适用于用户询问"我的积分是多少"或"我有多少优惠券"等问题。"
    )
    public static String queryAccountInfo(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询账户信息，用户ID：{}", userId);

        return """
            账户信息：
            会员等级：黄金会员
            当前积分：2,580 积分
            可用优惠券：3 张
              - ¥20 优惠券（满 ¥200 可用，有效期至 2024-01-31）
              - ¥10 优惠券（满 ¥100 可用，有效期至 2024-02-15）
              - 9 折会员日券（每月 18 日可用）
            """;
    }

    // ==================== 地址管理 ====================

    /**
     * 获取当前收货地址
     *
     * @param userId 用户 ID
     * @return 当前收货地址
     */
    @Tool(
        name = "get_shipping_address",
        description = "获取用户当前的默认收货地址。" +
                      "在需要更新地址前，应先查询当前地址信息。"
    )
    public static String getShippingAddress(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询用户收货地址，用户ID：{}", userId);

        return """
            当前收货地址：
            收货人：张三
            手机号：138****5678
            省市区：浙江省杭州市西湖区
            详细地址：文三路 123 号 XX 小区 1 栋 1001 室
            邮编：310000
            """;
    }

    /**
     * 更新收货地址
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
        description = "更新用户的收货地址。" +
                      "需要提供完整的地址信息，包括省市区和详细地址。"
    )
    public static String updateShippingAddress(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId,
            @ToolParam(name = "province", description = "省份，如：浙江省")
            String province,
            @ToolParam(name = "city", description = "城市，如：杭州市")
            String city,
            @ToolParam(name = "district", description = "区/县，如：西湖区")
            String district,
            @ToolParam(name = "detail_address", description = "详细地址，包含街道、小区、门牌号")
            String detailAddress,
            @ToolParam(name = "recipient", description = "收货人姓名")
            String recipient,
            @ToolParam(name = "phone", description = "手机号")
            String phone) {

        log.info("更新用户收货地址，用户ID：{}，新地址：{} {} {} {}",
            userId, province, city, district, detailAddress);

        return """
            地址更新成功！
            新收货地址：
            收货人：%s
            手机号：%s
            省市区：%s %s %s
            详细地址：%s
            更新时间：2024-01-18 15:30:00
            """.formatted(recipient, phone, province, city, district, detailAddress);
    }
}