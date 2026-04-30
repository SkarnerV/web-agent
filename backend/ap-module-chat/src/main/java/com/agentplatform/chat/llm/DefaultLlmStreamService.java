package com.agentplatform.chat.llm;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Default LLM stream service implementation.
 * TODO: Integrate with Spring AI ChatClient and ModelRegistry.buildChatClient(modelId)
 *
 * Currently provides a stub that returns a simple text response for development/testing.
 * Will be replaced with actual Spring AI integration when ModelRegistry is implemented (task 5.2).
 */
@Service
public class DefaultLlmStreamService implements LlmStreamService {

    private static final Logger log = LoggerFactory.getLogger(DefaultLlmStreamService.class);

    @Override
    public Iterator<LlmChunk> stream(String modelId, List<LlmMessage> messages, List<Map<String, Object>> tools) {
        if (modelId == null || modelId.isBlank()) {
            throw new BizException(ErrorCode.CHAT_MODEL_ERROR, Map.of("reason", "model_id is null"));
        }

        log.debug("LLM stream request: model={}, messages={}, tools={}",
                modelId, messages.size(), tools != null ? tools.size() : 0);

        // TODO: Replace with actual Spring AI ChatClient streaming call
        // ModelInfo model = modelRegistry.getById(modelId);
        // ChatClient client = modelRegistry.buildChatClient(model);
        // return client.stream(...)

        List<LlmChunk> chunks = new ArrayList<>();
        chunks.add(new LlmChunk.TokenChunk("我"));
        chunks.add(new LlmChunk.TokenChunk("是"));
        chunks.add(new LlmChunk.TokenChunk("AI"));
        chunks.add(new LlmChunk.TokenChunk("助手"));
        chunks.add(new LlmChunk.TokenChunk("，"));
        chunks.add(new LlmChunk.TokenChunk("有什么"));
        chunks.add(new LlmChunk.TokenChunk("可以"));
        chunks.add(new LlmChunk.TokenChunk("帮助"));
        chunks.add(new LlmChunk.TokenChunk("您的？"));
        chunks.add(new LlmChunk.FinishChunk("stop", 50, 20));
        return chunks.iterator();
    }
}
