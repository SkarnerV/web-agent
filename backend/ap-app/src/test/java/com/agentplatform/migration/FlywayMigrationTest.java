package com.agentplatform.migration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("migration-test")
class FlywayMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("agent_platform_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-test.sql");

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void allMigrationsExecuteSuccessfully() {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void pgvectorExtensionInstalled() {
        List<Map<String, Object>> extensions = jdbc.queryForList(
                "SELECT extname FROM pg_extension WHERE extname = 'vector'");
        assertThat(extensions).isNotEmpty();
    }

    @Test
    void seedUserExists() {
        Map<String, Object> user = jdbc.queryForMap(
                "SELECT id, name, email, role FROM users WHERE id = 'a0000000-0000-0000-0000-000000000001'::uuid");
        assertThat(user.get("name")).isEqualTo("MVP Test User");
        assertThat(user.get("role")).isEqualTo("admin");
    }

    @Test
    void seedOrganizationExists() {
        Map<String, Object> org = jdbc.queryForMap(
                "SELECT id, name FROM organizations WHERE id = 'b0000000-0000-0000-0000-000000000001'::uuid");
        assertThat(org.get("name")).isEqualTo("Default Organization");
    }

    @Test
    void seedOrgMembershipExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM org_memberships WHERE user_id = 'a0000000-0000-0000-0000-000000000001'::uuid",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void allAssetTablesCreated() {
        List<String> expectedTables = List.of(
                "users", "organizations", "org_memberships",
                "agents", "skills", "mcps", "knowledge_bases",
                "asset_versions", "asset_references", "agent_tool_bindings",
                "chat_sessions", "chat_messages", "chat_session_states",
                "files", "file_download_tokens",
                "market_items", "favorites", "reviews",
                "builtin_models", "custom_models",
                "kb_documents", "kb_chunks");

        for (String table : expectedTables) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, table);
            assertThat(count).as("Table '%s' should exist", table).isEqualTo(1);
        }
    }

    @Test
    void agentToolBindingsPartialUniqueIndexesExist() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'agent_tool_bindings' AND indexname LIKE 'uq_atb_%'",
                String.class);
        assertThat(indexes).containsExactlyInAnyOrder("uq_atb_builtin", "uq_atb_mcp", "uq_atb_knowledge");
    }

    @Test
    void kbChunksEmbeddingColumnIsVector() {
        Map<String, Object> column = jdbc.queryForMap(
                "SELECT udt_name FROM information_schema.columns WHERE table_name = 'kb_chunks' AND column_name = 'embedding'");
        assertThat(column.get("udt_name")).isEqualTo("vector");
    }

    @Test
    void builtinModelsHaveExactlyOneDefault() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM builtin_models WHERE is_default = TRUE", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void builtinModelsSeedDataExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM builtin_models", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
