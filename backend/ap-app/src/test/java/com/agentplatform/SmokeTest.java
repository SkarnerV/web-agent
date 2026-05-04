package com.agentplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 1.3 + 1.6 冒烟集成测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>{@code /api/v1/health} 在 Virtual Thread 上执行</li>
 *   <li>{@code /api/v1/health/me} 通过 {@code @CurrentUser} 注入测试用户</li>
 *   <li>{@code /api/v1/health/sse} 推送 3 个 SSE 事件后 complete</li>
 *   <li>响应头携带 {@code X-Request-Id}</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SmokeTest {

    @Autowired
    private TestRestTemplate rest;

    @LocalServerPort
    private int port;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void healthRunsOnVirtualThread() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(url("/api/v1/health"), String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getHeaders().getFirst("X-Request-Id")).isNotBlank();

        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode data = root.get("data");
        assertThat(data.get("status").asText()).isEqualTo("ok");
        assertThat(data.get("virtual").asBoolean())
                .as("health endpoint must execute on a virtual thread")
                .isTrue();
        assertThat(data.get("thread").asText()).contains("VirtualThread");
    }

    @Test
    void currentUserResolverInjectsStubUser() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(url("/api/v1/health/me"), String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode user = mapper.readTree(resp.getBody()).get("data");
        assertThat(user.get("id").asText()).isEqualTo("a0000000-0000-0000-0000-000000000001");
        assertThat(user.get("name").asText()).isEqualTo("测试用户");
    }

    @Test
    void sseEndpointEmitsThreeEventsThenCompletes() {
        URI uri = URI.create(url("/api/v1/health/sse"));
        RequestEntity<Void> req = RequestEntity.get(uri).accept(MediaType.TEXT_EVENT_STREAM).build();
        ResponseEntity<String> resp = rest.exchange(req, String.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        HttpHeaders h = resp.getHeaders();
        assertThat(h.getContentType()).isNotNull();
        assertThat(h.getContentType().includes(MediaType.TEXT_EVENT_STREAM)).isTrue();

        String body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("id:evt_00001", "id:evt_00002", "id:evt_00003");
    }

    @Test
    void unknownPathReturnsErrorBodyInUnifiedFormat() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(url("/api/v1/does-not-exist"), String.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        JsonNode root = mapper.readTree(resp.getBody());
        assertThat(root.has("error")).isTrue();
        assertThat(root.get("error").get("code").asText()).isEqualTo("NOT_FOUND");
        assertThat(root.get("error").get("requestId").asText()).isNotBlank();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
