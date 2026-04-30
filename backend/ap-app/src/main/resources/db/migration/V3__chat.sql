-- V3: Chat Sessions, Messages, and Session States
-- ==================================================

CREATE TABLE chat_sessions (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID NOT NULL REFERENCES users(id),
    current_agent_id UUID REFERENCES agents(id),
    title            VARCHAR(200),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id   UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system', 'separator')),
    content      TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'complete' CHECK (status IN ('complete', 'incomplete')),
    tool_calls   JSONB,
    tool_results JSONB,
    attachments  JSONB,
    agent_id     UUID,
    model_id     VARCHAR(64),
    step_count   INTEGER,
    usage        JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_session_states (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id       UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    context_snapshot JSONB NOT NULL,
    step_count       INTEGER NOT NULL DEFAULT 0,
    tool_cache       JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ NOT NULL
);
