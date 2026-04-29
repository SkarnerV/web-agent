package com.agentplatform.common.core.security.stub;

import com.agentplatform.common.core.security.CredentialStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * MVP 桩实现：明文存储 + 末 4 位脱敏。
 *
 * <p>脱敏规则（任务 1.8 DoD：{@code mask("sk-abc123xyz")} 返回 {@code "****xyz"}）：
 * <ul>
 *   <li>{@code null} / 空串 → 原样返回</li>
 *   <li>长度 ≤ 4：使用等长 {@code *} 替代，避免泄露</li>
 *   <li>长度 &gt; 4：固定前缀 {@code "****"} + 保留末 3 位，例如 {@code "sk-abc123xyz" → "****xyz"}</li>
 * </ul>
 *
 * <p>MCP 层若需展示 {@code "Bearer "} 等非敏感前缀，可在 McpService 层做二次拼接，
 * 不污染本类的统一脱敏契约。</p>
 */
// TODO: 替换为 AES-256-GCM + Jasypt 实现，密钥通过环境变量注入
@Component
@Profile("!production")
public class PlainCredentialStore implements CredentialStore {

    @Override
    public String encrypt(String plaintext) {
        return plaintext;
    }

    @Override
    public String decrypt(String stored) {
        return stored;
    }

    @Override
    public String mask(String stored) {
        if (stored == null || stored.isEmpty()) {
            return stored;
        }
        int len = stored.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        return "****" + stored.substring(len - 3);
    }
}
