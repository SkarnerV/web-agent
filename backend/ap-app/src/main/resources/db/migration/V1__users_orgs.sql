-- V1: Users, Organizations, and Org Memberships
-- ==================================================

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    w3_id       VARCHAR(64) UNIQUE,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    avatar_url  TEXT,
    role        VARCHAR(10) NOT NULL DEFAULT 'user' CHECK (role IN ('admin', 'user')),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    w3_org_id   VARCHAR(64) UNIQUE NOT NULL,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE org_memberships (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    org_id      UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role_in_org VARCHAR(50),
    synced_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, org_id)
);

-- Seed: MVP stub test user
INSERT INTO users (id, w3_id, name, email, role, is_active)
VALUES ('a0000000-0000-0000-0000-000000000001', 'stub-w3-user', 'MVP Test User', 'test@agentplatform.local', 'admin', TRUE);

-- Seed: test organization
INSERT INTO organizations (id, w3_org_id, name)
VALUES ('b0000000-0000-0000-0000-000000000001', 'stub-w3-org', 'Default Organization');

INSERT INTO org_memberships (user_id, org_id, role_in_org)
VALUES ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'admin');
