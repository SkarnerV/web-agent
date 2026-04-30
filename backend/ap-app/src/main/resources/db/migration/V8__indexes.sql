-- V8: Indexes for all tables
-- ============================

-- agents
CREATE INDEX idx_agents_owner_status ON agents (owner_id, status, deleted_at);
CREATE INDEX idx_agents_visibility_status ON agents (visibility, status, deleted_at);
CREATE INDEX idx_agents_fulltext ON agents USING GIN (to_tsvector('simple'::regconfig, coalesce(name, '') || ' ' || coalesce(description, '')));

-- skills
CREATE INDEX idx_skills_owner_status ON skills (owner_id, status, deleted_at);
CREATE INDEX idx_skills_visibility_status ON skills (visibility, status, deleted_at);
CREATE INDEX idx_skills_fulltext ON skills USING GIN (to_tsvector('simple'::regconfig, coalesce(name, '') || ' ' || coalesce(description, '')));

-- mcps
CREATE INDEX idx_mcps_owner ON mcps (owner_id, deleted_at);
CREATE INDEX idx_mcps_fulltext ON mcps USING GIN (to_tsvector('simple'::regconfig, coalesce(name, '') || ' ' || coalesce(description, '')));

-- chat_sessions / chat_messages
CREATE INDEX idx_chat_sessions_user ON chat_sessions (user_id, updated_at DESC);
CREATE INDEX idx_chat_messages_session ON chat_messages (session_id, created_at);

-- market_items
CREATE INDEX idx_market_items_type_status ON market_items (asset_type, status, visibility);
CREATE INDEX idx_market_items_tags ON market_items USING GIN (tags);
CREATE INDEX idx_market_items_fulltext ON market_items USING GIN (to_tsvector('simple'::regconfig, coalesce(category, '') || ' ' || coalesce(tags::text, '')));

-- files
CREATE INDEX idx_files_expiry ON files (expires_at) WHERE status = 'active';

-- file_download_tokens (token UNIQUE already created inline)
CREATE INDEX idx_fdt_expiry ON file_download_tokens (expires_at) WHERE used = FALSE;

-- audit_logs (table not created in MVP — placeholder)
-- CREATE INDEX idx_audit_user ON audit_logs (user_id, created_at);
-- CREATE INDEX idx_audit_resource ON audit_logs (resource_type, resource_id);

-- kb_documents
CREATE INDEX idx_kb_docs_kb_status ON kb_documents (knowledge_base_id, index_status);

-- kb_chunks
CREATE INDEX idx_kb_chunks_embedding ON kb_chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_kb_chunks_kb ON kb_chunks (knowledge_base_id);
CREATE INDEX idx_kb_chunks_doc ON kb_chunks (document_id);

-- asset_references
CREATE INDEX idx_asset_refs_referrer ON asset_references (referrer_type, referrer_id);
CREATE INDEX idx_asset_refs_referee ON asset_references (referee_type, referee_id);

-- agent_tool_bindings
CREATE INDEX idx_atb_agent ON agent_tool_bindings (agent_id);
CREATE INDEX idx_atb_source ON agent_tool_bindings (source_type, source_id);

-- Partial unique indexes for agent_tool_bindings (source_id can be NULL for builtin)
CREATE UNIQUE INDEX uq_atb_builtin ON agent_tool_bindings (agent_id, tool_name) WHERE source_type = 'builtin';
CREATE UNIQUE INDEX uq_atb_mcp ON agent_tool_bindings (agent_id, source_id, tool_name) WHERE source_type = 'mcp';
CREATE UNIQUE INDEX uq_atb_knowledge ON agent_tool_bindings (agent_id, source_id, tool_name) WHERE source_type = 'knowledge';

-- org_memberships
CREATE INDEX idx_org_members_user ON org_memberships (user_id);
CREATE INDEX idx_org_members_org ON org_memberships (org_id);
