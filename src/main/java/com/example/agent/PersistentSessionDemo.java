package com.example.agent;

import java.nio.file.Path;
import java.util.Scanner;

import com.example.agent.config.AgentConfig;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;

public class PersistentSessionDemo {

    public static void main(String[] args) {
        // 创建 OpenAI 兼容模型（DeepSeek），需显式指定 baseUrl
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(AgentConfig.getOpenAiApiKey())
                .modelName("deepseek-pro")
                .baseUrl("https://api.deepseek.com")
                .build();
        

        HarnessAgent agent = HarnessAgent.builder()
                .name("persistentsessiondemo")
                .sysPrompt("你是记忆助手，会记住用户告诉你的重要信息并在后续对话中使用。")
                .workspace("persistent-session-demo")
                .model(model)
                .stateStore(new JsonFileAgentStateStore(Path.of("persistent-session-demo/.state")))
                .compaction(CompactionConfig.builder().triggerMessages(10).keepMessages(8).build())
                .build();

        RuntimeContext context = RuntimeContext.builder()
                .userId("user-1")
                .sessionId("persistent-session-001")
                .build();

        Scanner  scanner   = new Scanner(System.in);
        while (true) {
            System.out.print("User: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }
            String response = agent.call(new UserMessage(input), context).block().getTextContent();
            System.out.println("Agent: " + response);
        }
        scanner.close();
    }
}
