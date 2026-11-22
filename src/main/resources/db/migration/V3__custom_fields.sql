-- M1 Step 7 (custom fields, data foundation): the definitions, the picklist choices, and the unique-value helper.
-- The answers live in a JSONB column on each record table (here: project.custom_values), keyed by the field's api_name.
CREATE TABLE custom_field (
    id            UUID PRIMARY KEY,
    portal_id     UUID         NOT NULL,
    entity_type   VARCHAR(30)  NOT NULL,
    api_name      VARCHAR(60)  NOT NULL,
    label         VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    field_type    VARCHAR(20)  NOT NULL,
    mandatory     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_unique     BOOLEAN      NOT NULL DEFAULT FALSE,
    sensitive     BOOLEAN      NOT NULL DEFAULT FALSE,
    default_value JSONB,
    config        JSONB        NOT NULL DEFAULT '{}',
    status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    created_by    UUID         NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    updated_by    UUID         NOT NULL,
    -- CUS-BR-01: the key never changes. DELETED rows stay, so a deleted key is never reused.
    CONSTRAINT uq_custom_field_api UNIQUE (portal_id, entity_type, api_name),
    CONSTRAINT ck_custom_field_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);
CREATE INDEX ix_custom_field_form ON custom_field (portal_id, entity_type, status, sort_order);

CREATE TABLE custom_field_option (
    id         UUID PRIMARY KEY,
    portal_id  UUID         NOT NULL,
    field_id   UUID         NOT NULL REFERENCES custom_field (id),
    label      VARCHAR(100) NOT NULL,
    color      VARCHAR(20),
    sort_order INT          NOT NULL DEFAULT 0,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_custom_field_option_field ON custom_field_option (field_id, sort_order);

-- Only for fields flagged unique: the primary key makes the database refuse a duplicate.
CREATE TABLE custom_field_unique_value (
    portal_id UUID NOT NULL,
    field_id  UUID NOT NULL REFERENCES custom_field (id),
    value_key TEXT NOT NULL,
    entity_id UUID NOT NULL,
    PRIMARY KEY (field_id, value_key)
);
CREATE INDEX ix_custom_field_unique_entity ON custom_field_unique_value (field_id, entity_id);

-- The answers. The default operator class keeps the "any of" operator (?|) usable for multi-selects.
ALTER TABLE project ADD COLUMN custom_values JSONB NOT NULL DEFAULT '{}';
CREATE INDEX ix_project_custom_values ON project USING GIN (custom_values);
