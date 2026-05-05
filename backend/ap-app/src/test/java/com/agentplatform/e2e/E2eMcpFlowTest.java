package com.agentplatform.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
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
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("migration-test")
class E2eMcpFlowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("agent_platform_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-test.sql");

    static WireMockServer wireMock;

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

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        // JSON-RPC: initialize
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{}}}")));

        // JSON-RPC: tools/list
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/list")))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"tools\":[" +
                        "{\"name\":\"get_weather\",\"description\":\"Get weather for a city\"," +
                        "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}},\"required\":[\"city\"]}}" +
                        "]}}")));

        // JSON-RPC: tools/call
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Sunny, 25°C\"}]}}")));
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @Test
    @DisplayName("MCP flow: create → test connection → discover tools → bind to agent → chat")
    void fullMcpFlow() throws Exception {
        String mcpUrl = wireMock.baseUrl();

        // Step 1: Create an MCP pointing at WireMock
        Map<String, Object> mcpBody = new LinkedHashMap<>();
        mcpBody.put("name", "E2E Weather MCP");
        mcpBody.put("description", "WireMock MCP for E2E testing");
        mcpBody.put("url", mcpUrl);
        mcpBody.put("protocol", "streamable_http");

        ResponseEntity<String> createResp = rest.postForEntity(
                url("/api/v1/mcps"), mcpBody, String.class);
        assertThat(createResp.getStatusCode().value())
                .as("Create MCP: %s", createResp.getBody()).isEqualTo(201);
        JsonNode mcp = mapper.readTree(createResp.getBody()).get("data");
        String mcpId = mcp.get("id").asText();
        assertThat(mcpId).isNotBlank();
        assertThat(mcp.get("connectionStatus").asText()).isEqualTo("offline");

        // Step 2: Test connection
        ResponseEntity<String> testResp = rest.postForEntity(
                url("/api/v1/mcps/" + mcpId + "/test"), null, String.class);
        assertThat(testResp.getStatusCode().is2xxSuccessful())
                .as("Test connection: %s", testResp.getBody()).isTrue();
        JsonNode testResult = mapper.readTree(testResp.getBody()).get("data");
        assertThat(testResult.get("connectionStatus").asText()).isEqualTo("online");

        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize"))));

        // Step 3: Discover tools
        ResponseEntity<String> discoverResp = rest.postForEntity(
                url("/api/v1/mcps/" + mcpId + "/discover"), null, String.class);
        assertThat(discoverResp.getStatusCode().is2xxSuccessful())
                .as("Discover tools: %s", discoverResp.getBody()).isTrue();
        JsonNode discoverResult = mapper.readTree(discoverResp.getBody()).get("data");
        String toolsDiscovered = discoverResult.get("toolsDiscovered").asText();
        assertThat(toolsDiscovered).contains("get_weather");

        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/list"))));

        // Step 4: Create agent with MCP tool binding
        Map<String, Object> toolBinding = new LinkedHashMap<>();
        toolBinding.put("sourceType", "mcp");
        toolBinding.put("sourceId", mcpId);
        toolBinding.put("toolName", "get_weather");
        toolBinding.put("enabled", true);

        Map<String, Object> agentBody = new LinkedHashMap<>();
        agentBody.put("name", "MCP Test Agent");
        agentBody.put("description", "Agent with MCP tool binding");
        agentBody.put("systemPrompt", "You are a weather assistant.");
        agentBody.put("maxSteps", 5);
        agentBody.put("toolBindings", List.of(toolBinding));

        ResponseEntity<String> agentResp = rest.postForEntity(
                url("/api/v1/agents"), agentBody, String.class);
        assertThat(agentResp.getStatusCode().is2xxSuccessful())
                .as("Create agent: %s", agentResp.getBody()).isTrue();
        JsonNode agent = mapper.readTree(agentResp.getBody()).get("data");
        String agentId = agent.get("id").asText();
        assertThat(agentId).isNotBlank();

        // Step 5: Create chat session and send message
        // Note: The LLM stub (DefaultLlmStreamService) always returns text tokens,
        // so tool_call events won't appear in the SSE stream. This verifies the
        // agent+MCP wiring is correct; actual tool invocation via chat requires
        // real LLM integration (task 5.2).
        Map<String, Object> sessionBody = Map.of("agentId", agentId);
        ResponseEntity<String> sessionResp = rest.postForEntity(
                url("/api/v1/chat/sessions"), sessionBody, String.class);
        assertThat(sessionResp.getStatusCode().is2xxSuccessful())
                .as("Create session: %s", sessionResp.getBody()).isTrue();
        String sessionId = mapper.readTree(sessionResp.getBody()).get("data").get("id").asText();

        Map<String, Object> msgBody = Map.of("content", "What's the weather in Beijing?");
        URI sseUri = URI.create(url("/api/v1/chat/sessions/" + sessionId + "/messages"));
        RequestEntity<Map<String, Object>> sseReq = RequestEntity
                .post(sseUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(msgBody);
        ResponseEntity<String> sseResp = rest.exchange(sseReq, String.class);
        assertThat(sseResp.getStatusCode().is2xxSuccessful())
                .as("SSE message: %s", sseResp.getBody()).isTrue();
        assertThat(sseResp.getBody()).contains("message_start").contains("message_end");

        // Step 6: Toggle MCP off, verify detail shows disabled
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> toggleResp = rest.exchange(
                url("/api/v1/mcps/" + mcpId + "/toggle?enabled=false"),
                HttpMethod.PUT, new HttpEntity<>(headers), String.class);
        assertThat(toggleResp.getStatusCode().is2xxSuccessful())
                .as("Toggle MCP off: %s", toggleResp.getBody()).isTrue();
        JsonNode toggleResult = mapper.readTree(toggleResp.getBody()).get("data");
        assertThat(toggleResult.get("enabled").asBoolean()).isFalse();

        // Step 7: Verify MCP detail shows disabled status
        ResponseEntity<String> detailResp = rest.getForEntity(
                url("/api/v1/mcps/" + mcpId), String.class);
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode detail = mapper.readTree(detailResp.getBody()).get("data");
        assertThat(detail.get("enabled").asBoolean()).isFalse();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
