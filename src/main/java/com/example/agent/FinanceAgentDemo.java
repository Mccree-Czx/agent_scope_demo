package com.example.agent;

import com.example.agent.config.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;

public class FinanceAgentDemo {

    public static void main(String[] args) {
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(AgentConfig.getDashScopeApiKey())
                .modelName("qwen-vl-max")  // 视觉模型，支持图片理解
                .build();

        HarnessAgent agent = HarnessAgent.builder()
                .name("finance-analyst")
                .sysPrompt("你是一个助手。")  // 会被 AGENTS.md 增强
                .model(model)
                .workspace(Paths.get("workspace/finance"))
                .build();
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("finance-demo")
                .userId("alice")
                .build();

        Msg msg = agent.call(new UserMessage("你怎么看贵州茅台"), ctx).block();
        System.out.println(msg.getTextContent());
    }


}
