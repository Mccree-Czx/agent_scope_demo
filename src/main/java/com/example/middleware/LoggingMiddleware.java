package com.example.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public class LoggingMiddleware implements MiddlewareBase {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LoggingMiddleware.class);

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, RuntimeContext ctx, AgentInput input, Function<AgentInput, Flux<AgentEvent>> next) {
        long start = System.currentTimeMillis();
        log.info("[Agent:{}] 会话开始 | userId={}", agent.getName(), ctx.getUserId());

        return next.apply(input).doFinally(signalType -> {
            long cost = System.currentTimeMillis() - start;
            log.info("[Agent:{}] 会话结束 | 耗时={}ms | 终止信号={}", agent.getName(), cost, signalType);
        });
    }
}

