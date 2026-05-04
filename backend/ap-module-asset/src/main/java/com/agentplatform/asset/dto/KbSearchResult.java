package com.agentplatform.asset.dto;

import lombok.Data;

@Data
public class KbSearchResult {

    private String content;
    private double score;
    private String documentName;
    private int chunkIndex;
}
