package com.example.agent;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;

import java.util.List;
import java.util.Scanner;


public class TerminalChatDemo {
        private static final String RESET = "[0m", GREEN = "[32m", BLUE = "[34m",
            YELLOW = "[33m", CYAN = "[36m", GRAY = "[90m";

        public static void main(String[] args) {
            HarnessAgent harnessAgent = HarnessAgent.builder()
                    .name("terminal-chat")
                    .sysPrompt("你是一个友好的ai助手，回答通俗易懂但富有深度")
                    .model("dashscope:qwen-plus")
                    .workspace("./workspace/terminal")
                    .compaction(CompactionConfig.builder()
                            .triggerMessages(5)        // 超 20 条消息触发压缩
                            .keepMessages(8)            // 压缩后保留最近 8 条
                            .triggerTokens(4096)        // 或超 4096 tokens 触发
                            .model("dashscope:qwen-plus")
                            .build())
                    .build();

            RuntimeContext ctx = RuntimeContext.builder()
                    .sessionId("terminal-" + System.currentTimeMillis())
                    .userId("terminal-user")
                    .build();

            System.out.println(CYAN + "=".repeat(60) + "\n 终端聊天演示 — 'exit' 退出 / 'new' 新会话\n" + "=".repeat(60) + RESET);

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n" + GREEN + "你: " + RESET);
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input.trim())) break;
                if ("new".equalsIgnoreCase(input.trim())) {
                    ctx = RuntimeContext.builder()
                            .sessionId("terminal-" + System.currentTimeMillis())
                            .userId("terminal-user")
                            .build();
                    System.out.println(YELLOW + "新会话已创建" + RESET);
                    continue;
                }
                System.out.print(BLUE + "Agent: " + RESET);

                AgentEvent blockLast = harnessAgent.streamEvents(new UserMessage(input), ctx).doOnNext(
                        agentEvent -> {
                            switch (agentEvent.getType()) {
                                case TEXT_BLOCK_DELTA -> {
                                    System.out.print(((TextBlockDeltaEvent) agentEvent).getDelta());
                                    System.out.flush();  // 实时打字机效果
                                }
                                case TOOL_CALL_START -> {
                                    ToolCallStartEvent tool = (ToolCallStartEvent) agentEvent;
                                    System.out.print("\n" + YELLOW + "  🔧 [工具] " + tool.getToolCallName()
                                            + " 参数: " + tool.getToolCallName() + RESET);
                                    System.out.print("\n" + BLUE);
                                }
                                case TOOL_CALL_END -> {
                                    System.out.print(GRAY + "  ✅ 完成" + RESET + "\n" + BLUE);
                                }
                            }
                        }
                ).blockLast();
                System.out.println(RESET);
            }
            scanner.close();
        }
}
