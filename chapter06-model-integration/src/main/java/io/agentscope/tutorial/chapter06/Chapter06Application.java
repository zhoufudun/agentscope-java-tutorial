package io.agentscope.tutorial.chapter06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope 第六章教程 - 模型集成
 *
 * <p>本应用演示如何配置和使用多种模型客户端，支持以下提供商：
 * <ul>
 *   <li><b>DashScope（阿里云百炼）</b> - 通义千问系列模型</li>
 *   <li><b>OpenAI</b> - GPT 系列模型</li>
 *   <li><b>Ollama</b> - 本地开源模型部署</li>
 *   <li><b>Gemini</b> - Google 多模态模型</li>
 * </ul>
 *
 * <p><b>核心概念</b>：
 * <ul>
 *   <li>统一的模型抽象层 - 通过 {@code Model} 接口统一不同提供商的调用方式</li>
 *   <li>热切换支持 - 运行时动态切换模型，无需重启应用</li>
 *   <li>模型注册机制 - 通过 {@code ModelService} 集中管理模型实例</li>
 *   <li>配置外部化 - 通过 application.yml 配置 API Key 和端点</li>
 * </ul>
 *
 * <p><b>访问方式</b>：
 * <ul>
 *   <li>模型管理 API: http://localhost:8080/api/models</li>
 *   <li>聊天 API: http://localhost:8080/api/chat</li>
 *   <li>H2 数据库控制台: http://localhost:8080/h2-console</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 1. 查看所有已注册的模型提供商
 * GET /api/models/providers
 *
 * // 2. 使用指定模型进行对话
 * POST /api/models/chat
 * {
 *   "provider": "dashscope",
 *   "modelName": "qwen-plus",
 *   "message": "你好，介绍一下你自己"
 * }
 *
 * // 3. 多模型对比（同一问题发给多个模型）
 * POST /api/models/compare
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootApplication
public class Chapter06Application {

    /**
     * 应用程序入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Chapter06Application.class, args);
    }
}