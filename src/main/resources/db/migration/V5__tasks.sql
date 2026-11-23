-- M1 Step 8 (tasks): statuses per task layout (owned by customfield), the project's task layout, task lists, tasks,
-- comments and the activity history. Every table carries portal_id.

-- A project's layout for its tasks; null means the portal's default TASK layout.
ALTER TABLE project ADD COLUMN task_layout_id UUID REFERENCES layout (id);

-- The statuses of a TASK layout. status_type decides Open or Closed behaviour; exactly one default (an OPEN one).
CREATE TABLE layout_status (
    id          UUID PRIMARY KEY,
    portal_id   UUID        NOT NULL,
    layout_id   UUID        NOT NULL REFERENCES layout (id) ON DELETE CASCADE,
    name        VARCHAR(30) NOT NULL,
    status_type VARCHAR(10) NOT NULL,
    color       VARCHAR(20),
    sort_order  INT         NOT NULL DEFAULT 0,
    is_default  BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX ux_layout_status_name ON layout_status (layout_id, lower(name));
CREATE UNIQUE INDEX ux_layout_status_default ON layout_status (layout_id) WHERE is_default;
CREATE INDEX ix_layout_status_portal ON layout_status (portal_id, layout_id, sort_order);

CREATE TABLE task_list (
    id         UUID PRIMARY KEY,
    portal_id  UUID         NOT NULL,
    project_id UUID         NOT NULL REFERENCES project (id),
    name       VARCHAR(100) NOT NULL,
    visibility VARCHAR(10)  NOT NULL DEFAULT 'INTERNAL',
    sort_order INT          NOT NULL DEFAULT 0,
    archived   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL,
    created_by UUID         NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX ux_task_list_name ON task_list (project_id, lower(name));
CREATE INDEX ix_task_list_project ON task_list (portal_id, project_id, sort_order);

-- The last task number handed out per project. Numbers are never reused, trashed tasks keep theirs.
CREATE TABLE task_sequence (
    project_id  UUID PRIMARY KEY REFERENCES project (id),
    portal_id   UUID NOT NULL,
    last_number INT  NOT NULL
);

CREATE TABLE task (
    id             UUID PRIMARY KEY,
    portal_id      UUID           NOT NULL,
    project_id     UUID           NOT NULL REFERENCES project (id),
    task_list_id   UUID           NOT NULL REFERENCES task_list (id),
    parent_task_id UUID REFERENCES task (id),
    depth          INT            NOT NULL DEFAULT 1,
    task_number    INT            NOT NULL,
    title          VARCHAR(200)   NOT NULL,
    description    TEXT,
    layout_id      UUID           NOT NULL REFERENCES layout (id),
    status_id      UUID           NOT NULL REFERENCES layout_status (id),
    priority       VARCHAR(10)    NOT NULL DEFAULT 'NONE',
    start_date     DATE,
    due_date       DATE,
    work_hours     NUMERIC(8, 2),
    completion     INT            NOT NULL DEFAULT 0,
    owner_ids      UUID[]         NOT NULL DEFAULT '{}',
    follower_ids   UUID[]         NOT NULL DEFAULT '{}',
    tags           TEXT[]         NOT NULL DEFAULT '{}',
    custom_values  JSONB          NOT NULL DEFAULT '{}',
    closed_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ    NOT NULL,
    created_by     UUID           NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    updated_by     UUID           NOT NULL,
    deleted_at     TIMESTAMPTZ,
    deleted_by     UUID,
    CONSTRAINT ux_task_number UNIQUE (project_id, task_number),
    CONSTRAINT ck_task_completion CHECK (completion BETWEEN 0 AND 100)
);
CREATE INDEX ix_task_project ON task (portal_id, project_id, deleted_at);
CREATE INDEX ix_task_parent ON task (portal_id, parent_task_id);
CREATE INDEX ix_task_layout ON task (portal_id, layout_id);
CREATE INDEX ix_task_status ON task (portal_id, status_id);
CREATE INDEX ix_task_custom_values ON task USING GIN (custom_values);

CREATE TABLE task_comment (
    id             UUID PRIMARY KEY,
    portal_id      UUID        NOT NULL,
    task_id        UUID        NOT NULL REFERENCES task (id),
    author_user_id UUID        NOT NULL,
    author_name    VARCHAR(200),
    body           TEXT        NOT NULL,
    edited         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_task_comment_task ON task_comment (portal_id, task_id, created_at);

CREATE TABLE task_activity (
    id            UUID PRIMARY KEY,
    portal_id     UUID         NOT NULL,
    task_id       UUID         NOT NULL REFERENCES task (id),
    action        VARCHAR(40)  NOT NULL,
    field         VARCHAR(80),
    old_value     TEXT,
    new_value     TEXT,
    detail        VARCHAR(500),
    actor_user_id UUID,
    actor_name    VARCHAR(200),
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_task_activity_task ON task_activity (portal_id, task_id, created_at DESC);
