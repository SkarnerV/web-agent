package com.agentplatform.chat.sse;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Implements idempotency check for chat message sending (§3.8.3).
 * Key format: idem:{session_id}:{idempotency_key} → {message_id, body_hash, status}
 */
@Service
public class IdempotencyService {

    private static final Duration IDEM_TTL = Duration.ofMinutes(5);
    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check idempotency for a message send request.
     *
     * @return existing messageId if this is a duplicate request, null if this is a new request
     * @throws BizException CHAT_IDEMPOTENCY_CONFLICT if same key but different body
     */
    public String checkAndRegister(UUID sessionId, String idempotencyKey, String requestBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String redisKey = PREFIX + sessionId + ":" + idempotencyKey;
        String bodyHash = sha256(requestBody);

        String existing = redisTemplate.opsForValue().get(redisKey);
        if (existing != null) {
            String[] parts = existing.split("\\|");
            if (parts.length >= 2) {
                String storedHash = parts[1];
                if (bodyHash.equals(storedHash)) {
                    return parts[0]; // return existing message_id
                } else {
                    throw new BizException(ErrorCode.CHAT_IDEMPOTENCY_CONFLICT,
                            Map.of("idempotency_key", idempotencyKey));
                }
            }
        }

        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String value = messageId + "|" + bodyHash + "|pending";
        redisTemplate.opsForValue().set(redisKey, value, IDEM_TTL);
        return null;
    }

    public void markComplete(UUID sessionId, String idempotencyKey, String messageId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        String redisKey = PREFIX + sessionId + ":" + idempotencyKey;
        String existing = redisTemplate.opsForValue().get(redisKey);
        if (existing != null) {
            String[] parts = existing.split("\\|");
            if (parts.length >= 2) {
                String updated = messageId + "|" + parts[1] + "|complete";
                redisTemplate.opsForValue().set(redisKey, updated, IDEM_TTL);
            }
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
