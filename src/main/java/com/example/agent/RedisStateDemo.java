package com.example.agent;

import java.util.Scanner;

import com.example.agent.config.AgentConfig;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.middleware.HarnessRuntimeMiddleware;
import redis.clients.jedis.UnifiedJedis;

/**
 * Redis 分布式状态后端演示。
 *
 * <h3>学习要点</h3>
 * <ul>
 *   <li>如何用 {@link RedisAgentStateStore} 把 Agent 状态存到 Redis</li>
 *   <li>AgentScope 不管理 Redis 连接，由调用方创建 {@link UnifiedJedis} 并注入</li>
 *   <li>生产多副本部署时，所有副本连同一个 Redis 即可共享会话状态</li>
 * </ul>
 *
 * <p>运行前需先启动本机 Redis：{@code brew services start redis}</p>
 */
public class RedisStateDemo {

    public static void main(String[] args) {
        // 1. 创建 Redis 客户端并注入到 StateStore
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        RedisAgentStateStore redisStateStore = RedisAgentStateStore.builder()
                .jedisClient(jedis)
                .keyPrefix("agentscope-demo")
                .build();

        // 2. 创建 OpenAI 兼容模型（DeepSeek），需显式指定 baseUrl
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(AgentConfig.getOpenAiApiKey())
                .modelName("deepseek-chat")
                .baseUrl("https://api.deepseek.com/v1")
                .build();

        RuntimeContext context = RuntimeContext.builder()
                .userId("redis-shared-session")
                .sessionId("redis-session-001")
                .build(); 
        

        HarnessAgent agent1 = HarnessAgent.builder()
                .name("redis-state-demo")
                .sysPrompt("你是一名助手，记住用户信息")
                .workspace("./workspace/redis_1")
                .model(model)
                .stateStore(redisStateStore)
                .build();
        Msg responseMsg1 = agent1.call(new UserMessage("我叫小智，在阿里巴巴做agent开发工程师"), context).block();
        System.out.println(responseMsg1.getTextContent());
        

        HarnessAgent agent2 = HarnessAgent.builder()
                .name("redis-state-demo")
                .sysPrompt("你是一名助手，记住用户信息")
                .workspace("./workspace/redis_2")
                .model(model)
                .stateStore(redisStateStore)
                .build();

        Msg responseMsg2 = agent2.call(new UserMessage("我叫什么名字？在哪里工作？"), context).block();
        System.out.println(responseMsg2.getTextContent());

        // 5. 关闭资源
        redisStateStore.close();
    }
}
