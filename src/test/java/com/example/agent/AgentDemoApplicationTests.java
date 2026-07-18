package com.example.agent;

import com.example.agent.config.AgentConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentDemoApplication 单元测试。
 *
 * <h3>学习要点</h3>
 * <ul>
 *   <li>使用 JUnit 5 进行单元测试</li>
 *   <li>测试配置加载逻辑</li>
 * </ul>
 */
class AgentDemoApplicationTests {

    @Test
    @DisplayName("系统属性优先级高于环境变量")
    void testApiKeyFromSystemProperty() {
        // 设置系统属性
        System.setProperty("dashscope.api.key", "sk-test-from-property");

        String apiKey = AgentConfig.getDashScopeApiKey();
        assertEquals("sk-test-from-property", apiKey);

        // 清理
        System.clearProperty("dashscope.api.key");
    }

    @Test
    @DisplayName("未配置 API Key 时抛出 IllegalStateException")
    void testApiKeyNotSet() {
        // 当前环境未设置 OPENAI_API_KEY，也未设置系统属性 openai.api.key
        System.clearProperty("openai.api.key");
        assertThrows(IllegalStateException.class, AgentConfig::getOpenAiApiKey);
    }

    @Test
    @DisplayName("默认模型名称为 qwen-max")
    void testDefaultModelName() {
        String modelName = AgentConfig.getModelName();
        assertEquals("qwen-max", modelName);
    }

    @Test
    @DisplayName("可通过系统属性覆盖模型名称")
    void testCustomModelName() {
        System.setProperty("dashscope.model.name", "qwen-turbo");

        String modelName = AgentConfig.getModelName();
        assertEquals("qwen-turbo", modelName);

        // 清理
        System.clearProperty("dashscope.model.name");
    }
}
