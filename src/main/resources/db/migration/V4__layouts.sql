-- M1 Step 7 (layouts): which custom fields a record shows, in which sections, with which per-layout rules.
-- A layout belongs to a module (entity_type); a record points to the layout it uses. Fields not placed in a layout are
-- hidden for the records that use it; their answers stay in the record (CUS-BR-10).
CREATE TABLE layout (
    id          UUID PRIMARY KEY,
    portal_id   UUID         NOT NULL,
    entity_type VARCHAR(30)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(300),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  UUID         NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  UUID         NOT NULL
);
CREATE UNIQUE INDEX ux_layout_name ON layout (portal_id, entity_type, lower(name));
-- Exactly one default per module and portal.
CREATE UNIQUE INDEX ux_layout_default ON layout (portal_id, entity_type) WHERE is_default;

CREATE TABLE layout_section (
    id          UUID PRIMARY KEY,
    portal_id   UUID        NOT NULL,
    layout_id   UUID        NOT NULL REFERENCES layout (id) ON DELETE CASCADE,
    name        VARCHAR(50) NOT NULL,
    collapsible BOOLEAN     NOT NULL DEFAULT FALSE,
    sort_order  INT         NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_layout_section_name ON layout_section (layout_id, lower(name));

-- A field placed in a section. A null mandatory or wide means "as the field says" (CUS-BR-04: a layout override beats the field).
CREATE TABLE layout_field (
    id            UUID PRIMARY KEY,
    portal_id     UUID    NOT NULL,
    layout_id     UUID    NOT NULL REFERENCES layout (id) ON DELETE CASCADE,
    section_id    UUID    NOT NULL REFERENCES layout_section (id) ON DELETE CASCADE,
    field_id      UUID    NOT NULL REFERENCES custom_field (id),
    sort_order    INT     NOT NULL DEFAULT 0,
    mandatory     BOOLEAN,
    read_only     BOOLEAN NOT NULL DEFAULT FALSE,
    default_value JSONB,
    wide          BOOLEAN,
    CONSTRAINT uq_layout_field UNIQUE (layout_id, field_id)
);
CREATE INDEX ix_layout_field_section ON layout_field (section_id, sort_order);
CREATE INDEX ix_layout_field_field ON layout_field (field_id);

-- Existing portals: one default layout "Standard" with one section holding every field, and every project on it.
INSERT INTO layout (id, portal_id, entity_type, name, description, is_default, active, created_at, created_by, updated_at, updated_by)
SELECT gen_random_uuid(), p.portal_id, 'PROJECT', 'Standard', 'Every project field in one section.', TRUE, TRUE, now(),
       '00000000-0000-0000-0000-000000000000', now(), '00000000-0000-0000-0000-000000000000'
FROM (SELECT portal_id FROM project UNION SELECT portal_id FROM custom_field WHERE entity_type = 'PROJECT') p;

INSERT INTO layout_section (id, portal_id, layout_id, name, collapsible, sort_order)
SELECT gen_random_uuid(), l.portal_id, l.id, 'Details', FALSE, 0 FROM layout l WHERE l.entity_type = 'PROJECT';

INSERT INTO layout_field (id, portal_id, layout_id, section_id, field_id, sort_order, read_only)
SELECT gen_random_uuid(), f.portal_id, s.layout_id, s.id, f.id,
       ROW_NUMBER() OVER (PARTITION BY f.portal_id ORDER BY f.sort_order, f.created_at) - 1, FALSE
FROM custom_field f
JOIN layout_section s ON s.portal_id = f.portal_id
JOIN layout l ON l.id = s.layout_id AND l.entity_type = 'PROJECT'
WHERE f.entity_type = 'PROJECT' AND f.status <> 'DELETED';

ALTER TABLE project ADD COLUMN layout_id UUID;
UPDATE project p SET layout_id = l.id
FROM layout l WHERE l.portal_id = p.portal_id AND l.entity_type = 'PROJECT' AND l.is_default;
ALTER TABLE project ALTER COLUMN layout_id SET NOT NULL;
ALTER TABLE project ADD CONSTRAINT fk_project_layout FOREIGN KEY (layout_id) REFERENCES layout (id);
CREATE INDEX ix_project_layout ON project (portal_id, layout_id);
