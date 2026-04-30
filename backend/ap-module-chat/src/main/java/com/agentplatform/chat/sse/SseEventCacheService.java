package com.agentplatform.chat.sse;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Caches SSE events in Redis for reconnection support (§3.8.3).
 * Key format: sse:{message_id}:events → List of serialized events
 * TTL: 5 minutes
 */
@Service
public class SseEventCacheService {

    private static final Duration EVENT_CACHE_TTL = Duration.ofMinutes(5);
    private static final String PREFIX = "sse:";
    private static final String SUFFIX = ":events";
    private static final String SESSION_MSG_PREFIX = "sse:session:";

    private final StringRedisTemplate redisTemplate;

    public SseEventCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Register a session → messageId mapping so reconnection by sessionId can
     * look up the correct message event stream.
     */
    public void registerSessionMessage(String sessionId, String messageId) {
        String key = SESSION_MSG_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, messageId, EVENT_CACHE_TTL);
    }

    /**
     * Resolve the current messageId for a given session.
     * @return messageId or null if no active stream is cached
     */
    public String resolveMessageId(String sessionId) {
        return redisTemplate.opsForValue().get(SESSION_MSG_PREFIX + sessionId);
    }

    /**
     * Append an event to the cache for a given message.
     *
     * @param messageId the message identifier
     * @param eventId   the SSE event id (evt_xxxxx)
     * @param eventType the SSE event type (token, message_start, etc.)
     * @param data      the JSON data payload
     */
    public void appendEvent(String messageId, String eventId, String eventType, String data) {
        String key = PREFIX + messageId + SUFFIX;
        String entry = eventId + "|" + eventType + "|" + data;
        redisTemplate.opsForList().rightPush(key, entry);
        redisTemplate.expire(key, EVENT_CACHE_TTL);
    }

    /**
     * Retrieve events after a given event ID for reconnection.
     * If lastEventId is null or empty, returns all cached events.
     *
     * @param messageId   the message identifier
     * @param lastEventId the last event ID the client received (null/empty = return all)
     * @return list of cached events after the specified ID
     */
    public List<CachedEvent> getEventsAfter(String messageId, String lastEventId) {
        String key = PREFIX + messageId + SUFFIX;
        List<String> allEntries = redisTemplate.opsForList().range(key, 0, -1);
        if (allEntries == null || allEntries.isEmpty()) {
            return List.of();
        }

        boolean returnAll = (lastEventId == null || lastEventId.isEmpty());
        List<CachedEvent> result = new ArrayList<>();
        boolean foundLast = returnAll;

        for (String entry : allEntries) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length < 3) continue;

            if (foundLast) {
                result.add(new CachedEvent(parts[0], parts[1], parts[2]));
            } else if (parts[0].equals(lastEventId)) {
                foundLast = true;
            }
        }

        return result;
    }

    /**
     * Check if events are cached for a message.
     */
    public boolean hasEvents(String messageId) {
        String key = PREFIX + messageId + SUFFIX;
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }

    public record CachedEvent(String eventId, String eventType, String data) {}
}
