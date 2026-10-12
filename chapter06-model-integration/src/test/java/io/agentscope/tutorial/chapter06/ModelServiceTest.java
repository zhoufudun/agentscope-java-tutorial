package io.agentscope.tutorial.chapter06;

import io.agentscope.core.model.Model;
import io.agentscope.tutorial.chapter06.config.ModelConfig;
import io.agentscope.tutorial.chapter06.service.ModelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelService 集成测试
 *
 * <p>验证模型服务的核心功能，包括：
 * <ul>
 *   <li>模型初始化和注册</li>
 *   <li>多提供商支持</li>
 *   <li>模型检索和移除</li>
 *   <li>配置注入</li>
 * </ul>
 *
 * <p>使用 dev 配置文件运行，避免使用生产环境 API Key。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("dev")
class ModelServiceTest {

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelConfig modelConfig;

    /**
     * 测试模型服务初始化
     *
     * <p>验证服务启动后已注册模型。
     */
    @Test
    void testModelServiceInitialization() {
        // 验证服务已初始化
        assertNotNull(modelService);

        // 验证模型已注册
        Map<String, Model> allModels = modelService.getAllModels();
        assertFalse(allModels.isEmpty(), "模型缓存不应为空");

        // 输出各提供商的模型数量
        for (String provider : modelService.getSupportedProviders()) {
            List<String> models = modelService.getModelNamesByProvider(provider);
            System.out.println("提供商 " + provider + " 注册了 " + models.size() + " 个模型: " + models);
        }
    }

    /**
     * 测试 DashScope 模型注册
     */
    @Test
    void testDashScopeModelRegistration() {
        // 获取 DashScope 模型列表
        List<String> dashscopeModels = modelService.getModelNamesByProvider("dashscope");
        assertFalse(dashscopeModels.isEmpty(), "DashScope 应至少有一个模型");
        assertTrue(dashscopeModels.contains("qwen-plus"), "qwen-plus 应已注册");

        // 验证模型可获取
        Model model = modelService.getModel("dashscope", "qwen-plus");
        assertNotNull(model, "应能获取到模型");
        assertEquals("qwen-plus", model.getModelName());
    }

    /**
     * 测试 OpenAI 模型注册
     */
    @Test
    void testOpenAIModelRegistration() {
        // 获取 OpenAI 模型列表
        List<String> openaiModels = modelService.getModelNamesByProvider("openai");
        assertFalse(openaiModels.isEmpty(), "OpenAI 应有已注册的模型");
        assertTrue(openaiModels.contains("gpt-4o"), "gpt-4o 应已注册");
        assertTrue(openaiModels.contains("deepseek-chat"), "deepseek-chat 应已注册");
    }

    /**
     * 测试 Ollama 模型注册
     */
    @Test
    void testOllamaModelRegistration() {
        // 获取 Ollama 模型列表
        List<String> ollamaModels = modelService.getModelNamesByProvider("ollama");
        assertFalse(ollamaModels.isEmpty(), "Ollama 应有已注册的模型");
        assertTrue(ollamaModels.contains("llama3.2"), "llama3.2 应已注册");
    }

    /**
     * 测试 Gemini 模型注册
     */
    @Test
    void testGeminiModelRegistration() {
        // 获取 Gemini 模型列表
        List<String> geminiModels = modelService.getModelNamesByProvider("gemini");
        assertFalse(geminiModels.isEmpty(), "Gemini 应有已注册的模型");
        assertTrue(geminiModels.contains("gemini-2.0-flash"), "gemini-2.0-flash 应已注册");
    }

    /**
     * 测试模型可用性检查
     */
    @Test
    void testModelAvailabilityCheck() {
        // 已注册的模型应可用
        assertTrue(modelService.isModelAvailable("dashscope", "qwen-plus"));
        assertTrue(modelService.isModelAvailable("openai", "gpt-4o"));
        assertTrue(modelService.isModelAvailable("ollama", "llama3.2"));
        assertTrue(modelService.isModelAvailable("gemini", "gemini-2.0-flash"));

        // 不存在的模型应不可用
        assertFalse(modelService.isModelAvailable("dashscope", "non-existent-model"));
    }

    /**
     * 测试模型移除
     */
    @Test
    void testModelRemoval() {
        // 注册一个测试模型
        modelService.registerDashScopeModel("test-model-remove");

        // 验证已注册
        assertTrue(modelService.isModelAvailable("dashscope", "test-model-remove"));

        // 移除模型
        modelService.removeModel("dashscope", "test-model-remove");

        // 验证已移除
        assertFalse(modelService.isModelAvailable("dashscope", "test-model-remove"));
    }

    /**
     * 测试模型切换
     */
    @Test
    void testModelSwitch() {
        // 切换到已存在的模型不应抛异常
        assertDoesNotThrow(() -> modelService.switchDefaultModel("dashscope", "qwen-plus"));

        // 切换到不存在的模型应抛异常
        assertThrows(IllegalArgumentException.class,
                () -> modelService.switchDefaultModel("dashscope", "non-existent-model"));
    }

    /**
     * 测试支持的提供商列表
     */
    @Test
    void testSupportedProviders() {
        List<String> providers = modelService.getSupportedProviders();
        assertEquals(4, providers.size());
        assertTrue(providers.contains("dashscope"));
        assertTrue(providers.contains("openai"));
        assertTrue(providers.contains("ollama"));
        assertTrue(providers.contains("gemini"));
    }

    /**
     * 测试配置注入
     */
    @Test
    void testModelConfigInjection() {
        // 验证配置已正确注入
        assertNotNull(modelConfig.getDashscope());
        assertNotNull(modelConfig.getOpenai());
        assertNotNull(modelConfig.getOllama());
        assertNotNull(modelConfig.getGemini());

        // 验证 DashScope 配置
        assertEquals("https://dashscope.aliyuncs.com/api/v1", modelConfig.getDashscope().getBaseUrl());
        assertEquals("qwen-plus", modelConfig.getDashscope().getDefaultModel());

        // 验证 OpenAI 配置
        assertEquals("https://api.openai.com/v1", modelConfig.getOpenai().getBaseUrl());
        assertEquals("gpt-4o", modelConfig.getOpenai().getDefaultModel());

        // 验证 Ollama 配置
        assertEquals("http://localhost:11434", modelConfig.getOllama().getBaseUrl());

        // 验证 Gemini 配置
        assertEquals("gemini-2.0-flash", modelConfig.getGemini().getDefaultModel());
    }
}