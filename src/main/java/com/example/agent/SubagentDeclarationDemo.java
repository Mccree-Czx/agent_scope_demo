package com.example.agent;

import com.example.agent.config.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.WorkspaceMode;

import java.nio.file.Paths;

public class SubagentDeclarationDemo {
    public static void main(String[] args) {
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(AgentConfig.getDashScopeApiKey())
                .modelName("dashscope:qwen-plus")
                .build();

        HarnessAgent agent = HarnessAgent.builder()
                .name("research-assistant")
                .sysPrompt("""
            你是研究助手。分析用户需求，协调子 Agent 完成工作：
            1. 需要搜索信息 → 调用 researcher
            2. 搜索结果需翻译 → 调用 translator
            3. 整理所有结果，给用户完整中文回答。
            """)
                .model("dashscope:qwen-plus")
                .workspace(Paths.get("./workspace/research"))
                .subagent(SubagentDeclaration.builder()
                        .name("researcher")
                        .description("搜索研究专家，用于搜索信息、查找资料。需要获取最新信息时使用。")
                        .inlineAgentsBody("你是研究助手。搜索相关信息，整理成结构化英文摘要。")
                        .steps(10)
                        .workspaceMode(WorkspaceMode.ISOLATED).build())
                .subagent(SubagentDeclaration.builder()
                        .name("translator")
                        .description("翻译专家，将英文翻译成中文。擅长技术文档翻译。")
                        .inlineAgentsBody("你是专业翻译。将英文翻译成地道中文。只输出翻译结果。")
                        .steps(3).build())
                .build();

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("subagent_demo")
                .userId("alice")
                .build();

        System.out.println("=== 子 Agent 调用演示 ===\n");
        System.out.println("📋 已声明的子 Agent:");
        System.out.println("   • researcher — 搜索研究专家（ISOLATED 工作区）");
        System.out.println("   • translator — 翻译专家\n");
        System.out.println("────────────────────────────────────────\n");

        agent.streamEvents(new UserMessage("你怎么看待星座，并翻译成英文"), ctx)
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case TEXT_BLOCK_DELTA ->
                                System.out.print(((TextBlockDeltaEvent) event).getDelta());

                        case TOOL_CALL_START -> {
                            var e = (ToolCallStartEvent) event;
                            String name = e.getToolCallName();
                            System.out.println("\n" + "─".repeat(40));
                            switch (name) {
                                case "agent_spawn" ->
                                        System.out.println("🔨 [子Agent] 正在启动子 Agent…");
                                case "agent_send" ->
                                        System.out.println("📤 [子Agent] 向子 Agent 发送任务…");
                                case "task_output" ->
                                        System.out.println("📥 [子Agent] 等待子 Agent 返回结果…");
                                case "wait_async_results" ->
                                        System.out.println("⏳ [子Agent] 等待异步结果…");
                                default ->
                                        System.out.println("🔧 [工具] " + name);
                            }
                        }

                        case TOOL_CALL_END ->
                                System.out.println("✅ 完成");

                        case TOOL_RESULT_START ->
                                System.out.println("  └─ 子 Agent 返回结果 ↓");

                        case SUBAGENT_EXPOSED -> {
                            var e = (SubagentExposedEvent) event;
                            System.out.println("  └─ 子Agent 已注册: 【" + e.getLabel() + "】");
                        }
                    }
                })
                .blockLast();

        System.out.println("\n────────────────────────────────────────");
        System.out.println("=== 演示结束 ===");
    }

}
