-- Work's own copy of who is in each portal and what they may do, fed by Platform's member events.
CREATE TABLE member_access (
    portal_id   UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    profile_key VARCHAR(40),
    permissions JSONB        NOT NULL DEFAULT '[]',
    active      BOOLEAN      NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (portal_id, user_id)
);

-- Event ids already applied. Delivery is at least once, so a repeated event is dropped by looking here.
CREATE TABLE processed_event (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

-- Work's own transactional outbox, same shape as Platform's.
CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    portal_id      UUID,
    aggregate_type VARCHAR(40)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(60)  NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ,
    version        SMALLINT     NOT NULL DEFAULT 1,
    actor_user_id  UUID,
    actor_name     VARCHAR(200)
);
CREATE INDEX ix_outbox_event_unpublished ON outbox_event (created_at, id) WHERE published_at IS NULL;
CREATE INDEX ix_outbox_event_portal ON outbox_event (portal_id);
