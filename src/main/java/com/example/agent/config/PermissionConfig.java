package com.example.agent.config;

import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;

import java.time.Duration;
import java.util.*;

/**
 * 权限配置 — 对 AgentScope 底层 {@link PermissionContextState} / {@link PermissionRule}
 * 的封装，提供更直观的 Builder API。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * PermissionConfig config = PermissionConfig.builder()
 *     .defaultPolicy(Permission.ALLOW)
 *     .toolPolicy("execute_update", Permission.REQUIRE_USER_APPROVAL)
 *     .toolPolicy("delete_file", Permission.DENY)
 *     .toolPolicy("db_*", Permission.REQUIRE_USER_APPROVAL)
 *     .filePolicy("/etc/**", Permission.DENY)
 *     .hitlTimeout(Duration.ofMinutes(5))
 *     .build();
 *
 * // 注入 Agent
 * HarnessAgent agent = HarnessAgent.builder()
 *     ...
 *     .permissionContext(config.toPermissionContextState())
 *     .build();
 * }</pre>
 */
public class PermissionConfig {

    /** 默认策略 */
    public enum Permission {
        ALLOW,
        DENY,
        REQUIRE_USER_APPROVAL
    }

    private final Permission defaultPolicy;
    private final Map<String, Permission> toolPolicies;
    private final Map<String, Permission> filePolicies;
    private final Duration hitlTimeout;

    private PermissionConfig(Builder builder) {
        this.defaultPolicy = builder.defaultPolicy;
        this.toolPolicies = Collections.unmodifiableMap(new LinkedHashMap<>(builder.toolPolicies));
        this.filePolicies = Collections.unmodifiableMap(new LinkedHashMap<>(builder.filePolicies));
        this.hitlTimeout = builder.hitlTimeout;
    }

    /**
     * 转换为 AgentScope 框架的 {@link PermissionContextState}。
     */
    public PermissionContextState toPermissionContextState() {
        PermissionContextState.Builder pcsBuilder = PermissionContextState.builder();

        for (Map.Entry<String, Permission> entry : toolPolicies.entrySet()) {
            String toolPattern = entry.getKey();
            PermissionBehavior behavior = toBehavior(entry.getValue());
            PermissionRule rule = new PermissionRule(toolPattern, "*", behavior, "PermissionConfig");
            addRuleToBuilder(pcsBuilder, behavior, "ungrouped", rule);
        }

        // 文件策略映射为文件系统工具的 deny 规则
        for (Map.Entry<String, Permission> entry : filePolicies.entrySet()) {
            String filePattern = entry.getKey();
            PermissionBehavior behavior = toBehavior(entry.getValue());
            if (behavior == PermissionBehavior.DENY) {
                // 对 read_file / write_file / edit_file / execute 生效
                for (String fsTool : List.of("read_file", "write_file", "edit_file", "execute")) {
                    PermissionRule rule = new PermissionRule(fsTool, filePattern, behavior, "PermissionConfig");
                    addRuleToBuilder(pcsBuilder, behavior, "ungrouped", rule);
                }
            }
        }

        return pcsBuilder.build();
    }

    public Permission getDefaultPolicy() { return defaultPolicy; }
    public Map<String, Permission> getToolPolicies() { return toolPolicies; }
    public Map<String, Permission> getFilePolicies() { return filePolicies; }
    public Duration getHitlTimeout() { return hitlTimeout; }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Permission defaultPolicy = Permission.ALLOW;
        private final Map<String, Permission> toolPolicies = new LinkedHashMap<>();
        private final Map<String, Permission> filePolicies = new LinkedHashMap<>();
        private Duration hitlTimeout = Duration.ofMinutes(5);

        /** 默认策略：未匹配到任何规则时的行为 */
        public Builder defaultPolicy(Permission policy) {
            this.defaultPolicy = Objects.requireNonNull(policy);
            return this;
        }

        /**
         * 工具级策略。toolName 支持通配符 {@code *}（如 {@code "db_*"} 匹配 db_query, db_update 等）。
         */
        public Builder toolPolicy(String toolName, Permission policy) {
            toolPolicies.put(toolName, Objects.requireNonNull(policy));
            return this;
        }

        /**
         * 文件路径策略。pattern 支持 Ant 风格通配符（如 {@code "/etc/**"）}。
         * 当前仅支持 {@link Permission#DENY}。
         */
        public Builder filePolicy(String pathPattern, Permission policy) {
            filePolicies.put(pathPattern, Objects.requireNonNull(policy));
            return this;
        }

        /** HITL 审批超时时间（默认 5 分钟） */
        public Builder hitlTimeout(Duration timeout) {
            this.hitlTimeout = Objects.requireNonNull(timeout);
            return this;
        }

        public PermissionConfig build() {
            return new PermissionConfig(this);
        }
    }

    // ---- 内部工具方法 ----

    private static PermissionBehavior toBehavior(Permission p) {
        return switch (p) {
            case ALLOW -> PermissionBehavior.ALLOW;
            case DENY -> PermissionBehavior.DENY;
            case REQUIRE_USER_APPROVAL -> PermissionBehavior.ASK;
        };
    }

    private static void addRuleToBuilder(PermissionContextState.Builder builder,
                                         PermissionBehavior behavior, String group, PermissionRule rule) {
        switch (behavior) {
            case ALLOW -> builder.addAllowRule(group, rule);
            case DENY -> builder.addDenyRule(group, rule);
            case ASK -> builder.addAskRule(group, rule);
        }
    }

    @Override
    public String toString() {
        return "PermissionConfig{default=" + defaultPolicy
                + ", tools=" + toolPolicies
                + ", files=" + filePolicies
                + ", hitlTimeout=" + hitlTimeout + "}";
    }
}
