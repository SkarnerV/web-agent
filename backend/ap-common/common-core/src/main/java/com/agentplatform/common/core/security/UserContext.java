package com.agentplatform.common.core.security;

import java.util.UUID;

/**
 * 当前用户上下文。MVP 期间由 {@link com.agentplatform.common.core.security.stub.StubUserContext}
 * 提供固定测试用户；接入 W3 OAuth 后替换为基于 SecurityContext 的实现，调用方契约不变。
 */
public interface UserContext {

    UserPrincipal getCurrentUser();

    default UUID getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user == null ? null : user.id();
    }
}
