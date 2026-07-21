package com.example.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DynamicContextMiddleware implements MiddlewareBase {
    @Override
    public Mono<String> onSystemPrompt(Agent agent, RuntimeContext ctx, String currentPrompt) {
        String enhancedPrompt =
                currentPrompt + """
                ==动态上下文==
                当前日期：%s
                用户 ID：%s
                会话 ID：%s
                ============
                """.formatted(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                        ctx.getUserId(),
                        ctx.getSessionId()
                );
        return MiddlewareBase.super.onSystemPrompt(agent, ctx, enhancedPrompt);
    }
}
