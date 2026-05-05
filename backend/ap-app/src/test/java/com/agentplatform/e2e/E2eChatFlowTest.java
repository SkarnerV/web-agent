package com.agentplatform.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("migration-test")
class E2eChatFlowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("agent_platform_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-test.sql");

    @Autowired
    private TestRestTemplate rest;

    @LocalServerPort
    private int port;

    private final ObjectMapper mapper = new ObjectMapper();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ───────── 13.1 Core Chat Flow ─────────

    @Test
    @DisplayName("Full chat flow: agent → session → send message (SSE) → history")
    void fullChatFlow() throws Exception {
        UUID userId = UUID.fromString("a0000000-0000-0000-0000-000000000001");

        // Step 1: Create an agent
        Map<String, Object> agentBody = new LinkedHashMap<>();
        agentBody.put("name", "E2E Test Agent " + UUID.randomUUID().toString().substring(0, 6));
        agentBody.put("description", "Agent for E2E chat flow testing");
        agentBody.put("systemPrompt", "You are a helpful assistant.");
        agentBody.put("maxSteps", 5);

        ResponseEntity<String> createResp = rest.postForEntity(
                url("/api/v1/agents"), agentBody, String.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode agent = mapper.readTree(createResp.getBody()).get("data");
        String agentId = agent.get("id").asText();
        assertThat(agentId).isNotBlank();

        // Step 2: Create a chat session for this agent
        Map<String, Object> sessionBody = Map.of("agentId", agentId);
        ResponseEntity<String> sessionResp = rest.postForEntity(
                url("/api/v1/chat/sessions"), sessionBody, String.class);
        assertThat(sessionResp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode session = mapper.readTree(sessionResp.getBody()).get("data");
        String sessionId = session.get("id").asText();
        assertThat(sessionId).isNotBlank();

        // Step 3: Send a message via SSE
        Map<String, Object> msgBody = Map.of("content", "Hello, what can you do?");
        URI sseUri = URI.create(url("/api/v1/chat/sessions/" + sessionId + "/messages"));
        RequestEntity<Map<String, Object>> sseReq = RequestEntity
                .post(sseUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(msgBody);
        ResponseEntity<String> sseResp = rest.exchange(sseReq, String.class);

        assertThat(sseResp.getStatusCode().is2xxSuccessful()).isTrue();
        HttpHeaders sseHeaders = sseResp.getHeaders();
        assertThat(sseHeaders.getContentType()).isNotNull();
        assertThat(sseHeaders.getContentType().includes(MediaType.TEXT_EVENT_STREAM)).isTrue();

        String sseBody = sseResp.getBody();
        assertThat(sseBody).isNotNull();
        assertThat(sseBody).contains("message_start");
        assertThat(sseBody).contains("token");
        assertThat(sseBody).contains("message_end");

        // Step 4: Query session history
        ResponseEntity<String> historyResp = rest.getForEntity(
                url("/api/v1/chat/sessions/" + sessionId), String.class);
        assertThat(historyResp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode history = mapper.readTree(historyResp.getBody()).get("data");
        assertThat(history.get("id").asText()).isEqualTo(sessionId);

        JsonNode messages = history.get("messages");
        assertThat(messages.isArray()).isTrue();
        assertThat(messages.size()).isGreaterThanOrEqualTo(2);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
