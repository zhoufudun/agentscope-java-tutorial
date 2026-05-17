package io.agentscope.tutorial.chapter05.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索工具
 *
 * <p>提供网络搜索功能，支持多种搜索引擎和搜索选项。
 * 在实际应用中，可替换为真实的搜索 API（如 Google Search API、Bing Search API 等）。
 *
 * <p>注意：这是一个模拟实现，返回的数据是虚构的，仅用于演示目的。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class SearchTool {

    // 模拟搜索索引
    private static final Map<String, List<SearchResult>> SEARCH_INDEX = new HashMap<>();

    static {
        // 初始化模拟搜索数据
        SEARCH_INDEX.put("java", List.of(
            new SearchResult("Java 官方文档", "https://docs.oracle.com/javase/", "Java 编程语言的官方文档，包含语言规范和 API 参考。"),
            new SearchResult("Spring Framework", "https://spring.io/projects/spring-framework", "Spring 框架是 Java 生态系统中最流行的企业级应用开发框架。"),
            new SearchResult("Baeldung Java 教程", "https://www.baeldung.com/java-tutorial", "全面的 Java 教程，涵盖从基础到高级的主题。")
        ));

        SEARCH_INDEX.put("python", List.of(
            new SearchResult("Python 官方文档", "https://docs.python.org/", "Python 编程语言的官方文档。"),
            new SearchResult("Real Python", "https://realpython.com/", "高质量的 Python 教程和文章。"),
            new SearchResult("PyPI", "https://pypi.org/", "Python 包索引，包含超过 300,000 个包。")
        ));

        SEARCH_INDEX.put("ai", List.of(
            new SearchResult("OpenAI", "https://openai.com/", "人工智能研究公司，开发了 GPT 系列模型。"),
            new SearchResult("Anthropic", "https://anthropic.com/", "AI 安全和研究公司，开发了 Claude 模型。"),
            new SearchResult("AgentScope", "https://agentscope.io/", "阿里巴巴的 AgentScope Java 框架。")
        ));

        SEARCH_INDEX.put("machine learning", List.of(
            new SearchResult("TensorFlow", "https://www.tensorflow.org/", "Google 开源的机器学习框架。"),
            new SearchResult("PyTorch", "https://pytorch.org/", "Facebook 开源的机器学习框架。"),
            new SearchResult("scikit-learn", "https://scikit-learn.org/", "Python 机器学习库。")
        ));
    }

    // ==================== 搜索功能 ====================

    /**
     * 执行网络搜索
     *
     * @param query 搜索关键词
     * @param limit 返回结果数量限制（默认 10）
     * @return 搜索结果列表
     */
    @Tool(
        name = "search",
        description = "执行网络搜索，返回与关键词相关的网页结果。可用于查找文档、新闻、技术问题等。"
    )
    public String search(
        @ToolParam(
            name = "query",
            description = "搜索关键词（建议使用英文或专业术语以获得更准确的结果）",
            required = true
        ) String query,
        @ToolParam(
            name = "limit",
            description = "返回结果数量限制（1-50）",
            required = false
        ) Integer limit
    ) {
        validateNotEmpty(query, "query");

        int maxResults = (limit != null && limit >= 1 && limit <= 50) ? limit : 10;

        StringBuilder result = new StringBuilder();
        result.append(String.format("🔍 搜索结果：%s\n\n", query));

        // 模拟搜索延迟
        try {
            Thread.sleep(100 + new Random().nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 搜索匹配的结果
        List<SearchResult> matchedResults = new ArrayList<>();
        query = query.toLowerCase();

        for (Map.Entry<String, List<SearchResult>> entry : SEARCH_INDEX.entrySet()) {
            if (entry.getKey().toLowerCase().contains(query) ||
                query.contains(entry.getKey().toLowerCase())) {
                matchedResults.addAll(entry.getValue());
            }
        }

        // 如果没有精确匹配，进行模糊搜索
        if (matchedResults.isEmpty()) {
            for (List<SearchResult> results : SEARCH_INDEX.values()) {
                for (SearchResult sr : results) {
                    if (sr.title.toLowerCase().contains(query) ||
                        sr.snippet.toLowerCase().contains(query)) {
                        matchedResults.add(sr);
                    }
                }
            }
        }

        if (matchedResults.isEmpty()) {
            return result.append("😢 没有找到相关结果。试试其他关键词。\n").toString();
        }

        // 限制返回数量
        matchedResults = matchedResults.stream()
            .limit(maxResults)
            .collect(Collectors.toList());

        for (int i = 0; i < matchedResults.size(); i++) {
            SearchResult sr = matchedResults.get(i);
            result.append(String.format("%d. 【%s】\n   %s\n   🔗 %s\n\n",
                i + 1, sr.title, sr.snippet, sr.url));
        }

        result.append(String.format("共找到 %d 条结果", matchedResults.size()));

        return result.toString();
    }

    /**
     * 搜索图片
     *
     * @param query 搜索关键词
     * @param limit 返回数量限制
     * @return 图片搜索结果
     */
    @Tool(
        name = "search_images",
        description = "搜索图片，返回与关键词相关的图片链接。"
    )
    public String searchImages(
        @ToolParam(name = "query", description = "图片搜索关键词", required = true) String query,
        @ToolParam(name = "limit", description = "返回数量限制（1-20）", required = false) Integer limit
    ) {
        validateNotEmpty(query, "query");

        int maxResults = (limit != null && limit >= 1 && limit <= 20) ? limit : 10;

        StringBuilder result = new StringBuilder();
        result.append(String.format("🖼️ 图片搜索结果：%s\n\n", query));

        // 模拟搜索
        Random random = new Random();
        for (int i = 0; i < maxResults; i++) {
            int width = 800 + random.nextInt(400);
            int height = 600 + random.nextInt(300);
            result.append(String.format("%d. https://placeholder.com/image/%s_%dx%d.jpg\n   尺寸：%dx%d\n\n",
                i + 1, query.replaceAll("\\s+", "_"), width, height, width, height));
        }

        return result.toString();
    }

    /**
     * 搜索新闻
     *
     * @param query 搜索关键词
     * @param days 发布时间范围（天内）
     * @return 新闻搜索结果
     */
    @Tool(
        name = "search_news",
        description = "搜索新闻，返回与关键词相关的最近新闻。"
    )
    public String searchNews(
        @ToolParam(name = "query", description = "新闻搜索关键词", required = true) String query,
        @ToolParam(name = "days", description = "发布时间范围（天数）", required = false) Integer days
    ) {
        validateNotEmpty(query, "query");

        int maxDays = (days != null && days >= 1 && days <= 30) ? days : 7;

        StringBuilder result = new StringBuilder();
        result.append(String.format("📰 新闻搜索结果：%s（近 %d 天）\n\n", query, maxDays));

        // 模拟新闻数据
        Random random = new Random();
        String[] newsSources = {"科技日报", "新浪新闻", "腾讯新闻", "网易新闻", "搜狐新闻"};
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            LocalDateTime newsTime = now.minusDays(random.nextInt(maxDays))
                                        .minusHours(random.nextInt(24));
            result.append(String.format("%d. 【%s】%s 相关新闻标题 %d\n   来源：%s | 时间：%s\n\n",
                i + 1,
                i == 0 ? "置顶" : "",
                query,
                i + 1,
                newsSources[random.nextInt(newsSources.length)],
                newsTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }

        return result.toString();
    }

    /**
     * 获取搜索建议
     *
     * @param query 部分搜索词
     * @return 搜索建议列表
     */
    @Tool(
        name = "get_search_suggestions",
        description = "获取搜索建议，返回与输入相关的搜索词推荐。"
    )
    public String getSearchSuggestions(
        @ToolParam(name = "query", description = "部分搜索词", required = true) String query
    ) {
        validateNotEmpty(query, "query");

        StringBuilder result = new StringBuilder();
        result.append(String.format("💡 搜索建议：%s\n\n", query));

        // 模拟搜索建议
        String[][] suggestions = {
            {"java 基础教程", "java 高级特性", "java 面试题"},
            {"python 入门", "python 数据分析", "python 机器学习"},
            {"ai 人工智能", "ai 生成式模型", "ai 大语言模型"},
            {"machine learning 入门", "machine learning 算法", "machine learning 实战"}
        };

        boolean found = false;
        for (String[] group : suggestions) {
            for (String suggestion : group) {
                if (suggestion.toLowerCase().contains(query.toLowerCase())) {
                    result.append(String.format("• %s\n", suggestion));
                    found = true;
                }
            }
        }

        if (!found) {
            result.append("• 试试其他关键词\n");
        }

        return result.toString();
    }

    // ==================== 辅助方法 ====================

    private void validateNotEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("参数 '" + paramName + "' 不能为空");
        }
    }

    /**
     * 搜索结果数据结构
     */
    private static class SearchResult {
        final String title;
        final String url;
        final String snippet;

        SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}