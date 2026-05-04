package com.agentplatform.common.core.embedding.stub;

import com.agentplatform.common.core.embedding.EmbeddingClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!production")
public class RandomEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSIONS = 1536;
    private final SecureRandom random = new SecureRandom();

    @Override
    public List<Float> embed(String text) {
        List<Float> vector = new ArrayList<>(DIMENSIONS);
        for (int i = 0; i < DIMENSIONS; i++) {
            vector.add(random.nextFloat(-1f, 1f));
        }
        return vector;
    }
}
