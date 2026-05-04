package com.agentplatform.common.core.knowledge.stub;

import com.agentplatform.common.core.knowledge.KbIndexMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!production")
public class LoggingKbIndexMessageSender implements KbIndexMessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingKbIndexMessageSender.class);

    @Override
    public void sendIndex(UUID documentId) {
        log.info("Index message queued for document {} (kb.index)", documentId);
    }
}
