package com.example.agent;

import com.example.agent.config.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AgentScope OpenAI 演示程序入口。
 *
 * <h3>学习要点</h3>
 * <ul>
 *   <li>如何通过 {@code OpenAIChatModel} 接入 OpenAI 兼容模型（GPT / DeepSeek 等）</li>
 *   <li>如何构建 {@code HarnessAgent} 并管理多轮对话</li>
 *   <li>如何使用 SLF4J 记录日志</li>
 * </ul>
 */
public class AgentDemoOpenAi {

    private static final Logger log = LoggerFactory.getLogger(AgentDemoOpenAi.class);

    public static void main(String[] args) {

        // 从 AgentConfig 加载配置
        String openAiApiKey = AgentConfig.getOpenAiApiKey();
        String modelName = "deepseek-chat";   // 按需修改，例如 "deepseek-chat"

        OpenAIChatModel openAiChatModel = OpenAIChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelName)
                .baseUrl("https://api.deepseek.com/v1")  // DeepSeek 端点
                .build();

        HarnessAgent agent = HarnessAgent.builder()
                .model(openAiChatModel)
                .name("deepseek-agent")
                .sysPrompt("你是精通Java的agent")
                .build();

        RuntimeContext ctx = RuntimeContext.builder()
                .userId("user-1")
                .sessionId("openai-session-001")
                .build();

        Msg reply = agent.call(new UserMessage("你好，请介绍一下你自己"), ctx).block();
        log.info("Agent 回复: {}", reply.getTextContent());

        // 流式输出
        agent.streamEvents(new UserMessage("你好，请介绍一下你自己"), ctx)
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case TEXT_BLOCK_DELTA -> System.out.print(((TextBlockDeltaEvent) event).getDelta());
                        case TOOL_CALL_START -> System.out.println("\n🔧 调用工具: " + ((ToolCallStartEvent) event).getToolCallName());
                        case TOOL_CALL_END -> System.out.println("\n✅ 工具执行完成");
                        default -> log.info("Event: {}", event.getType());
                    }
                })
                .blockLast();

        
    }
}
