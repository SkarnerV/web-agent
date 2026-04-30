-- V5: Marketplace — Market Items, Favorites, Reviews
-- =====================================================

CREATE TABLE market_items (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_type         VARCHAR(20) NOT NULL CHECK (asset_type IN ('agent', 'skill', 'mcp')),
    asset_id           UUID NOT NULL,
    current_version_id UUID REFERENCES asset_versions(id),
    author_id          UUID NOT NULL REFERENCES users(id),
    status             VARCHAR(20) NOT NULL DEFAULT 'listed' CHECK (status IN ('listed', 'unlisted')),
    visibility         VARCHAR(20) NOT NULL DEFAULT 'public' CHECK (visibility IN ('public', 'group_edit', 'group_read', 'private')),
    category           VARCHAR(50),
    tags               JSONB,
    use_count          BIGINT NOT NULL DEFAULT 0,
    favorite_count     BIGINT NOT NULL DEFAULT 0,
    avg_rating         DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    review_count       INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE favorites (
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    market_item_id UUID NOT NULL REFERENCES market_items(id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, market_item_id)
);

CREATE TABLE reviews (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    market_item_id UUID NOT NULL REFERENCES market_items(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id),
    rating         SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
