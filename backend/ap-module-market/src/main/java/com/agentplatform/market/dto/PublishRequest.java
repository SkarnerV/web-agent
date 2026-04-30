package com.agentplatform.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PublishRequest {

    @NotBlank
    private String assetType;

    @NotNull
    private UUID assetId;

    private String visibility;

    private String version;

    private String releaseNotes;
}
