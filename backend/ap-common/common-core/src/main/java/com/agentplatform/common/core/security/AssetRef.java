package com.agentplatform.common.core.security;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 权限判定所需的资产摘要：归属、可见性、是否已被软删除。
 *
 * <p>各资产模块在向 {@link PermissionChecker} 提交请求时构造此对象。</p>
 */
public record AssetRef(
        UUID id,
        UUID ownerId,
        AssetVisibility visibility,
        OffsetDateTime deletedAt
) {
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
