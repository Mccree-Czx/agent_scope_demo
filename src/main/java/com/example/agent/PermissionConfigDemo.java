package com.example.agent;

import com.example.agent.config.AgentConfig;
import com.example.agent.config.PermissionConfig;
import com.example.tool.DatabaseQueryTool;
import com.example.tool.WeatherTool;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.core.tool.Toolkit;

import java.time.Duration;

/**
 * 权限配置演示 — 展示 {@link PermissionConfig} Builder API 和 HITL 审批流程。
 *
 * <p>与底层 {@code PermissionContextState} 对比，PermissionConfig 提供更直观的：
 * <ul>
 *   <li>{@code .defaultPolicy(ALLOW)} — 默认放行</li>
 *   <li>{@code .toolPolicy("toolName", REQUIRE_USER_APPROVAL)} — 敏感工具需审批</li>
 *   <li>{@code .toolPolicy("toolName", DENY)} — 危险工具直接拒绝</li>
 *   <li>{@code .toolPolicy("db_*", REQUIRE_USER_APPROVAL)} — 通配符匹配</li>
 *   <li>{@code .filePolicy("/etc/**", DENY)} — 文件路径保护</li>
 * </ul>
 */
public class PermissionConfigDemo {

    public static void main(String[] args) {
        // ① 构建权限配置
        PermissionConfig permConfig = PermissionConfig.builder()
                .defaultPolicy(PermissionConfig.Permission.ALLOW)
                .toolPolicy("excute_update", PermissionConfig.Permission.REQUIRE_USER_APPROVAL)
                .toolPolicy("delete_file", PermissionConfig.Permission.DENY)
                .toolPolicy("db_*", PermissionConfig.Permission.REQUIRE_USER_APPROVAL)
                .filePolicy("/etc/**", PermissionConfig.Permission.DENY)
                .hitlTimeout(Duration.ofMinutes(5))
                .build();

        System.out.println("权限配置: " + permConfig + "\n");

        // ② 构建 Model / Toolkit
        var model = DashScopeChatModel.builder()
                .apiKey(AgentConfig.getDashScopeApiKey())
                .modelName(AgentConfig.getModelName())
                .build();

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTool());
        toolkit.registerTool(new DatabaseQueryTool("jdbc:sqlite::memory:", "", ""));

        // ③ 注入 Agent
        HarnessAgent agent = HarnessAgent.builder()
                .name("secure-agent")
                .sysPrompt("你是安全助手。查询用 query_database，更新用 excute_update（需审批），查天气用 get_weather。")
                .model(model)
                .toolkit(toolkit)
                .maxIters(5)
                .permissionContext(permConfig.toPermissionContextState())  // ← 关键
                .build();

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("perm_demo")
                .userId("alice")
                .build();

        // ④ 流式运行 + HITL 响应
        // 注意：AgentEventEmitter 只在 Agent 内部中间件链（onAgent/onReasoning/onActing）的
        // Reactor Context 中可用。外部 streamEvents() 的 .handle() 中拿不到它，因此 HITL 审批
        // 逻辑应放在自定义 Middleware.onActing 中处理。
        agent.streamEvents(new UserMessage("""
            请帮我在数据库中创建一个 users 表，插入测试数据'张三 28'，然后查天气。
            """), ctx)
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case TEXT_BLOCK_DELTA ->
                                System.out.print(((TextBlockDeltaEvent) event).getDelta());
                        case TOOL_CALL_START -> {
                            var e = (ToolCallStartEvent) event;
                            System.out.println("\n🔧 [调用] " + e.getToolCallName());
                        }
                        case TOOL_CALL_END ->
                                System.out.println("   ✅ 完成");
                        case REQUIRE_USER_CONFIRM -> {
                            var e = (RequireUserConfirmEvent) event;
                            System.out.println("\n⚠️  [HITL] 需要审批: " +
                                    e.getToolCalls().stream()
                                            .map(tc -> tc.getName() + "(" + tc.getInput() + ")")
                                            .toList());
                            System.out.println("   （AgentEventEmitter 在外部流不可用，请在 Middleware.onActing 中处理审批）");
                        }
                        case ALL_TOOLS_DENIED ->
                                System.out.println("   🚫 已被权限策略拒绝");
                    }
                })
                .blockLast();

        System.out.println("\n=== 演示结束 ===");
    }
}
