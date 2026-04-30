package com.agentplatform.agent.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AssetVersionVO {

    private UUID id;

    private String assetType;

    private UUID assetId;

    private String version;

    private String releaseNotes;

    private UUID publishedBy;

    private OffsetDateTime publishedAt;
}
