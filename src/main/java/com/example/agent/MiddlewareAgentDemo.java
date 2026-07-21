package com.example.agent;

import com.example.agent.config.AgentConfig;
import com.example.middleware.DynamicContextMiddleware;
import com.example.middleware.LoggingMiddleware;
import com.example.middleware.SensitiveFilterMiddleware;
import com.example.tool.WeatherTool;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.List;

public class MiddlewareAgentDemo {
    public static void main(String[] args) {
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(AgentConfig.getDashScopeApiKey())
                .modelName("qwen-vl-max")  // 视觉模型，支持图片理解
                .build();
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTool());

        HarnessAgent harnessAgent = HarnessAgent.builder()
                .name("security_agent")
                .sysPrompt("你是股票分析助手")
                .model(model)
                .toolkit(toolkit)
                .workspace("./workspace/mw")
                .middlewares(List.of(
                        new LoggingMiddleware(),           // 最外层
                        new SensitiveFilterMiddleware(),    // 第二层
                        new DynamicContextMiddleware()
                )).build();

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("security_demo")
                .userId("czx")
                .build();


        Msg msg = harnessAgent.call(new UserMessage("请给我分析sk海力士，三星电子，美光的哪个公司最具性价比"), ctx).block();
        System.out.println("touzi_agent" + msg);
    }
}
