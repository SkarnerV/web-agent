package com.agentplatform.common.core.security.stub;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.core.security.AssetVisibility;
import com.agentplatform.common.core.security.Permission;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimplePermissionCheckerTest {

    private final SimplePermissionChecker checker = new SimplePermissionChecker();

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    private AssetRef privateAsset() {
        return new AssetRef(UUID.randomUUID(), owner, AssetVisibility.PRIVATE, null);
    }

    private AssetRef publicAsset() {
        return new AssetRef(UUID.randomUUID(), owner, AssetVisibility.PUBLIC, null);
    }

    @Test
    void ownerHasAllPermissions() {
        AssetRef asset = privateAsset();
        for (Permission p : Permission.values()) {
            checker.checkAccess(owner, asset, p);
        }
    }

    @Test
    void otherUserCannotReadPrivate() {
        assertThatThrownBy(() -> checker.checkAccess(other, privateAsset(), Permission.READ))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ASSET_PERMISSION_DENIED);
    }

    @Test
    void otherUserCanReadPublic() {
        checker.checkAccess(other, publicAsset(), Permission.READ);
        assertThat(checker.canAccess(other, publicAsset(), Permission.READ)).isTrue();
    }

    @Test
    void otherUserCannotWriteEvenPublic() {
        assertThatThrownBy(() -> checker.checkAccess(other, publicAsset(), Permission.WRITE))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ASSET_PERMISSION_DENIED);
    }

    @Test
    void deletedAssetReturnsNotFound() {
        AssetRef deleted = new AssetRef(UUID.randomUUID(), owner, AssetVisibility.PUBLIC, OffsetDateTime.now());
        assertThatThrownBy(() -> checker.checkAccess(owner, deleted, Permission.READ))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
    }
}
