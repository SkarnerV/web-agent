package com.agentplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.agentplatform.mapper",
        "com.agentplatform.common.mybatis.mapper",
        "com.agentplatform.agent.mapper",
        "com.agentplatform.chat.mapper",
        "com.agentplatform.asset.mapper",
        "com.agentplatform.market.mapper",
        "com.agentplatform.file.mapper"
})
class MybatisMapperConfig {
}
