package com.agentplatform.agent.provider;

import com.agentplatform.agent.entity.BuiltinModelEntity;
import com.agentplatform.agent.entity.CustomModelEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.BuiltinModelMapper;
import com.agentplatform.agent.mapper.CustomModelMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.model.ModelInfo;
import com.agentplatform.common.core.security.CredentialStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelRegistryImplTest {

    @Mock private BuiltinModelMapper builtinModelMapper;
    @Mock private CustomModelMapper customModelMapper;
    @Mock private AgentMapper agentMapper;
    @Mock private CredentialStore credentialStore;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ModelRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new ModelRegistryImpl(builtinModelMapper, customModelMapper,
                agentMapper, credentialStore, objectMapper);
    }

    @Nested
    class GetAllModels {

        @Test
        void mergesBuiltinAndCustomModels() {
            UUID userId = UUID.randomUUID();

            BuiltinModelEntity b1 = builtinEntity("gpt-4o", "GPT-4o", "openai", true, 1);
            BuiltinModelEntity b2 = builtinEntity("gpt-4o-mini", "GPT-4o Mini", "openai", false, 2);
            CustomModelEntity c1 = customEntity(userId, "My Model", "https://api.example.com", "sk-key");

            when(builtinModelMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(b1, b2));
            when(customModelMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(c1));
            when(credentialStore.mask("sk-key")).thenReturn("****key");

            List<ModelInfo> result = registry.getAllModels(userId);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(ModelInfo::getSource)
                    .containsExactly(ModelInfo.Source.BUILTIN, ModelInfo.Source.BUILTIN, ModelInfo.Source.CUSTOM);
            assertThat(result).extracting(ModelInfo::getName)
                    .containsExactly("GPT-4o", "GPT-4o Mini", "My Model");
            assertThat(result.get(2).getApiUrl()).isEqualTo("https://api.example.com");
            assertThat(result.get(2).getApiKeyMasked()).isEqualTo("****key");
        }

        @Test
        void returnsEmptyListWhenNoModels() {
            UUID userId = UUID.randomUUID();
            when(builtinModelMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(customModelMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<ModelInfo> result = registry.getAllModels(userId);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetById {

        @Test
        void findsBuiltinModel() {
            BuiltinModelEntity entity = builtinEntity("gpt-4o", "GPT-4o", "openai", true, 1);
            when(builtinModelMapper.selectById("gpt-4o")).thenReturn(entity);

            Optional<ModelInfo> result = registry.getById("gpt-4o");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("gpt-4o");
            assertThat(result.get().getSource()).isEqualTo(ModelInfo.Source.BUILTIN);
            assertThat(result.get().isDefault()).isTrue();
        }

        @Test
        void findsCustomModel() {
            UUID modelUuid = UUID.randomUUID();
            CustomModelEntity entity = customEntity(modelUuid, UUID.randomUUID(),
                    "Custom GPT", "https://api.example.com", "sk-abc");
            when(builtinModelMapper.selectById(modelUuid.toString())).thenReturn(null);
            when(customModelMapper.selectById(modelUuid)).thenReturn(entity);
            when(credentialStore.mask("sk-abc")).thenReturn("****abc");

            Optional<ModelInfo> result = registry.getById(modelUuid.toString());

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Custom GPT");
            assertThat(result.get().getSource()).isEqualTo(ModelInfo.Source.CUSTOM);
            assertThat(result.get().getApiKeyMasked()).isEqualTo("****abc");
        }

        @Test
        void returnsEmptyForNullId() {
            assertThat(registry.getById(null)).isEmpty();
        }

        @Test
        void returnsEmptyForBlankId() {
            assertThat(registry.getById("")).isEmpty();
            assertThat(registry.getById("   ")).isEmpty();
        }

        @Test
        void returnsEmptyForNonexistentId() {
            when(builtinModelMapper.selectById("nonexistent")).thenReturn(null);

            Optional<ModelInfo> result = registry.getById("nonexistent");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DeleteCustomModel {

        @Test
        void throwsNotFoundWhenModelIsNull() {
            UUID modelId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(customModelMapper.selectById(modelId)).thenReturn(null);

            assertThatThrownBy(() -> registry.deleteCustomModel(modelId, userId))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }

        @Test
        void throwsPermissionDeniedWhenUserIdDoesNotMatch() {
            UUID modelId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            CustomModelEntity entity = customEntity(modelId, ownerId, "Owner's Model",
                    "https://api.example.com", "sk-key");

            when(customModelMapper.selectById(modelId)).thenReturn(entity);

            assertThatThrownBy(() -> registry.deleteCustomModel(modelId, otherUserId))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_PERMISSION_DENIED);
        }
    }

    @Nested
    class GetDefaultModelId {

        @Test
        void returnsDefaultModel() {
            BuiltinModelEntity defaultModel = builtinEntity("gpt-4o", "GPT-4o", "openai", true, 1);
            when(builtinModelMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(defaultModel));

            String id = registry.getDefaultModelId();
            assertThat(id).isEqualTo("gpt-4o");
        }

        @Test
        void fallbackToFirstEnabledWhenNoDefault() {
            BuiltinModelEntity notDefault = builtinEntity("gpt-4o-mini", "GPT-4o Mini", "openai", false, 2);
            when(builtinModelMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(), List.of(notDefault));

            String id = registry.getDefaultModelId();
            assertThat(id).isEqualTo("gpt-4o-mini");
        }

        @Test
        void throwsInternalErrorWhenNoEnabledModels() {
            when(builtinModelMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(), List.of());

            assertThatThrownBy(() -> registry.getDefaultModelId())
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Nested
    class BuildChatClient {

        @Test
        void returnsPlaceholderMap() {
            ModelInfo model = new ModelInfo();
            model.setId("gpt-4o");
            model.setName("GPT-4o");
            model.setProvider("openai");
            model.setSource(ModelInfo.Source.BUILTIN);
            model.setApiUrl("https://api.openai.com");

            Map<String, Object> client = registry.buildChatClient(model);
            assertThat(client).containsEntry("model_id", "gpt-4o");
            assertThat(client).containsEntry("placeholder", true);
        }

        @Test
        void handlesNullProvider() {
            ModelInfo model = new ModelInfo();
            model.setId("test");
            model.setName("Test");
            model.setSource(ModelInfo.Source.CUSTOM);

            Map<String, Object> client = registry.buildChatClient(model);
            assertThat(client.get("provider")).isEqualTo("unknown");
        }
    }

    // ─── entity helpers ───

    private BuiltinModelEntity builtinEntity(String id, String name, String provider,
                                              boolean isDefault, int sortOrder) {
        BuiltinModelEntity e = new BuiltinModelEntity();
        e.setId(id);
        e.setName(name);
        e.setProvider(provider);
        e.setDescription(name + " description");
        e.setIsDefault(isDefault);
        e.setEnabled(true);
        e.setSortOrder(sortOrder);
        e.setConfig("{\"temperature\":0.7}");
        return e;
    }

    private CustomModelEntity customEntity(UUID id, UUID ownerId, String name,
                                            String apiUrl, String apiKey) {
        CustomModelEntity e = new CustomModelEntity();
        e.setId(id);
        e.setOwnerId(ownerId);
        e.setName(name);
        e.setApiUrl(apiUrl);
        e.setApiKeyEnc(apiKey.getBytes());
        e.setConnectionStatus("ok");
        return e;
    }

    private CustomModelEntity customEntity(UUID ownerId, String name, String apiUrl, String apiKey) {
        return customEntity(UUID.randomUUID(), ownerId, name, apiUrl, apiKey);
    }
}
