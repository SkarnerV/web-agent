package com.agentplatform.market.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MarketItemDetailVO extends MarketItemVO {

    private String configSnapshot;
}
