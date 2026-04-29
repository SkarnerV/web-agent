package com.agentplatform.common.core.security;

import java.util.UUID;

/**
 * 资产权限判定接口（设计文档 §3.6）。
 *
 * <p>MVP 由 {@link com.agentplatform.common.core.security.stub.SimplePermissionChecker} 实现，
 * 仅校验 owner 与可见性；接入 RBAC 后由完整版本替换，签名不变。</p>
 */
public interface PermissionChecker {

    /**
     * 校验通过返回；失败抛出 {@link com.agentplatform.common.core.error.BizException}。
     *
     * @param userId   当前用户 ID
     * @param asset    目标资产摘要
     * @param required 所需权限
     */
    void checkAccess(UUID userId, AssetRef asset, Permission required);

    /**
     * 仅判断是否允许，不抛异常。
     */
    boolean canAccess(UUID userId, AssetRef asset, Permission required);
}
