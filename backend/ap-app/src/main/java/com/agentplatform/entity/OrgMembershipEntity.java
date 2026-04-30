package com.agentplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
@TableName("org_memberships")
public class OrgMembershipEntity {

    private UUID userId;

    private UUID orgId;

    private String roleInOrg;

    private OffsetDateTime syncedAt;
}
