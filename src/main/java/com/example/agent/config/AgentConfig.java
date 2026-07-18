package com.example.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent 配置工具类。
 *
 * <p>负责从环境变量 / 系统属性中读取配置，提供合理的默认值。</p>
 *
 * <h3>学习要点</h3>
 * <ul>
 *   <li>如何安全管理 API Key（严禁硬编码，改用环境变量 / 系统属性）</li>
 *   <li>如何通过系统属性覆盖默认配置</li>
 * </ul>
 *
 * <h3>API Key 配置方式（二选一）</h3>
 * <pre>
 * 1. 环境变量：   export DASHSCOPE_API_KEY=sk-xxx   / export OPENAI_API_KEY=sk-xxx
 * 2. JVM 系统属性：-Ddashscope.api.key=sk-xxx        / -Dopenai.api.key=sk-xxx
 * </pre>
 */
public final class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    /** 环境变量名：DashScope API Key */
    private static final String ENV_DASHSCOPE_API_KEY = "DASHSCOPE_API_KEY";

    /** 系统属性名：DashScope API Key（用于 JVM 参数 -D 覆盖） */
    private static final String PROP_DASHSCOPE_API_KEY = "dashscope.api.key";

    /** 环境变量名：OpenAI API Key */
    private static final String ENV_OPENAI_API_KEY = "OPENAI_API_KEY";

    /** 系统属性名：OpenAI API Key（用于 JVM 参数 -D 覆盖） */
    private static final String PROP_OPENAI_API_KEY = "openai.api.key";

    /** 默认模型名称 */
    private static final String DEFAULT_MODEL_NAME = "qwen-max";

    private AgentConfig() {
        // 工具类，禁止实例化
    }

    /**
     * 获取 DashScope API Key。
     * <p>优先级：系统属性 &gt; 环境变量</p>
     *
     * @return API Key 字符串
     * @throws IllegalStateException 未配置时抛出，提示如何设置
     */
    public static String getDashScopeApiKey() {
        return resolveApiKey("DashScope", PROP_DASHSCOPE_API_KEY, ENV_DASHSCOPE_API_KEY);
    }

    /**
     * 获取 OpenAI API Key。
     * <p>优先级：系统属性 &gt; 环境变量</p>
     *
     * @return API Key 字符串
     * @throws IllegalStateException 未配置时抛出，提示如何设置
     */
    public static String getOpenAiApiKey() {
        return resolveApiKey("OpenAI", PROP_OPENAI_API_KEY, ENV_OPENAI_API_KEY);
    }

    /**
     * 获取模型名称。
     *
     * @return 模型名称，默认 {@value #DEFAULT_MODEL_NAME}
     */
    public static String getModelName() {
        return System.getProperty("dashscope.model.name", DEFAULT_MODEL_NAME);
    }

    /**
     * 统一的 API Key 解析逻辑：系统属性优先，其次环境变量。
     *
     * @param providerName 供应商名称（仅用于日志/异常提示）
     * @param propName     系统属性名
     * @param envName      环境变量名
     * @return 解析到的 API Key
     * @throws IllegalStateException 两处均未配置时抛出
     */
    private static String resolveApiKey(String providerName, String propName, String envName) {
        // 先查系统属性（-DpropName=xxx）
        String apiKey = System.getProperty(propName);
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("使用系统属性 {} 中的 {} API Key", propName, providerName);
            return apiKey;
        }

        // 再查环境变量
        apiKey = System.getenv(envName);
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("使用环境变量 {} 中的 {} API Key", envName, providerName);
            return apiKey;
        }

        // 均未配置：快速失败，给出清晰指引
        throw new IllegalStateException(String.format(
                "未配置 %s API Key！请设置环境变量 %s 或 JVM 系统属性 -D%s=<your-key>",
                providerName, envName, propName));
    }
}
