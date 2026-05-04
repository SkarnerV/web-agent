package com.agentplatform.asset.client;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public McpClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Test connection to an MCP server by sending an initialize request.
     */
    public Map<String, Object> testConnection(String url, String protocol, String authHeaders) {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "agent-platform", "version", "0.1.0")
                )
        );

        Map<String, Object> response = sendJsonRpc(url, protocol, authHeaders, request);
        log.debug("MCP initialize response: {}", response);
        return Map.of("status", "ok");
    }

    /**
     * Discover tools from an MCP server by sending a tools/list request.
     */
    public List<Map<String, Object>> discoverTools(String url, String protocol, String authHeaders) {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", 2,
                "method", "tools/list",
                "params", Map.of()
        );

        Map<String, Object> response = sendJsonRpc(url, protocol, authHeaders, request);
        log.debug("MCP tools/list response: {}", response);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        if (result == null) {
            throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                    Map.of("reason", "MCP server returned no result field"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        return tools != null ? tools : List.of();
    }

    private Map<String, Object> sendJsonRpc(String url, String protocol,
                                             String authHeaders, Map<String, Object> request) {
        try {
            String body = objectMapper.writeValueAsString(request);

            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (authHeaders != null && !authHeaders.isBlank()) {
                for (String line : authHeaders.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    int colonIdx = trimmed.indexOf(':');
                    if (colonIdx > 0) {
                        String headerName = trimmed.substring(0, colonIdx).trim();
                        String headerValue = trimmed.substring(colonIdx + 1).trim();
                        spec.header(headerName, headerValue);
                    }
                }
            }

            String responseBody = spec.body(body).retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                        Map.of("reason", "Empty response from MCP server"));
            }

            return objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.MCP_CONNECTION_FAILED,
                    Map.of("reason", "MCP JSON-RPC call failed: " + e.getMessage()));
        }
    }
}
