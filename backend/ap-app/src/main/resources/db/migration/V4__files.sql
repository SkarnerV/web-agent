-- V4: Files and File Download Tokens
-- =====================================

CREATE TABLE files (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id                UUID NOT NULL REFERENCES users(id),
    session_id              UUID,
    source                  VARCHAR(20) NOT NULL CHECK (source IN ('chat_upload', 'chat_generated', 'knowledge', 'asset')),
    generated_by_message_id UUID,
    filename                VARCHAR(255) NOT NULL,
    file_size               BIGINT NOT NULL,
    mime_type               VARCHAR(100) NOT NULL,
    storage_path            TEXT NOT NULL,
    storage_type            VARCHAR(20) NOT NULL DEFAULT 'minio' CHECK (storage_type IN ('minio')),
    scan_status             VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (scan_status IN ('pending', 'clean', 'infected', 'error')),
    scan_error              TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'expired', 'deleted')),
    expires_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE file_download_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id     UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    session_id  UUID,
    token       VARCHAR(128) UNIQUE NOT NULL,
    token_type  VARCHAR(20) NOT NULL CHECK (token_type IN ('download', 'preview')),
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    user_agent  VARCHAR(512),
    ip_address  INET,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
