package com.agentplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent Platform — modular monolith bootstrap.
 *
 * <p>组件扫描根包统一为 {@code com.agentplatform}；各模块的 Mapper 扫描随后续任务
 * （详见 tasks 2.x）按模块分别声明 {@code @MapperScan}。</p>
 */
@SpringBootApplication(scanBasePackages = "com.agentplatform")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
