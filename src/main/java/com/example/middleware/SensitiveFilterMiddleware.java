package com.example.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.regex.Pattern;

public class SensitiveFilterMiddleware implements MiddlewareBase {
    private static final Logger log = LoggerFactory.getLogger(SensitiveFilterMiddleware.class);
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD = Pattern.compile("\\d{17}[\\dXx]");

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, RuntimeContext ctx, AgentInput input,
                                     Function<AgentInput, Flux<AgentEvent>> next) {
        for (Msg msg : input.msgs()) {
            String text = msg.getTextContent();
            if (text != null && (PHONE.matcher(text).find() || ID_CARD.matcher(text).find())) {
                log.warn("[Agent:{}] 检测到敏感信息，已拦截 | userId={}", agent.getName(), ctx.getUserId());
                return Flux.just(
                        new TextBlockDeltaEvent("sensitive-filter", "block-msg",
                                "您的消息包含敏感信息（手机号或身份证号），已被拦截，请脱敏后重新发送。")
                );
            }
        }

        return next.apply(input);
    }
}
