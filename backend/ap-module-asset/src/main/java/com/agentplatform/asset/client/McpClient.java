package com.agentplatform.asset.client;

import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.enums.ConnectionStatus;
import com.agentplatform.common.core.enums.McpProtocol;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.security.CredentialStore;
import com.agentplatform.common.core.tool.McpToolInvoker;
import com.agentplatform.common.core.tool.ToolResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpClient implements McpToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 2_000;
    private static final long TOOL_TIMEOUT_MS = 30_000;
    private static final int CONSECUTIVE_FAILURE_THRESHOLD = 3;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final McpMapper mcpMapper;
    private final CredentialStore credentialStore;
    private final ConcurrentHashMap<UUID, Integer> failureCounts = new ConcurrentHashMap<>();

    public McpClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                     McpMapper mcpMapper, CredentialStore credentialStore) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.mcpMapper = mcpMapper;
        this.credentialStore = credentialStore;
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

    /**
     * Execute a tool on a remote MCP server with retry and failure tracking.
     */
    @Override
    public ToolResult executeTool(UUID mcpId, String toolName, String arguments) {
        long startTime = System.currentTimeMillis();

        McpEntity mcp = mcpMapper.selectById(mcpId);
        if (mcp == null) {
            return ToolResult.error("mcp_" + mcpId, "MCP not found: " + mcpId,
                    System.currentTimeMillis() - startTime);
        }
        if (!Boolean.TRUE.equals(mcp.getEnabled())) {
            return ToolResult.error("mcp_" + mcpId, "MCP is disabled: " + mcpId,
                    System.currentTimeMillis() - startTime);
        }

        String authHeaders = decryptAuth(mcp);
        String protocol = mcp.getProtocol();

        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String resultContent = callTool(mcp.getUrl(), protocol, authHeaders,
                        toolName, arguments);
                long duration = System.currentTimeMillis() - startTime;
                resetFailureCount(mcpId);
                return ToolResult.success("mcp_" + mcpId, resultContent, duration);
            } catch (Exception e) {
                lastException = e;
                log.warn("MCP tool call failed (attempt {}/{}): tool={}, error={}",
                        attempt + 1, MAX_RETRIES + 1, toolName, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        int failures = recordFailure(mcpId);
        String errMsg = lastException != null ? lastException.getMessage() : "unknown";
        return ToolResult.error("mcp_" + mcpId, errMsg, duration);
    }

    // ─── helpers ───

    /**
     * Sends a tools/call JSON-RPC request to the MCP server.
     */
    private String callTool(String url, String protocol, String authHeaders,
                            String toolName, String arguments) {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().hashCode() & Integer.MAX_VALUE,
                "method", "tools/call",
                "params", Map.of(
                        "name", toolName,
                        "arguments", parseArguments(arguments)
                )
        );

        Map<String, Object> response = sendJsonRpc(url, protocol, authHeaders, request);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        if (result == null) {
            Object err = response.get("error");
            throw new BizException(ErrorCode.MCP_TOOL_CALL_FAILED,
                    Map.of("reason", err != null ? err.toString() : "No result from MCP server"));
        }

        // Return the content array or text from the result
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        if (content != null) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> item : content) {
                Object text = item.get("text");
                if (text != null) sb.append(text);
            }
            return sb.toString();
        }
        return result.toString();
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

    private String decryptAuth(McpEntity mcp) {
        if (mcp.getAuthHeadersEnc() != null && mcp.getAuthHeadersEnc().length > 0) {
            return credentialStore.decrypt(new String(mcp.getAuthHeadersEnc()));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(arguments, Map.class);
        } catch (Exception e) {
            return Map.of("raw", arguments);
        }
    }

    private int recordFailure(UUID mcpId) {
        int count = failureCounts.merge(mcpId, 1, Integer::sum);
        if (count >= CONSECUTIVE_FAILURE_THRESHOLD) {
            log.warn("MCP {} reached {} consecutive failures, marking as error", mcpId, count);
            McpEntity mcp = mcpMapper.selectById(mcpId);
            if (mcp != null) {
                mcp.setConnectionStatus(ConnectionStatus.ERROR.getValue());
                mcpMapper.updateById(mcp);
            }
            failureCounts.remove(mcpId);
        }
        return count;
    }

    private void resetFailureCount(UUID mcpId) {
        failureCounts.remove(mcpId);
    }
}
