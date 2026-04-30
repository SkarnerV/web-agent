-- V2: Asset tables — Agents, Skills, MCPs, Knowledge Bases, Versions, References, Tool Bindings
-- ================================================================================================

CREATE TABLE agents (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id                UUID REFERENCES users(id),
    name                    VARCHAR(30) NOT NULL,
    description             VARCHAR(200),
    avatar                  TEXT,
    system_prompt           TEXT,
    max_steps               INTEGER NOT NULL DEFAULT 10 CHECK (max_steps BETWEEN 1 AND 50),
    model_id                VARCHAR(64),
    status                  VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'published', 'archived')),
    visibility              VARCHAR(20) NOT NULL DEFAULT 'private' CHECK (visibility IN ('public', 'group_edit', 'group_read', 'private')),
    current_version         VARCHAR(20),
    has_unpublished_changes BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMPTZ,
    created_by              UUID,
    updated_by              UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE skills (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id                UUID REFERENCES users(id),
    name                    VARCHAR(100) NOT NULL,
    description             TEXT,
    trigger_conditions      JSONB,
    format                  VARCHAR(20) NOT NULL CHECK (format IN ('yaml', 'markdown')),
    content                 TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'published', 'archived')),
    visibility              VARCHAR(20) NOT NULL DEFAULT 'private' CHECK (visibility IN ('public', 'group_edit', 'group_read', 'private')),
    current_version         VARCHAR(20),
    has_unpublished_changes BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMPTZ,
    created_by              UUID,
    updated_by              UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE mcps (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id                UUID REFERENCES users(id),
    name                    VARCHAR(100) NOT NULL,
    description             TEXT,
    url                     TEXT,
    protocol                VARCHAR(20) NOT NULL CHECK (protocol IN ('sse', 'streamable_http')),
    auth_headers_enc        BYTEA,
    json_config             TEXT,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    connection_status       VARCHAR(20) NOT NULL DEFAULT 'offline' CHECK (connection_status IN ('online', 'offline', 'error')),
    last_error              TEXT,
    tools_discovered        JSONB,
    status                  VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'published', 'archived')),
    visibility              VARCHAR(20) NOT NULL DEFAULT 'private' CHECK (visibility IN ('public', 'group_edit', 'group_read', 'private')),
    current_version         VARCHAR(20),
    has_unpublished_changes BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMPTZ,
    created_by              UUID,
    updated_by              UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_bases (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id         UUID REFERENCES users(id),
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    index_config     JSONB,
    visibility       VARCHAR(20) NOT NULL DEFAULT 'private' CHECK (visibility IN ('group_edit', 'group_read', 'private')),
    doc_count        INTEGER NOT NULL DEFAULT 0,
    total_size_bytes BIGINT NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,
    deleted_at       TIMESTAMPTZ,
    created_by       UUID,
    updated_by       UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE asset_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_type      VARCHAR(20) NOT NULL CHECK (asset_type IN ('agent', 'skill', 'mcp')),
    asset_id        UUID NOT NULL,
    version         VARCHAR(20) NOT NULL,
    config_snapshot JSONB NOT NULL,
    release_notes   TEXT,
    published_by    UUID REFERENCES users(id),
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (asset_type, asset_id, version)
);

CREATE TABLE asset_references (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    referrer_type VARCHAR(20) NOT NULL DEFAULT 'agent' CHECK (referrer_type IN ('agent')),
    referrer_id   UUID NOT NULL,
    referee_type  VARCHAR(30) NOT NULL CHECK (referee_type IN ('agent', 'skill', 'mcp', 'knowledge_base')),
    referee_id    UUID NOT NULL,
    ref_kind      VARCHAR(20) NOT NULL CHECK (ref_kind IN ('skill', 'knowledge', 'collaborator')),
    config_params JSONB
);

CREATE TABLE agent_tool_bindings (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id             UUID NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    source_type          VARCHAR(20) NOT NULL CHECK (source_type IN ('builtin', 'mcp', 'knowledge')),
    source_id            UUID,
    tool_name            VARCHAR(128) NOT NULL,
    tool_schema_snapshot JSONB,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order           INTEGER NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
