package com.agentplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.agentplatform")
@MapperScan({
        "com.agentplatform.mapper",
        "com.agentplatform.agent.mapper",
        "com.agentplatform.chat.mapper",
        "com.agentplatform.asset.mapper",
        "com.agentplatform.market.mapper",
        "com.agentplatform.file.mapper"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
