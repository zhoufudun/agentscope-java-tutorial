package io.agentscope.tutorial.chapter05.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 请求工具
 *
 * <p>提供 HTTP 请求功能，支持 GET、POST、PUT、DELETE 等方法。
 * 支持自定义请求头、超时设置、响应处理等。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class HttpTool {

    // HTTP 客户端（复用以提高性能）
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // 常用请求头预设
    private static final Map<String, String> COMMON_HEADERS = new ConcurrentHashMap<>();
    static {
        COMMON_HEADERS.put("User-Agent", "AgentScope-Tutorial/1.0");
        COMMON_HEADERS.put("Accept", "application/json, text/plain, */*");
        COMMON_HEADERS.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
    }

    // ==================== HTTP 请求 ====================

    /**
     * 发送 GET 请求
     *
     * @param url 请求 URL
     * @param headers 额外的请求头（可选，格式：key1:value1,key2:value2）
     * @return 响应内容
     */
    @Tool(
        name = "http_get",
        description = "发送 HTTP GET 请求，获取指定 URL 的内容。适用于获取 API 数据、网页内容等场景。"
    )
    public String httpGet(
        @ToolParam(name = "url", description = "请求 URL（必须以 http:// 或 https:// 开头）", required = true) String url,
        @ToolParam(name = "headers", description = "额外的请求头（可选，格式：key1:value1,key2:value2）", required = false) String headers
    ) {
        validateUrl(url);
        return executeRequest("GET", url, headers, null);
    }

    /**
     * 发送 POST 请求
     *
     * @param url 请求 URL
     * @param body 请求体（JSON 格式或纯文本）
     * @param headers 额外的请求头（可选）
     * @return 响应内容
     */
    @Tool(
        name = "http_post",
        description = "发送 HTTP POST 请求，向指定 URL 提交数据。适用于提交表单、调用 API 等场景。"
    )
    public String httpPost(
        @ToolParam(name = "url", description = "请求 URL", required = true) String url,
        @ToolParam(name = "body", description = "请求体内容（JSON 格式或纯文本）", required = false) String body,
        @ToolParam(name = "headers", description = "额外的请求头（可选）", required = false) String headers
    ) {
        validateUrl(url);
        return executeRequest("POST", url, headers, body);
    }

    /**
     * 发送 PUT 请求
     *
     * @param url 请求 URL
     * @param body 请求体内容
     * @param headers 额外的请求头（可选）
     * @return 响应内容
     */
    @Tool(
        name = "http_put",
        description = "发送 HTTP PUT 请求，更新指定 URL 的资源。"
    )
    public String httpPut(
        @ToolParam(name = "url", description = "请求 URL", required = true) String url,
        @ToolParam(name = "body", description = "请求体内容", required = false) String body,
        @ToolParam(name = "headers", description = "额外的请求头（可选）", required = false) String headers
    ) {
        validateUrl(url);
        return executeRequest("PUT", url, headers, body);
    }

    /**
     * 发送 DELETE 请求
     *
     * @param url 请求 URL
     * @param headers 额外的请求头（可选）
     * @return 响应内容
     */
    @Tool(
        name = "http_delete",
        description = "发送 HTTP DELETE 请求，删除指定 URL 的资源。"
    )
    public String httpDelete(
        @ToolParam(name = "url", description = "请求 URL", required = true) String url,
        @ToolParam(name = "headers", description = "额外的请求头（可选）", required = false) String headers
    ) {
        validateUrl(url);
        return executeRequest("DELETE", url, headers, null);
    }

    /**
     * 检查 URL 是否可访问
     *
     * @param url 要检查的 URL
     * @param timeout 超时时间（秒）
     * @return 检查结果
     */
    @Tool(
        name = "check_url",
        description = "检查 URL 是否可访问，返回状态码和响应时间。"
    )
    public String checkUrl(
        @ToolParam(name = "url", description = "要检查的 URL", required = true) String url,
        @ToolParam(name = "timeout", description = "超时时间（秒，默认 5）", required = false) Integer timeout
    ) {
        validateUrl(url);

        int timeoutSeconds = (timeout != null && timeout > 0) ? timeout : 5;

        try {
            long startTime = System.currentTimeMillis();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());

            long responseTime = System.currentTimeMillis() - startTime;

            return String.format("✅ URL 可访问\n" +
                "• URL: %s\n" +
                "• 状态码: %d\n" +
                "• 响应时间: %dms\n" +
                "• 内容长度: %d bytes",
                url,
                response.statusCode(),
                responseTime,
                response.body().length());

        } catch (Exception e) {
            return String.format("❌ URL 不可访问\n" +
                "• URL: %s\n" +
                "• 错误: %s",
                url,
                e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 执行 HTTP 请求
     */
    private String executeRequest(String method, String url, String headers, String body) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));

            // 添加常用请求头
            for (Map.Entry<String, String> entry : COMMON_HEADERS.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            // 添加自定义请求头
            if (headers != null && !headers.isBlank()) {
                parseHeaders(headers).forEach(requestBuilder::header);
            }

            // 设置方法和请求体
            switch (method) {
                case "GET" -> requestBuilder.GET();
                case "DELETE" -> requestBuilder.DELETE();
                case "POST" -> {
                    requestBuilder.header("Content-Type", "application/json");
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                }
                case "PUT" -> {
                    requestBuilder.header("Content-Type", "application/json");
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                }
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());

            // 格式化响应
            StringBuilder result = new StringBuilder();
            result.append(String.format("📡 HTTP %s 请求结果\n", method));
            result.append(String.format("• URL: %s\n", url));
            result.append(String.format("• 状态码: %d %s\n",
                response.statusCode(),
                getStatusText(response.statusCode())));
            result.append(String.format("• 响应时间: %dms\n", 0)); // 简化版

            // 添加响应头
            result.append("• 响应头:\n");
            response.headers().map().forEach((key, values) -> {
                result.append(String.format("   %s: %s\n", key, String.join(", ", values)));
            });

            // 添加响应体（限制长度）
            String responseBody = response.body();
            if (responseBody != null && !responseBody.isEmpty()) {
                if (responseBody.length() > 1000) {
                    responseBody = responseBody.substring(0, 1000) + "\n... (内容已截断)";
                }
                result.append(String.format("• 响应体:\n%s", responseBody));
            }

            return result.toString();

        } catch (Exception e) {
            return String.format("❌ HTTP 请求失败\n" +
                "• URL: %s\n" +
                "• 方法: %s\n" +
                "• 错误: %s",
                url,
                method,
                e.getMessage());
        }
    }

    /**
     * 解析请求头字符串
     */
    private Map<String, String> parseHeaders(String headers) {
        Map<String, String> headerMap = new ConcurrentHashMap<>();
        if (headers != null && !headers.isBlank()) {
            String[] pairs = headers.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length >= 2) {
                    headerMap.put(keyValue[0].trim(), pair.substring(pair.indexOf(":") + 1).trim());
                }
            }
        }
        return headerMap;
    }

    /**
     * 验证 URL 格式
     */
    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL 不能为空");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL 必须以 http:// 或 https:// 开头");
        }
    }

    /**
     * 获取状态码文本
     */
    private String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }
}