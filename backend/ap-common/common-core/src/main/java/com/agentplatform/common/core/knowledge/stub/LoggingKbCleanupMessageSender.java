package com.agentplatform.common.core.knowledge.stub;

import com.agentplatform.common.core.knowledge.KbCleanupMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MVP stub: logs the cleanup event instead of sending a RabbitMQ message.
 * Will be replaced by a real RabbitMQ sender when message infrastructure is in place.
 */
@Component
@Profile("!production")
public class LoggingKbCleanupMessageSender implements KbCleanupMessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingKbCleanupMessageSender.class);

    @Override
    public void sendCleanup(UUID knowledgeBaseId) {
        log.info("Cleanup message queued for knowledge base {} (kb.cleanup)", knowledgeBaseId);
    }
}
