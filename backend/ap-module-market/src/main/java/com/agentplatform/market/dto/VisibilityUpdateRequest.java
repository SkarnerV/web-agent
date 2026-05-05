package com.agentplatform.market.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VisibilityUpdateRequest {

    @NotBlank
    private String visibility;
}
