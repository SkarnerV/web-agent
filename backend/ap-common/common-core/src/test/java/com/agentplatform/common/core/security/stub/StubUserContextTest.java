package com.agentplatform.common.core.security.stub;

import com.agentplatform.common.core.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StubUserContextTest {

    private final StubUserContext ctx = new StubUserContext();

    @AfterEach
    void cleanup() {
        StubUserContext.clear();
    }

    @Test
    void returnsSeededDefaultUser() {
        UserPrincipal user = ctx.getCurrentUser();
        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(UUID.fromString(StubUserContext.SEED_USER_ID));
        assertThat(ctx.getCurrentUserId()).isEqualTo(user.id());
        assertThat(user.orgIds()).contains(UUID.fromString(StubUserContext.SEED_ORG_ID));
    }

    @Test
    void overrideSwitchesIdentity() {
        UUID anotherId = UUID.randomUUID();
        UserPrincipal another = new UserPrincipal(anotherId, "userB", "b@example.com", "user", List.of());
        StubUserContext.setUser(another);

        assertThat(ctx.getCurrentUserId()).isEqualTo(anotherId);
        assertThat(ctx.getCurrentUser().name()).isEqualTo("userB");
    }
}
