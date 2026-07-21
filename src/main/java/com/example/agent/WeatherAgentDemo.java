package com.example.agent;

import com.example.agent.config.AgentConfig;
import com.example.tool.WeatherTool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import reactor.core.publisher.Mono;

/**
 * 天气查询 Agent 演示。
 *
 * <p>展示 {@code WeatherTool} 的注册和调用流程：ReActAgent 在需要时自动调用
 * {@code get_weather} / {@code get_forecast} 工具。</p>
 */
public class WeatherAgentDemo {

    public static void main(String[] args) {
        Model model = DashScopeChatModel.builder()
                .apiKey(AgentConfig.getDashScopeApiKey())
                .modelName(AgentConfig.getModelName())
                .build();

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTool());

        HarnessAgent harnessAgent = HarnessAgent.builder()
                .name("weather-agent")
                .sysPrompt("你是一个天气查询助手，用户询问天气时请调用 get_weather 或 get_forecast 工具获取数据后回答。")
                .model(model)
                .toolkit(toolkit)
                .maxIters(5)
                .workspace("./workspace/weather")
                .build();

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("weather_demo")
                .userId("alice")
                .build();

//        Msg msg = harnessAgent.call(new UserMessage("北京天气怎么样以及接下来三天呢"), ctx).block();
//            System.out.println("agent"+msg);

        harnessAgent.streamEvents(new UserMessage("北京接下来天气怎么样"),ctx).doOnNext(agentEvent -> {
            switch (agentEvent.getType()){
                case TEXT_BLOCK_DELTA -> System.out.print(
                        ((TextBlockDeltaEvent) agentEvent).getDelta());
                case TOOL_CALL_START -> System.out.println(
                        "\n[调用工具] " + ((ToolCallStartEvent) agentEvent).getToolCallName());
                case TOOL_CALL_END -> System.out.println(
                        "\n[工具完成]");

            }
        }).blockLast();
    }
}
