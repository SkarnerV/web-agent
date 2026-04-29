package com.agentplatform.common.core.security;

import java.util.List;
import java.util.UUID;

/**
 * 当前请求用户身份。
 */
public record UserPrincipal(
        UUID id,
        String name,
        String email,
        String role,
        List<UUID> orgIds
) {
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
