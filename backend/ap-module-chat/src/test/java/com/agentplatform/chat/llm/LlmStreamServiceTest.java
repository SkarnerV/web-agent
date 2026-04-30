package com.agentplatform.chat.llm;

import com.agentplatform.common.core.error.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DefaultLlmStreamService — 4.2 LLM Stream")
class LlmStreamServiceTest {

    private DefaultLlmStreamService service;

    @BeforeEach
    void setUp() {
        service = new DefaultLlmStreamService();
    }

    @Test
    @DisplayName("stream returns token chunks followed by finish chunk")
    void streamReturnsTokensAndFinish() {
        List<LlmMessage> messages = List.of(LlmMessage.user("Hello"));
        Iterator<LlmChunk> iter = service.stream("gpt-4o", messages, List.of());

        List<LlmChunk> chunks = new ArrayList<>();
        iter.forEachRemaining(chunks::add);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getLast()).isInstanceOf(LlmChunk.FinishChunk.class);

        long tokenCount = chunks.stream()
                .filter(c -> c instanceof LlmChunk.TokenChunk)
                .count();
        assertThat(tokenCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("stream with null modelId throws CHAT_MODEL_ERROR")
    void streamWithNullModelThrows() {
        List<LlmMessage> messages = List.of(LlmMessage.user("Hello"));

        assertThatThrownBy(() -> service.stream(null, messages, List.of()))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("stream with blank modelId throws CHAT_MODEL_ERROR")
    void streamWithBlankModelThrows() {
        List<LlmMessage> messages = List.of(LlmMessage.user("Hello"));

        assertThatThrownBy(() -> service.stream("", messages, List.of()))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("finish chunk contains token usage")
    void finishChunkHasUsage() {
        List<LlmMessage> messages = List.of(LlmMessage.user("Hi"));
        Iterator<LlmChunk> iter = service.stream("gpt-4o", messages, List.of());

        LlmChunk.FinishChunk finish = null;
        while (iter.hasNext()) {
            LlmChunk chunk = iter.next();
            if (chunk instanceof LlmChunk.FinishChunk fc) {
                finish = fc;
            }
        }

        assertThat(finish).isNotNull();
        assertThat(finish.promptTokens()).isGreaterThan(0);
        assertThat(finish.completionTokens()).isGreaterThan(0);
    }
}
