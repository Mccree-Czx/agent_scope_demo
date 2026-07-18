package com.example.agent;

import com.example.agent.config.AgentConfig;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AgentScope DashScope 演示程序入口。
 *
 * <h3>学习要点</h3>
 * <ul>
 *   <li>如何通过 {@code AgentScope.run()} 启动一个 Agent 会话</li>
 *   <li>如何构建 Agent 并与 DashScope 模型交互</li>
 *   <li>如何使用 SLF4J 记录日志</li>
 *   <li>如何管理 API Key（环境变量 / 系统属性）</li>
 * </ul>
 */
public class AgentDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentDemoApplication.class);

    public static void main(String[] args) {
        log.info("===== AgentScope DashScope Demo 启动 =====");

        // 从环境变量或系统属性加载 API Key
        String dashScopeApiKey = AgentConfig.getDashScopeApiKey();
        String modelName = AgentConfig.getModelName();


        // 通过 DashScopeChatModel.Builder 传入 API Key，而非 HarnessAgent.Builder
        HarnessAgent agent = HarnessAgent.builder()
                .model(DashScopeChatModel.builder()
                        .apiKey(dashScopeApiKey)
                        .modelName(modelName)
                        .build())
                .name("DemoAgent")
                .sysPrompt("你的姓名为小智")
                .build();

        
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId("user-1")
                .sessionId("hello-session-001")
                .build();


        Msg r1Msg = agent.call(new UserMessage("请介绍一下你自己"), runtimeContext).block();
        log.info("Agent 回复: {}", r1Msg.getTextContent());

        Msg r2Msg = agent.call(new UserMessage("我刚刚问了什么问题呢"), runtimeContext).block();
        log.info("Agent 回复: {}", r2Msg.getTextContent());

        RuntimeContext runtimeContext2 = RuntimeContext.builder()
                .userId("user-2")
                .sessionId("hello-session-002")
                .build();

        Msg r3Msg = agent.call(new UserMessage("你叫什么名字"), runtimeContext).block();
        log.info("Agent 回复: {}", r3Msg.getTextContent());
        

         }
}
