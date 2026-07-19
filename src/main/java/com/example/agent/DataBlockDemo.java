package com.example.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.example.agent.config.AgentConfig;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;

public class DataBlockDemo {

    public static void main(String[] args) throws IOException {
        // 创建 DashScope 模型（通义千问）
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(AgentConfig.getDashScopeApiKey())
                .modelName("qwen-vl-max")  // 视觉模型，支持图片理解
                .build();

        HarnessAgent agent = HarnessAgent.builder()
                .name("vision-agent")
                .sysPrompt("你是多模态助手，可以理解图片内容。")
                .model(model)
                .workspace("./workspace-vision")
                .build();

        // 读取本地图片并转为 Base64
        Path imagePath = Path.of("/Users/mj/Desktop/agent_dashscope_demo/d02608ee08a2a43433c8ff63270c4388.jpg");  // 替换为你的图片路径
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 构造带图片的消息
        UserMessage msg = UserMessage.builder()
                .textContent("请描述这张图片的内容")
                .content(ImageBlock.builder()
                        .source(Base64Source.builder()
                                .mediaType("image/jpeg")
                                .data(base64Image)
                                .build())
                        .build())
                .build();

        // 发送给 Agent
        RuntimeContext context = RuntimeContext.builder()
                .userId("user-1")
                .sessionId("vision-session-001")
                .build();

        Msg reply = agent.call(msg, context).block();
        System.out.println("Agent: " + reply.getTextContent());
    }
}
