package com.agentplatform.agent.service;

import com.agentplatform.agent.dto.BuiltinModelVO;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.model.ModelRegistry;
import com.agentplatform.common.core.security.CredentialStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelQueryTest {

    @Mock private ModelRegistry modelRegistry;
    @Mock private CredentialStore credentialStore;
    @Mock private RestClient.Builder restClientBuilder;

    private ModelService modelService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        modelService = new ModelService(modelRegistry, credentialStore, restClientBuilder);
    }

    @Nested
    @DisplayName("List builtin models")
    class ListBuiltinModels {

        @Test
        @DisplayName("returns enabled builtin models")
        void returnsEnabledBuiltinModels() {
            ModelInfo gpt4 = builtinModel("gpt-4o", "GPT-4o", "openai", "GPT-4o description", true, 1);
            ModelInfo gpt4mini = builtinModel("gpt-4o-mini", "GPT-4o Mini", "openai", "Smaller model", false, 2);
            ModelInfo claude = builtinModel("claude-3-5-sonnet", "Claude 3.5 Sonnet", "anthropic", "Claude model", false, 3);

            when(modelRegistry.getBuiltinModels()).thenReturn(List.of(gpt4, gpt4mini, claude));

            List<BuiltinModelVO> result = modelService.listBuiltinModels();

            assertThat(result).hasSize(3);
            assertThat(result).extracting(BuiltinModelVO::getId)
                    .containsExactly("gpt-4o", "gpt-4o-mini", "claude-3-5-sonnet");
            assertThat(result).extracting(BuiltinModelVO::getProvider)
                    .containsExactly("openai", "openai", "anthropic");
            assertThat(result.get(0).getIsDefault()).isTrue();
            assertThat(result.get(1).getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("returns empty list when no builtin models")
        void returnsEmptyWhenNoBuiltinModels() {
            when(modelRegistry.getBuiltinModels()).thenReturn(List.of());

            List<BuiltinModelVO> result = modelService.listBuiltinModels();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("List all models (builtin + custom)")
    class ListAllModels {

        @Test
        @DisplayName("merges builtin and custom models with correct source")
        void mergesBuiltinAndCustom() {
            ModelInfo builtin1 = builtinModel("gpt-4o", "GPT-4o", "openai", "desc", true, 1);
            ModelInfo builtin2 = builtinModel("gpt-4o-mini", "GPT-4o Mini", "openai", "desc", false, 2);
            ModelInfo builtin3 = builtinModel("claude-3-5-sonnet", "Claude", "anthropic", "desc", false, 3);
            ModelInfo custom1 = customModel("my-model-1", "My Model 1", "https://api.a.com", "***key1", "ok");
            ModelInfo custom2 = customModel("my-model-2", "My Model 2", "https://api.b.com", "***key2", "ok");

            when(modelRegistry.getAllModels(USER_A))
                    .thenReturn(List.of(builtin1, builtin2, builtin3, custom1, custom2));

            List<ModelInfo> result = modelService.listAllModels(USER_A);

            assertThat(result).hasSize(5);
            assertThat(result).extracting(ModelInfo::getSource)
                    .containsExactly(
                            ModelInfo.Source.BUILTIN, ModelInfo.Source.BUILTIN,
                            ModelInfo.Source.BUILTIN, ModelInfo.Source.CUSTOM, ModelInfo.Source.CUSTOM);
        }

        @Test
        @DisplayName("exactly one default model in builtin list")
        void exactlyOneDefault() {
            ModelInfo defaultModel = builtinModel("gpt-4o", "GPT-4o", "openai", "desc", true, 1);
            ModelInfo nonDefault1 = builtinModel("gpt-4o-mini", "GPT-4o Mini", "openai", "desc", false, 2);
            ModelInfo nonDefault2 = builtinModel("claude", "Claude", "anthropic", "desc", false, 3);

            when(modelRegistry.getAllModels(USER_A))
                    .thenReturn(List.of(defaultModel, nonDefault1, nonDefault2));

            List<ModelInfo> result = modelService.listAllModels(USER_A);

            long defaultCount = result.stream().filter(ModelInfo::isDefault).count();
            assertThat(defaultCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("List custom models")
    class ListCustomModels {

        @Test
        @DisplayName("returns user custom models with masked API key")
        void returnsCustomWithMaskedKey() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            ModelInfo custom1 = customModel(id1.toString(), "My GPT", "https://api.example.com", "***abc", "ok");
            ModelInfo custom2 = customModel(id2.toString(), "My Claude", "https://api.other.com", "***xyz", "failed");

            when(modelRegistry.getCustomModels(USER_A)).thenReturn(List.of(custom1, custom2));

            List<CustomModelVO> result = modelService.listCustomModels(USER_A);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CustomModelVO::getName)
                    .containsExactly("My GPT", "My Claude");
            assertThat(result).extracting(CustomModelVO::getApiKeyMasked)
                    .containsExactly("***abc", "***xyz");
            assertThat(result).extracting(CustomModelVO::getConnectionStatus)
                    .containsExactly("ok", "failed");
            assertThat(result.get(0).getApiUrl()).isEqualTo("https://api.example.com");
        }
    }

    // ─── helper factories ───

    private ModelInfo builtinModel(String id, String name, String provider, String desc,
                                    boolean isDefault, int sortOrder) {
        ModelInfo info = new ModelInfo();
        info.setId(id);
        info.setName(name);
        info.setProvider(provider);
        info.setSource(ModelInfo.Source.BUILTIN);
        info.setDescription(desc);
        info.setDefault(isDefault);
        info.setEnabled(true);
        info.setSortOrder(sortOrder);
        return info;
    }

    private ModelInfo customModel(String id, String name, String apiUrl, String apiKeyMasked, String status) {
        ModelInfo info = new ModelInfo();
        info.setId(id);
        info.setName(name);
        info.setApiUrl(apiUrl);
        info.setApiKeyMasked(apiKeyMasked);
        info.setConnectionStatus(status);
        info.setProvider("custom");
        info.setSource(ModelInfo.Source.CUSTOM);
        info.setEnabled(true);
        return info;
    }
}
