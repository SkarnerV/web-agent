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
class E2eMarketFlowTest {

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

    @Test
    @DisplayName("Market flow: create agent → publish → search → import → chat with copy")
    void fullMarketFlow() throws Exception {
        // Step 1: Create an agent
        Map<String, Object> agentBody = new LinkedHashMap<>();
        agentBody.put("name", "Market Test Agent " + UUID.randomUUID().toString().substring(0, 6));
        agentBody.put("description", "Agent for market E2E testing");
        agentBody.put("systemPrompt", "You are a helpful assistant.");
        agentBody.put("maxSteps", 5);

        ResponseEntity<String> createResp = rest.postForEntity(
                url("/api/v1/agents"), agentBody, String.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful())
                .as("Create agent: %s", createResp.getBody()).isTrue();
        JsonNode agent = mapper.readTree(createResp.getBody()).get("data");
        String agentId = agent.get("id").asText();
        assertThat(agentId).isNotBlank();

        // Step 2: Publish agent to market
        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("assetType", "agent");
        publishBody.put("assetId", agentId);
        publishBody.put("visibility", "public");
        publishBody.put("version", "v1.0.0");
        publishBody.put("releaseNotes", "E2E test publish");

        ResponseEntity<String> publishResp = rest.postForEntity(
                url("/api/v1/market/publish"), publishBody, String.class);
        assertThat(publishResp.getStatusCode().value())
                .as("Publish agent: %s", publishResp.getBody()).isEqualTo(201);
        JsonNode marketItem = mapper.readTree(publishResp.getBody()).get("data");
        String marketItemId = marketItem.get("id").asText();
        assertThat(marketItemId).isNotBlank();

        // Step 3: Search market items and find the published agent
        ResponseEntity<String> listResp = rest.getForEntity(
                url("/api/v1/market/items?type=agent"), String.class);
        assertThat(listResp.getStatusCode().is2xxSuccessful())
                .as("List market items: %s", listResp.getBody()).isTrue();
        JsonNode pageResult = mapper.readTree(listResp.getBody()).get("data");
        JsonNode items = pageResult.get("data");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThanOrEqualTo(1);

        // Step 4: Import the published agent
        ResponseEntity<String> importResp = rest.postForEntity(
                url("/api/v1/market/items/" + marketItemId + "/import"),
                null, String.class);
        assertThat(importResp.getStatusCode().value())
                .as("Import agent: %s", importResp.getBody()).isEqualTo(201);
        JsonNode importData = mapper.readTree(importResp.getBody()).get("data");
        String importedAgentId = importData.get("asset_id").asText();
        assertThat(importedAgentId).isNotBlank();

        // Step 5: Create a chat session with the imported agent
        Map<String, Object> sessionBody = Map.of("agentId", importedAgentId);
        ResponseEntity<String> sessionResp = rest.postForEntity(
                url("/api/v1/chat/sessions"), sessionBody, String.class);
        assertThat(sessionResp.getStatusCode().is2xxSuccessful())
                .as("Create session: %s", sessionResp.getBody()).isTrue();
        JsonNode session = mapper.readTree(sessionResp.getBody()).get("data");
        String sessionId = session.get("id").asText();

        // Step 6: Send a message and verify SSE response
        Map<String, Object> msgBody = Map.of("content", "Hello from imported agent!");
        URI sseUri = URI.create(url("/api/v1/chat/sessions/" + sessionId + "/messages"));
        RequestEntity<Map<String, Object>> sseReq = RequestEntity
                .post(sseUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(msgBody);
        ResponseEntity<String> sseResp = rest.exchange(sseReq, String.class);

        assertThat(sseResp.getStatusCode().is2xxSuccessful())
                .as("SSE message: %s", sseResp.getBody()).isTrue();
        String sseBody = sseResp.getBody();
        assertThat(sseBody).contains("message_start");
        assertThat(sseBody).contains("message_end");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
