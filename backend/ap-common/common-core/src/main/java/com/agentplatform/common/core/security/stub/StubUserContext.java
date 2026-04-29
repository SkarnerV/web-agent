package com.agentplatform.common.core.security.stub;

import com.agentplatform.common.core.security.UserContext;
import com.agentplatform.common.core.security.UserPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * MVP 期间的桩实现：返回固定测试用户。
 *
 * <p>非 production 环境激活；接入 W3 OAuth 后由真实实现替换。</p>
 *
 * <p>用户 ID 与 Flyway 种子数据保持一致：{@value #SEED_USER_ID}。</p>
 */
// TODO: 替换为基于 SecurityContextHolder + JWT 的真实 UserContext 实现
@Component
@Profile("!production")
public class StubUserContext implements UserContext {

    public static final String SEED_USER_ID = "11111111-1111-1111-1111-111111111111";
    public static final String SEED_ORG_ID = "22222222-2222-2222-2222-222222222222";

    private static final UserPrincipal DEFAULT = new UserPrincipal(
            UUID.fromString(SEED_USER_ID),
            "测试用户",
            "tester@example.com",
            "user",
            List.of(UUID.fromString(SEED_ORG_ID))
    );

    private static final ThreadLocal<UserPrincipal> OVERRIDE = new ThreadLocal<>();

    @Override
    public UserPrincipal getCurrentUser() {
        UserPrincipal override = OVERRIDE.get();
        return override != null ? override : DEFAULT;
    }

    /**
     * 测试辅助：在当前线程内切换用户身份。请在 finally 中调用 {@link #clear()}。
     */
    public static void setUser(UserPrincipal user) {
        OVERRIDE.set(user);
    }

    public static void clear() {
        OVERRIDE.remove();
    }
}
