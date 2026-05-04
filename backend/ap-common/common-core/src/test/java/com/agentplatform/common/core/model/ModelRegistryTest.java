package com.agentplatform.common.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelRegistryTest {

    private StubModelRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StubModelRegistry();
    }

    @Nested
    class ModelInfoProperties {

        @Test
        void sourceEnum() {
            assertThat(ModelInfo.Source.BUILTIN).isNotNull();
            assertThat(ModelInfo.Source.CUSTOM).isNotNull();
        }

        @Test
        void settersAndGetters() {
            ModelInfo info = new ModelInfo();
            info.setId("gpt-4o");
            info.setName("GPT-4o");
            info.setProvider("openai");
            info.setSource(ModelInfo.Source.BUILTIN);
            info.setDescription("OpenAI GPT-4o model");
            info.setDefault(true);
            info.setEnabled(true);
            info.setSortOrder(1);
            info.setApiUrl("https://api.openai.com");
            info.setApiKeyMasked("****1234");
            info.setConnectionStatus("ok");
            info.setConfig(Map.of("temperature", 0.7));

            assertThat(info.getId()).isEqualTo("gpt-4o");
            assertThat(info.getName()).isEqualTo("GPT-4o");
            assertThat(info.getProvider()).isEqualTo("openai");
            assertThat(info.getSource()).isEqualTo(ModelInfo.Source.BUILTIN);
            assertThat(info.isDefault()).isTrue();
            assertThat(info.isEnabled()).isTrue();
            assertThat(info.getSortOrder()).isEqualTo(1);
            assertThat(info.getApiUrl()).isEqualTo("https://api.openai.com");
            assertThat(info.getApiKeyMasked()).isEqualTo("****1234");
            assertThat(info.getConnectionStatus()).isEqualTo("ok");
            assertThat(info.getConfig()).containsEntry("temperature", 0.7);
        }
    }

    @Nested
    class GetAllModels {

        @Test
        void mergesBuiltinAndCustom() {
            UUID userId = UUID.randomUUID();
            // Add a custom model for this user so the merge test sees both types
            registry.addCustomModel(userId, "My Custom Model", "https://custom.example.com");
            List<ModelInfo> models = registry.getAllModels(userId);
            assertThat(models).isNotEmpty();
            assertThat(models).extracting(ModelInfo::getSource)
                    .contains(ModelInfo.Source.BUILTIN, ModelInfo.Source.CUSTOM);
        }

        @Test
        void customsArePerUser() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();

            registry.addCustomModel(userA, "A's model", "https://a.example.com");
            registry.addCustomModel(userB, "B's model", "https://b.example.com");

            List<ModelInfo> modelsA = registry.getAllModels(userA);
            List<ModelInfo> modelsB = registry.getAllModels(userB);

            assertThat(modelsA).extracting(ModelInfo::getName).contains("A's model");
            assertThat(modelsA).extracting(ModelInfo::getName).doesNotContain("B's model");
            assertThat(modelsB).extracting(ModelInfo::getName).contains("B's model");
        }
    }

    @Nested
    class GetById {

        @Test
        void findsBuiltinModel() {
            Optional<ModelInfo> model = registry.getById("gpt-4o");
            assertThat(model).isPresent();
            assertThat(model.get().getSource()).isEqualTo(ModelInfo.Source.BUILTIN);
        }

        @Test
        void findsCustomModel() {
            UUID userId = UUID.randomUUID();
            String id = registry.addCustomModel(userId, "My Model", "https://example.com");

            Optional<ModelInfo> model = registry.getById(id);
            assertThat(model).isPresent();
            assertThat(model.get().getName()).isEqualTo("My Model");
            assertThat(model.get().getSource()).isEqualTo(ModelInfo.Source.CUSTOM);
            assertThat(model.get().getApiKeyMasked()).isNotNull();
        }

        @Test
        void returnsEmptyForUnknownId() {
            assertThat(registry.getById("nonexistent")).isEmpty();
            assertThat(registry.getById(null)).isEmpty();
            assertThat(registry.getById("")).isEmpty();
        }
    }

    @Nested
    class DeleteCustomModel {

        @Test
        void resetsAgentsToDefault() {
            UUID userId = UUID.randomUUID();
            String customId = registry.addCustomModel(userId, "To Delete", "https://example.com");
            registry.bindAgentToModel(UUID.randomUUID(), customId);

            registry.deleteCustomModel(UUID.fromString(customId), userId);

            // Model is gone
            assertThat(registry.getById(customId)).isEmpty();
            // All agents that used it now use the default
            assertThat(registry.agentModelIds.values()).containsOnly("gpt-4o");
        }

        @Test
        void throwsForNonOwner() {
            UUID owner = UUID.randomUUID();
            UUID other = UUID.randomUUID();
            String customId = registry.addCustomModel(owner, "Owner Model", "https://example.com");

            assertThatThrownBy(() -> registry.deleteCustomModel(UUID.fromString(customId), other))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Permission denied");
        }

        @Test
        void throwsForNonexistentModel() {
            assertThatThrownBy(() -> registry.deleteCustomModel(UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    class DefaultModel {

        @Test
        void returnsDefaultModelId() {
            assertThat(registry.getDefaultModelId()).isEqualTo("gpt-4o");
        }
    }

    @Nested
    class BuildChatClient {

        @Test
        void returnsPlaceholder() {
            ModelInfo model = registry.getById("gpt-4o").orElseThrow();
            Map<String, Object> client = registry.buildChatClient(model);

            assertThat(client).containsEntry("model_id", "gpt-4o");
            assertThat(client).containsEntry("placeholder", true);
        }
    }

    // ─── Stub implementation ───

    static class StubModelRegistry implements ModelRegistry {
        final Map<String, ModelInfo> models = new LinkedHashMap<>();
        final Map<String, UUID> modelOwners = new HashMap<>(); // modelId → ownerId
        final Map<UUID, String> agentModelIds = new HashMap<>(); // agentId → modelId

        StubModelRegistry() {
            // Seed builtin models
            ModelInfo gpt4o = new ModelInfo();
            gpt4o.setId("gpt-4o");
            gpt4o.setName("GPT-4o");
            gpt4o.setProvider("openai");
            gpt4o.setSource(ModelInfo.Source.BUILTIN);
            gpt4o.setDescription("OpenAI GPT-4o");
            gpt4o.setDefault(true);
            gpt4o.setEnabled(true);
            gpt4o.setSortOrder(1);
            models.put("gpt-4o", gpt4o);

            ModelInfo gpt4mini = new ModelInfo();
            gpt4mini.setId("gpt-4o-mini");
            gpt4mini.setName("GPT-4o Mini");
            gpt4mini.setProvider("openai");
            gpt4mini.setSource(ModelInfo.Source.BUILTIN);
            gpt4mini.setDescription("OpenAI GPT-4o Mini");
            gpt4mini.setEnabled(true);
            gpt4mini.setSortOrder(2);
            models.put("gpt-4o-mini", gpt4mini);
        }

        @Override
        public List<ModelInfo> getAllModels(UUID userId) {
            List<ModelInfo> result = new ArrayList<>();
            for (ModelInfo m : models.values()) {
                boolean isCustom = m.getSource() == ModelInfo.Source.CUSTOM;
                UUID owner = modelOwners.get(m.getId());
                if (isCustom && !userId.equals(owner)) continue;
                if (m.isEnabled()) {
                    result.add(m);
                }
            }
            return result;
        }

        @Override
        public Optional<ModelInfo> getById(String modelId) {
            if (modelId == null || modelId.isBlank()) return Optional.empty();
            return Optional.ofNullable(models.get(modelId));
        }

        @Override
        public void deleteCustomModel(UUID modelId, UUID userId) {
            String id = modelId.toString();
            ModelInfo model = models.get(id);
            if (model == null) throw new RuntimeException("not found");
            UUID owner = modelOwners.get(id);
            if (!userId.equals(owner)) throw new RuntimeException("Permission denied");

            String defaultId = getDefaultModelId();
            agentModelIds.replaceAll((aid, mid) -> mid.equals(id) ? defaultId : mid);
            models.remove(id);
            modelOwners.remove(id);
        }

        @Override
        public String getDefaultModelId() {
            return "gpt-4o";
        }

        @Override
        public Map<String, Object> buildChatClient(ModelInfo model) {
            return Map.of(
                    "model_id", model.getId(),
                    "model_name", model.getName(),
                    "provider", model.getProvider() != null ? model.getProvider() : "unknown",
                    "placeholder", true);
        }

        String addCustomModel(UUID userId, String name, String apiUrl) {
            String id = UUID.randomUUID().toString();
            ModelInfo info = new ModelInfo();
            info.setId(id);
            info.setName(name);
            info.setProvider("custom");
            info.setSource(ModelInfo.Source.CUSTOM);
            info.setApiUrl(apiUrl);
            info.setApiKeyMasked("****key");
            info.setEnabled(true);
            models.put(id, info);
            modelOwners.put(id, userId);
            return id;
        }

        void bindAgentToModel(UUID agentId, String modelId) {
            agentModelIds.put(agentId, modelId);
        }
    }
}
