package com.agentplatform.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("migration-test")
class IndexVerificationTest {

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

    @ParameterizedTest(name = "Index {0} exists on table {1}")
    @CsvSource({
            "idx_agents_owner_status, agents",
            "idx_agents_visibility_status, agents",
            "idx_agents_fulltext, agents",
            "idx_skills_owner_status, skills",
            "idx_skills_visibility_status, skills",
            "idx_skills_fulltext, skills",
            "idx_mcps_owner, mcps",
            "idx_mcps_fulltext, mcps",
            "idx_chat_sessions_user, chat_sessions",
            "idx_chat_messages_session, chat_messages",
            "idx_market_items_type_status, market_items",
            "idx_market_items_tags, market_items",
            "idx_market_items_fulltext, market_items",
            "idx_files_expiry, files",
            "idx_fdt_expiry, file_download_tokens",
            "idx_kb_docs_kb_status, kb_documents",
            "idx_kb_chunks_embedding, kb_chunks",
            "idx_kb_chunks_kb, kb_chunks",
            "idx_kb_chunks_doc, kb_chunks",
            "idx_asset_refs_referrer, asset_references",
            "idx_asset_refs_referee, asset_references",
            "idx_atb_agent, agent_tool_bindings",
            "idx_atb_source, agent_tool_bindings",
            "idx_org_members_user, org_memberships",
            "idx_org_members_org, org_memberships"
    })
    void indexExists(String indexName, String tableName) {
        List<String> found = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = ? AND indexname = ?",
                String.class, tableName, indexName);
        assertThat(found).as("Index %s on table %s should exist", indexName, tableName).hasSize(1);
    }

    @Test
    void agentsGinFullTextIndexExists() {
        List<String> found = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'agents' AND indexdef LIKE '%gin%'",
                String.class);
        assertThat(found).isNotEmpty();
    }

    @Test
    void kbChunksHnswIndexExists() {
        List<String> found = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'kb_chunks' AND indexdef LIKE '%hnsw%'",
                String.class);
        assertThat(found).isNotEmpty();
    }

    @Test
    void agentToolBindingsPartialUniqueIndexes() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'agent_tool_bindings' AND indexname LIKE 'uq_atb_%'",
                String.class);
        assertThat(indexes).containsExactlyInAnyOrder("uq_atb_builtin", "uq_atb_mcp", "uq_atb_knowledge");
    }

    @Test
    void assetVersionsUniqueConstraintExists() {
        List<String> found = jdbc.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE table_name = 'asset_versions' AND constraint_type = 'UNIQUE'",
                String.class);
        assertThat(found).isNotEmpty();
    }
}
