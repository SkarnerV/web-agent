-- V6: Builtin Models and Custom Models
-- =======================================

CREATE TABLE builtin_models (
    id          VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    provider    VARCHAR(50) NOT NULL,
    description TEXT,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    config      JSONB,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

INSERT INTO builtin_models (id, name, provider, description, is_default, config, enabled, sort_order)
VALUES
    ('gpt-4o',           'GPT-4o',            'openai',    'OpenAI GPT-4o model',                       TRUE,  '{"api_url": "https://api.openai.com/v1", "max_tokens": 4096}', TRUE, 1),
    ('gpt-4o-mini',      'GPT-4o Mini',       'openai',    'OpenAI GPT-4o Mini - faster and cheaper',   FALSE, '{"api_url": "https://api.openai.com/v1", "max_tokens": 4096}', TRUE, 2),
    ('claude-3-5-sonnet', 'Claude 3.5 Sonnet', 'anthropic', 'Anthropic Claude 3.5 Sonnet',              FALSE, '{"api_url": "https://api.anthropic.com/v1", "max_tokens": 4096}', TRUE, 3);

CREATE TABLE custom_models (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id          UUID NOT NULL REFERENCES users(id),
    name              VARCHAR(100) NOT NULL,
    api_url           TEXT NOT NULL,
    api_key_enc       BYTEA,
    connection_status VARCHAR(20) NOT NULL DEFAULT 'connected' CHECK (connection_status IN ('connected', 'failed')),
    last_error        TEXT,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
