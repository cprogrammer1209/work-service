-- M1 Step 4: projects, their members, and what happened to them.
CREATE TABLE project (
    id                 UUID PRIMARY KEY,
    portal_id          UUID         NOT NULL,
    project_key        VARCHAR(10)  NOT NULL,
    name               VARCHAR(100) NOT NULL,
    description        TEXT         NOT NULL DEFAULT '',
    owner_user_id      UUID         NOT NULL,
    status             VARCHAR(40)  NOT NULL,
    start_date         DATE,
    end_date           DATE,
    visibility         VARCHAR(10)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    deleted_at         TIMESTAMPTZ,
    deleted_by_user_id UUID,
    -- PRJ-BR-01: unique per portal, and a trashed project keeps its key so it can be restored.
    CONSTRAINT uq_project_portal_key UNIQUE (portal_id, project_key),
    -- PRJ-BR-02
    CONSTRAINT ck_project_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX ix_project_portal ON project (portal_id) WHERE deleted_at IS NULL;

CREATE TABLE project_member (
    portal_id  UUID        NOT NULL,
    project_id UUID        NOT NULL REFERENCES project (id),
    user_id    UUID        NOT NULL,
    role       VARCHAR(10) NOT NULL,
    added_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (project_id, user_id)
);
CREATE INDEX ix_project_member_user ON project_member (portal_id, user_id);

CREATE TABLE project_activity (
    id             UUID PRIMARY KEY,
    portal_id      UUID         NOT NULL,
    project_id     UUID         NOT NULL REFERENCES project (id),
    action         VARCHAR(40)  NOT NULL,
    actor_user_id  UUID         NOT NULL,
    actor_name     VARCHAR(200),
    target_user_id UUID,
    detail         VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_project_activity_project ON project_activity (project_id, created_at DESC);
