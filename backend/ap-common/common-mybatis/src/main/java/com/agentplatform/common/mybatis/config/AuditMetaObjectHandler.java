package com.agentplatform.common.mybatis.config;

import com.agentplatform.common.core.security.UserContext;
import com.agentplatform.common.core.security.UserPrincipal;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 自动填充 {@code created_at / updated_at / created_by / updated_by / version}。
 *
 * <p>{@link UserContext} 通过 {@link ObjectProvider} 延迟解析，避免单元测试或非 Web 场景下因缺失 Bean 而初始化失败。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private final ObjectProvider<UserContext> userContextProvider;

    public AuditMetaObjectHandler(ObjectProvider<UserContext> userContextProvider) {
        this.userContextProvider = userContextProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        // Auto-generate UUID primary key if the entity has a UUID id field and it is not yet set
        if (metaObject.hasSetter("id")) {
            Object existingId = metaObject.getValue("id");
            if (existingId == null) {
                Class<?> idType = metaObject.getGetterType("id");
                if (UUID.class.equals(idType)) {
                    metaObject.setValue("id", UUID.randomUUID());
                }
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        UUID userId = currentUserId();
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
        if (userId != null) {
            strictInsertFill(metaObject, "createdBy", UUID.class, userId);
            strictInsertFill(metaObject, "updatedBy", UUID.class, userId);
        }
        strictInsertFill(metaObject, "version", Long.class, 0L);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
        UUID userId = currentUserId();
        if (userId != null) {
            strictUpdateFill(metaObject, "updatedBy", UUID.class, userId);
        }
    }

    private UUID currentUserId() {
        UserContext ctx = userContextProvider.getIfAvailable();
        if (ctx == null) {
            return null;
        }
        UserPrincipal user = ctx.getCurrentUser();
        return user == null ? null : user.id();
    }
}
