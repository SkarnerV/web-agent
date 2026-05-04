package com.agentplatform.asset.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KbSearchRequest {

    @NotBlank
    private String query;

    @Min(1)
    @Max(100)
    private int topK = 5;
}
