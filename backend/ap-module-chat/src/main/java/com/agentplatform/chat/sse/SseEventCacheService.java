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

    private final StringRedisTemplate redisTemplate;

    public SseEventCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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
     *
     * @param messageId   the message identifier
     * @param lastEventId the last event ID the client received
     * @return list of cached events after the specified ID
     */
    public List<CachedEvent> getEventsAfter(String messageId, String lastEventId) {
        String key = PREFIX + messageId + SUFFIX;
        List<String> allEntries = redisTemplate.opsForList().range(key, 0, -1);
        if (allEntries == null || allEntries.isEmpty()) {
            return List.of();
        }

        List<CachedEvent> result = new ArrayList<>();
        boolean foundLast = false;

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
