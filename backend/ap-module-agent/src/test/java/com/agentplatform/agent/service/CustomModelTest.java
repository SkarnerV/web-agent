package com.agentplatform.agent.service;

import com.agentplatform.agent.dto.CustomModelCreateRequest;
import com.agentplatform.agent.dto.CustomModelUpdateRequest;
import com.agentplatform.agent.dto.CustomModelVO;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomModelTest {

    @Mock private ModelRegistry modelRegistry;
    @Mock private CredentialStore credentialStore;
    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private ModelService modelService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODEL_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        modelService = new ModelService(modelRegistry, credentialStore, restClientBuilder);
    }

    @Nested
    @DisplayName("Create custom model")
    class CreateCustomModel {

        @Test
        @DisplayName("valid API URL + Key verifies connectivity and returns 201-worthy response")
        void validApiUrlAndKey() {
            CustomModelCreateRequest request = new CustomModelCreateRequest();
            request.setName("My GPT-4o");
            request.setApiUrl("https://api.openai.com/v1/models");
            request.setApiKey("sk-valid-key");

            // Connectivity check succeeds
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("https://api.openai.com/v1/models")).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn("{\"data\":[]}");

            when(credentialStore.encrypt("sk-valid-key")).thenReturn("encrypted-sk-valid-key");

            ModelInfo created = customModelInfo(MODEL_ID.toString(), "My GPT-4o",
                    "https://api.openai.com/v1/models", "***valid-key", "ok");
            when(modelRegistry.createCustomModel(
                    eq("My GPT-4o"), eq("https://api.openai.com/v1/models"),
                    any(byte[].class), eq("ok"), eq(USER_A)))
                    .thenReturn(created);

            CustomModelVO result = modelService.createCustomModel(request, USER_A);

            assertThat(result.getName()).isEqualTo("My GPT-4o");
            assertThat(result.getApiUrl()).isEqualTo("https://api.openai.com/v1/models");
            assertThat(result.getApiKeyMasked()).isEqualTo("***valid-key");
            assertThat(result.getConnectionStatus()).isEqualTo("ok");

            verify(credentialStore).encrypt("sk-valid-key");
        }

        @Test
        @DisplayName("invalid API URL returns 422 MODEL_CONNECTION_FAILED")
        void invalidApiUrlReturnsConnectionFailed() {
            CustomModelCreateRequest request = new CustomModelCreateRequest();
            request.setName("Bad Model");
            request.setApiUrl("https://invalid.example.com/api");
            request.setApiKey("sk-bad-key");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("https://invalid.example.com/api")).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> modelService.createCustomModel(request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MODEL_CONNECTION_FAILED);

            verify(modelRegistry, never()).createCustomModel(anyString(), anyString(),
                    any(byte[].class), anyString(), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Update custom model")
    class UpdateCustomModel {

        @Test
        @DisplayName("update with new API key re-verifies connectivity")
        void updateWithNewApiKeyReverifies() {
            CustomModelUpdateRequest request = new CustomModelUpdateRequest();
            request.setName("Updated Model");
            request.setApiUrl("https://api.new.com/v1/models");
            request.setApiKey("sk-new-key");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("https://api.new.com/v1/models")).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn("{\"data\":[]}");

            when(credentialStore.encrypt("sk-new-key")).thenReturn("encrypted-sk-new-key");

            ModelInfo updated = customModelInfo(MODEL_ID.toString(), "Updated Model",
                    "https://api.new.com/v1/models", "***new-key", "ok");
            when(modelRegistry.updateCustomModel(eq(MODEL_ID), eq("Updated Model"),
                    eq("https://api.new.com/v1/models"), any(byte[].class), eq("ok"), eq(USER_A)))
                    .thenReturn(updated);

            CustomModelVO result = modelService.updateCustomModel(MODEL_ID, request, USER_A);

            assertThat(result.getName()).isEqualTo("Updated Model");
            assertThat(result.getConnectionStatus()).isEqualTo("ok");
            verify(credentialStore).encrypt("sk-new-key");
        }

        @Test
        @DisplayName("update with new API key but no URL falls back to existing model URL")
        void updateWithNewKeyFallsBackToExistingUrl() {
            CustomModelUpdateRequest request = new CustomModelUpdateRequest();
            request.setApiKey("sk-changed-key");

            // Should fall back to the existing model's URL
            ModelInfo existing = customModelInfo(MODEL_ID.toString(), "My Model",
                    "https://api.existing.com/v1/models", "***old", "ok");
            when(modelRegistry.getById(MODEL_ID.toString()))
                    .thenReturn(java.util.Optional.of(existing));

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri("https://api.existing.com/v1/models")).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn("{\"data\":[]}");

            when(credentialStore.encrypt("sk-changed-key")).thenReturn("encrypted-changed");

            ModelInfo updated = customModelInfo(MODEL_ID.toString(), "My Model",
                    "https://api.existing.com/v1/models", "***changed", "ok");
            when(modelRegistry.updateCustomModel(eq(MODEL_ID), isNull(), isNull(),
                    any(byte[].class), eq("ok"), eq(USER_A))).thenReturn(updated);

            CustomModelVO result = modelService.updateCustomModel(MODEL_ID, request, USER_A);

            assertThat(result.getConnectionStatus()).isEqualTo("ok");
            verify(modelRegistry).getById(MODEL_ID.toString());
            verify(credentialStore).encrypt("sk-changed-key");
        }

        @Test
        @DisplayName("update with only name does not re-verify connectivity")
        void updateNameOnly() {
            CustomModelUpdateRequest request = new CustomModelUpdateRequest();
            request.setName("Renamed Model");

            ModelInfo updated = customModelInfo(MODEL_ID.toString(), "Renamed Model",
                    "https://api.old.com/v1/models", "***old-key", "ok");
            when(modelRegistry.updateCustomModel(eq(MODEL_ID), eq("Renamed Model"),
                    isNull(), isNull(), isNull(), eq(USER_A)))
                    .thenReturn(updated);

            CustomModelVO result = modelService.updateCustomModel(MODEL_ID, request, USER_A);

            assertThat(result.getName()).isEqualTo("Renamed Model");
            verify(restClient, never()).get();
        }
    }

    @Nested
    @DisplayName("Delete custom model")
    class DeleteCustomModel {

        @Test
        @DisplayName("deletes custom model and resets affected agents")
        void deletesAndResetsAgents() {
            modelService.deleteCustomModel(MODEL_ID, USER_A);

            verify(modelRegistry).deleteCustomModel(MODEL_ID, USER_A);
        }

        @Test
        @DisplayName("throws NOT_FOUND when model does not exist")
        void throwsNotFound() {
            doThrow(new BizException(ErrorCode.ASSET_NOT_FOUND, "Custom model not found"))
                    .when(modelRegistry).deleteCustomModel(MODEL_ID, USER_A);

            assertThatThrownBy(() -> modelService.deleteCustomModel(MODEL_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }

        @Test
        @DisplayName("throws PERMISSION_DENIED when user does not own model")
        void throwsPermissionDenied() {
            UUID otherUser = UUID.fromString("99999999-9999-9999-9999-999999999999");
            doThrow(new BizException(ErrorCode.ASSET_PERMISSION_DENIED))
                    .when(modelRegistry).deleteCustomModel(MODEL_ID, otherUser);

            assertThatThrownBy(() -> modelService.deleteCustomModel(MODEL_ID, otherUser))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_PERMISSION_DENIED);
        }
    }

    // ─── helper ───

    private ModelInfo customModelInfo(String id, String name, String apiUrl,
                                       String apiKeyMasked, String connectionStatus) {
        ModelInfo info = new ModelInfo();
        info.setId(id);
        info.setName(name);
        info.setApiUrl(apiUrl);
        info.setApiKeyMasked(apiKeyMasked);
        info.setConnectionStatus(connectionStatus);
        info.setProvider("custom");
        info.setSource(ModelInfo.Source.CUSTOM);
        info.setEnabled(true);
        return info;
    }
}
