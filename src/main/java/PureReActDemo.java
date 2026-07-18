import com.example.agent.config.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;

/**
 * 纯 ReActAgent（Core 层）+ 自定义工具演示。
 *
 * <h3>学习要点</h3>
 * <ul>
 *   <li>不依赖 Harness，直接使用 Core 层的 {@link ReActAgent}</li>
 *   <li>如何用 {@link Tool} / {@link ToolParam} 注解定义工具并注册到 {@link Toolkit}</li>
 *   <li>ReAct 循环如何自动决定调用工具</li>
 * </ul>
 */
public class PureReActDemo {

    /**
     * 计算器工具持有类：方法上用 {@link Tool} 注解暴露给 Agent。
     */
    public static class CalculatorTool {

        @Tool(name = "calculate", description = "执行四则运算，支持 + - * / 和括号")
        public String calculate(
                @ToolParam(name = "expression", description = "数学表达式，例如 1+2*3") String expression) {
            return "计算结果是：" + eval(expression);
        }
    }

    public static void main(String[] args) {
        // DeepSeek 走 OpenAI 兼容端点，需指定 baseUrl
        Model model = OpenAIChatModel.builder()
                .apiKey(AgentConfig.getOpenAiApiKey())
                .modelName("deepseek-chat")
                .baseUrl("https://api.deepseek.com/v1")
                .build();

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new CalculatorTool());

        ReActAgent reactAgent = ReActAgent.builder()
                .name("calc-agent")
                .sysPrompt("你是一个计算助手，遇到计算问题请调用 calculate 工具。")
                .model(model)
                .toolkit(toolkit)
                .maxIters(5)
                .build();

        RuntimeContext context = RuntimeContext.builder()
                .sessionId("test-session")
                .userId("test-user")
                .build();

        reactAgent.streamEvents(new UserMessage("1到15的和是多少"), context)
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case TEXT_BLOCK_DELTA -> System.out.print(((TextBlockDeltaEvent) event).getDelta());
                        default -> System.out.println("\n[事件] " + event.getType());
                    }
                })
                .blockLast();
    }

    /**
     * 极简四则运算求值器（递归下降），支持 + - * / 和括号。
     */
   
    private static double eval(String expr) {     return new Object() {
            int pos = -1, ch;
            final String s = expr.replaceAll("\\s+", "");

            void next() { ch = (++pos < s.length()) ? s.charAt(pos) : -1; }
            boolean eat(int c) { if (ch == c) { next(); return true; } return false; }

            double parse() {
                next();
                double x = expression();
                if (pos < s.length()) throw new RuntimeException("非法字符: " + (char) ch);
                return x;
            }

            // 加减
            double expression() {
                double x = term();
                for (;;) {
                    if (eat('+')) x += term();
                    else if (eat('-')) x -= term();
                    else return x;
                }
            }

            // 乘除
            double term() {
                double x = factor();
                for (;;) {
                    if (eat('*')) x *= factor();
                    else if (eat('/')) x /= factor();
                    else return x;
                }
            }

            // 括号 / 数字
            double factor() {
                if (eat('+')) return factor();
                if (eat('-')) return -factor();
                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = expression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') next();
                    x = Double.parseDouble(s.substring(startPos, pos));
                } else {
                    throw new RuntimeException("非法字符: " + (char) ch);
                }
                return x;
            }
        }.parse();
    }
}
