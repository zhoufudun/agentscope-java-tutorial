package io.agentscope.tutorial.chapter08.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General query tools (simulated implementation)
 *
 * Provides product search, promotion query, address update and other general functions
 */
public class QueryTools {

    private static final Logger log = LoggerFactory.getLogger(QueryTools.class);

    public QueryTools() {}

    // ==================== Product Search ====================

    /**
     * Search products
     *
     * @param keyword Search keyword
     * @return Product list
     */
    @Tool(
        name = "search_products",
        description = "Search for products or merchandise information. Use when user asks for product recommendations or prices."
    )
    public static String searchProducts(
            @ToolParam(name = "keyword", description = "Search keyword")
            String keyword) {

        log.info("Searching products, keyword: {}", keyword);

        return """
            Search Results (keyword: %s):
            1. [Hot] Smart Bluetooth Earphones Pro - ¥299.00 - 98%% positive reviews
            2. [New] Wireless Charger 15W - ¥89.00 - 95%% positive reviews
            3. [Special] Phone Case for iPhone 15 - ¥39.00 - 92%% positive reviews
            """.formatted(keyword);
    }

    // ==================== Promotion Query ====================

    /**
     * Query available promotions
     *
     * @param userId User ID
     * @return Available promotions
     */
    @Tool(
        name = "query_promotions",
        description = "Query current available promotional activities. Use when user asks about discounts or special offers."
    )
    public static String queryPromotions(
            @ToolParam(name = "user_id", description = "User ID")
            String userId) {

        log.info("Querying promotions, user ID: {}", userId);

        return """
            Current Available Promotions:
            1. New User First Order Discount ¥50 (for users who haven't placed order yet)
            2. ¥200 off ¥30 (valid until 2024-01-31)
            3. Member Day 10%% Off Coupon (18th of each month)
            4. Points Exchange (100 points = ¥1)
            5. [Limited Time] Winter Essentials 20%% Off or more
            """;
    }

    // ==================== Account Info ====================

    /**
     * Query account information
     *
     * @param userId User ID
     * @return Account related information
     */
    @Tool(
        name = "query_account_info",
        description = "Query user's account information including points, coupons, membership level, etc. Use when user asks about points or coupons."
    )
    public static String queryAccountInfo(
            @ToolParam(name = "user_id", description = "User ID")
            String userId) {

        log.info("Querying account info, user ID: {}", userId);

        return """
            Account Information:
            Membership Level: Gold Member
            Current Points: 2,580 points
            Available Coupons: 3
              - ¥20 Coupon (¥200 minimum, valid until 2024-01-31)
              - ¥10 Coupon (¥100 minimum, valid until 2024-02-15)
              - 10%% Off Member Day Coupon (available on 18th of each month)
            """;
    }

    // ==================== Address Management ====================

    /**
     * Get current shipping address
     *
     * @param userId User ID
     * @return Current shipping address
     */
    @Tool(
        name = "get_shipping_address",
        description = "Get user's current default shipping address. Query current address before updating."
    )
    public static String getShippingAddress(
            @ToolParam(name = "user_id", description = "User ID")
            String userId) {

        log.info("Querying shipping address, user ID: {}", userId);

        return """
            Current Shipping Address:
            Recipient: Zhang San
            Phone: 138****5678
            Province/City/District: Zhejiang Province, Hangzhou City, Xihu District
            Detailed Address: Building 1, Unit 1001, XX Community, 123 Wensan Road
            Postal Code: 310000
            """;
    }

    /**
     * Update shipping address
     *
     * @param userId User ID
     * @param province Province
     * @param city City
     * @param district District/County
     * @param detailAddress Detailed address
     * @param recipient Recipient name
     * @param phone Phone number
     * @return Update result
     */
    @Tool(
        name = "update_shipping_address",
        description = "Update user's shipping address. Complete address information including province, city, district and detailed address is required."
    )
    public static String updateShippingAddress(
            @ToolParam(name = "user_id", description = "User ID")
            String userId,
            @ToolParam(name = "province", description = "Province, e.g.: Zhejiang Province")
            String province,
            @ToolParam(name = "city", description = "City, e.g.: Hangzhou City")
            String city,
            @ToolParam(name = "district", description = "District/County, e.g.: Xihu District")
            String district,
            @ToolParam(name = "detail_address", description = "Detailed address including street, community, house number")
            String detailAddress,
            @ToolParam(name = "recipient", description = "Recipient name")
            String recipient,
            @ToolParam(name = "phone", description = "Phone number")
            String phone) {

        log.info("Updating shipping address, user ID: {}, new address: {} {} {} {}",
            userId, province, city, district, detailAddress);

        return """
            Address Update Successful!
            New Shipping Address:
            Recipient: %s
            Phone: %s
            Province/City/District: %s %s %s
            Detailed Address: %s
            Update Time: 2024-01-18 15:30:00
            """.formatted(recipient, phone, province, city, district, detailAddress);
    }
}