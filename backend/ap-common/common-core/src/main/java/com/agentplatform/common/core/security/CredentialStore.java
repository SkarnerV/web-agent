package com.agentplatform.common.core.security;

/**
 * 凭据存储抽象（设计文档 §5.2.9 / R12-1, R12-2）。
 *
 * <p>用于 MCP {@code auth_headers} 与自定义模型 {@code api_key} 的加密、解密与脱敏展示。</p>
 *
 * <p>MVP 期间由 {@link com.agentplatform.common.core.security.stub.PlainCredentialStore} 实现，
 * 后续替换为 AES-256-GCM + Jasypt 实现。</p>
 */
public interface CredentialStore {

    /**
     * 将明文凭据加密为存储形式。
     */
    String encrypt(String plaintext);

    /**
     * 从存储形式解密为明文。
     */
    String decrypt(String stored);

    /**
     * 对存储形式做脱敏处理，用于 API 响应 / 日志输出。
     */
    String mask(String stored);
}
