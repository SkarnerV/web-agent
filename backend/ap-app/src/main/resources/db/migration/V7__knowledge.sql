-- V7: Knowledge Base Documents and Chunks (pgvector)
-- =====================================================

CREATE TABLE kb_documents (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    file_id           UUID NOT NULL REFERENCES files(id),
    filename          VARCHAR(255) NOT NULL,
    file_size         BIGINT NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    scan_status       VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (scan_status IN ('pending', 'clean', 'infected', 'error')),
    index_status      VARCHAR(20) NOT NULL DEFAULT 'pending_scan' CHECK (index_status IN ('pending_scan', 'pending', 'indexing', 'indexed', 'failed')),
    index_error       TEXT,
    chunk_count       INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE kb_chunks (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id       UUID NOT NULL REFERENCES kb_documents(id) ON DELETE CASCADE,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    chunk_index       INTEGER NOT NULL,
    content           TEXT NOT NULL,
    embedding         vector(1536),
    metadata          JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
