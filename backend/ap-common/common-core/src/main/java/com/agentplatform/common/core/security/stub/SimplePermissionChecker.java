package com.agentplatform.common.core.security.stub;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.core.security.AssetVisibility;
import com.agentplatform.common.core.security.Permission;
import com.agentplatform.common.core.security.PermissionChecker;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MVP 简化权限检查：
 * <ul>
 *   <li>软删除资产对所有人不可见 → ASSET_NOT_FOUND</li>
 *   <li>owner 拥有全部权限</li>
 *   <li>非 owner 访问 PRIVATE → ASSET_PERMISSION_DENIED</li>
 *   <li>非 owner 访问 PUBLIC / GROUP_* → 一律放行（暂不查组织关系）</li>
 *   <li>WRITE / DELETE / PUBLISH 仅 owner 允许</li>
 * </ul>
 *
 * <p>注意：组织内编辑/只读判定、admin 旁路等行为 MVP 不实现，对应错误码沿用设计文档。</p>
 */
// TODO: 替换为完整 §3.6 权限矩阵（含组织成员关系 + 角色 + admin 旁路）
@Component
public class SimplePermissionChecker implements PermissionChecker {

    @Override
    public void checkAccess(UUID userId, AssetRef asset, Permission required) {
        if (asset == null || asset.isDeleted()) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }

        boolean isOwner = userId != null && userId.equals(asset.ownerId());
        if (isOwner) {
            return;
        }

        // 非 owner 的写/删除/发布操作一律拒绝
        if (required == Permission.WRITE || required == Permission.DELETE || required == Permission.PUBLISH) {
            throw new BizException(ErrorCode.ASSET_PERMISSION_DENIED);
        }

        // 非 owner 读：仅 PRIVATE 拒绝，其余放行
        if (asset.visibility() == AssetVisibility.PRIVATE) {
            throw new BizException(ErrorCode.ASSET_PERMISSION_DENIED);
        }
    }

    @Override
    public boolean canAccess(UUID userId, AssetRef asset, Permission required) {
        try {
            checkAccess(userId, asset, required);
            return true;
        } catch (BizException ex) {
            return false;
        }
    }
}
