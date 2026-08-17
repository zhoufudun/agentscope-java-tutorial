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
 * ModelService Integration Tests
 */
@SpringBootTest
@ActiveProfiles("dev")
class ModelServiceTest {

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelConfig modelConfig;

    @Test
    void testModelServiceInitialization() {
        // Verify model service is initialized
        assertNotNull(modelService);

        // Verify models are registered
        Map<String, Model> allModels = modelService.getAllModels();
        assertFalse(allModels.isEmpty(), "Model cache should not be empty");

        // Verify each provider has models
        for (String provider : modelService.getSupportedProviders()) {
            List<String> models = modelService.getModelNamesByProvider(provider);
            System.out.println("Provider " + provider + " has " + models.size() + " models: " + models);
        }
    }

    @Test
    void testDashScopeModelRegistration() {
        // Get DashScope models
        List<String> dashscopeModels = modelService.getModelNamesByProvider("dashscope");
        assertFalse(dashscopeModels.isEmpty(), "DashScope should have at least one model");
        assertTrue(dashscopeModels.contains("qwen-plus"), "qwen-plus should be registered");

        // Verify model can be retrieved
        Model model = modelService.getModel("dashscope", "qwen-plus");
        assertNotNull(model, "Model should be retrievable");
        assertEquals("qwen-plus", model.getModelName());
    }

    @Test
    void testOpenAIModelRegistration() {
        // Get OpenAI models
        List<String> openaiModels = modelService.getModelNamesByProvider("openai");
        assertFalse(openaiModels.isEmpty(), "OpenAI should have models registered");
        assertTrue(openaiModels.contains("gpt-4o"), "gpt-4o should be registered");
        assertTrue(openaiModels.contains("deepseek-chat"), "deepseek-chat should be registered");
    }

    @Test
    void testOllamaModelRegistration() {
        // Get Ollama models
        List<String> ollamaModels = modelService.getModelNamesByProvider("ollama");
        assertFalse(ollamaModels.isEmpty(), "Ollama should have models registered");
        assertTrue(ollamaModels.contains("llama3.2"), "llama3.2 should be registered");
    }

    @Test
    void testGeminiModelRegistration() {
        // Get Gemini models
        List<String> geminiModels = modelService.getModelNamesByProvider("gemini");
        assertFalse(geminiModels.isEmpty(), "Gemini should have models registered");
        assertTrue(geminiModels.contains("gemini-2.0-flash"), "gemini-2.0-flash should be registered");
    }

    @Test
    void testModelAvailabilityCheck() {
        assertTrue(modelService.isModelAvailable("dashscope", "qwen-plus"));
        assertTrue(modelService.isModelAvailable("openai", "gpt-4o"));
        assertTrue(modelService.isModelAvailable("ollama", "llama3.2"));
        assertTrue(modelService.isModelAvailable("gemini", "gemini-2.0-flash"));

        // Non-existent model
        assertFalse(modelService.isModelAvailable("dashscope", "non-existent-model"));
    }

    @Test
    void testModelRemoval() {
        // Register a test model
        modelService.registerDashScopeModel("test-model-remove");

        // Verify it's registered
        assertTrue(modelService.isModelAvailable("dashscope", "test-model-remove"));

        // Remove it
        modelService.removeModel("dashscope", "test-model-remove");

        // Verify it's removed
        assertFalse(modelService.isModelAvailable("dashscope", "test-model-remove"));
    }

    @Test
    void testModelSwitch() {
        // Verify switching doesn't throw exception for existing model
        assertDoesNotThrow(() -> modelService.switchDefaultModel("dashscope", "qwen-plus"));

        // Verify exception for non-existing model
        assertThrows(IllegalArgumentException.class,
                () -> modelService.switchDefaultModel("dashscope", "non-existent-model"));
    }

    @Test
    void testSupportedProviders() {
        List<String> providers = modelService.getSupportedProviders();
        assertEquals(4, providers.size());
        assertTrue(providers.contains("dashscope"));
        assertTrue(providers.contains("openai"));
        assertTrue(providers.contains("ollama"));
        assertTrue(providers.contains("gemini"));
    }

    @Test
    void testModelConfigInjection() {
        // Verify config is properly injected
        assertNotNull(modelConfig.getDashscope());
        assertNotNull(modelConfig.getOpenai());
        assertNotNull(modelConfig.getOllama());
        assertNotNull(modelConfig.getGemini());

        // Verify DashScope config
        assertEquals("https://dashscope.aliyuncs.com/api/v1", modelConfig.getDashscope().getBaseUrl());
        assertEquals("qwen-plus", modelConfig.getDashscope().getDefaultModel());

        // Verify OpenAI config
        assertEquals("https://api.openai.com/v1", modelConfig.getOpenai().getBaseUrl());
        assertEquals("gpt-4o", modelConfig.getOpenai().getDefaultModel());

        // Verify Ollama config
        assertEquals("http://localhost:11434", modelConfig.getOllama().getBaseUrl());

        // Verify Gemini config
        assertEquals("gemini-2.0-flash", modelConfig.getGemini().getDefaultModel());
    }
}