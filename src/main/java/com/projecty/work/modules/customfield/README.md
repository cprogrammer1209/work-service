# customfield module: custom fields for projects, tasks and other modules

> **Status: built.** The tables `custom_field`, `custom_field_option` and `custom_field_unique_value` and the
> `custom_values` column on `project` exist (Flyway V3), and so do **layouts** (Flyway V4: `layout`, `layout_section`,
> `layout_field`, and `project.layout_id`), with the admin endpoints below. Not built yet: field permissions and history,
> statuses per layout, layout rules, formula and lookup fields. Tasks will use the same engine when they exist.

## What it is for

Every customer (portal) wants different extra fields. One portal wants "Customer tier" and "Budget" on projects,
another wants "Region". A custom field lets a portal Admin add such fields to a module without a code change, and
lets users fill them in on the form.

One engine serves every module. A field belongs to an `entity_type` (`PROJECT`, `TASK`, later `ISSUE` and others).
Projects get custom fields first; Tasks use the same engine when they exist.

Requirements: `docs/requirements/07-customization-engine.md` in the projecty repo
(https://github.com/cprogrammer1209/projecty/blob/master/docs/requirements/07-customization-engine.md),
in particular CUS-BR-01 to CUS-BR-15. CUS-BR-14 matters most for the storage design: custom values must be
filterable, sortable and groupable like normal fields.

## The idea in one picture

Two kinds of data, kept apart:

| Kind | Example | Where it lives |
|---|---|---|
| The **question** (definition) | "Customer tier": a picklist with Gold, Silver, Bronze | tables owned by this module |
| The **answer** (value) | WEB's customer tier is Gold | one `JSONB` column on the record's own table |

```
custom_field  (this module)                project  (Project module's table)
 f1 | cf_customer_tier | PICKLIST             p1 | WEB | {"cf_customer_tier": "<option id>", "cf_budget": 12000}
 f2 | cf_budget        | DECIMAL              p2 | MOB | {"cf_customer_tier": "<option id>"}
```

- Adding a field adds a row to `custom_field`. No record and no table changes.
- A record with no answer for a field simply has no key for it.
- Each module keeps its answers in its own table (the Project module owns `project.custom_values`), so the
  architecture rule "no module reaches into another module's tables" holds.

## Why JSONB (decision record)

Options considered, for many portals sharing the same tables:

| Option | Verdict |
|---|---|
| A real column per field (`ALTER TABLE` per field) | Rejected. Changes a shared table for every portal, hits the PostgreSQL column limit (about 1,600), name clashes between portals |
| One row per value (EAV table) | Possible but heavy. A list of 100 projects with 10 fields means 1,000 extra rows to join and pivot; every filter field is another join; splits a module's data across two owners |
| Pre-made generic typed columns ("flex columns", `udf_text1`, `udf_num1` ...) | Possible, fixed ceiling per type, needs a slot-mapping layer, unreadable SQL |
| **A `JSONB` column on each record** | **Chosen.** One row per record, saved in the same transaction, no schema change per field, GIN-indexable |

What we accept: the database does not check the values, so this module's validator does (one gate, used by every
save). Unique fields need a helper table. A field that people sort by constantly gets its own expression index.
If one field is still too slow, move just that field to a typed side table (the hybrid fallback).

## Tables

All tables belong to this module except the value column, which is added to each consuming module's own table.

### `custom_field`: one row per field definition (first release)

| Column | Type | Meaning |
|---|---|---|
| `id` | UUID PK | |
| `portal_id` | UUID | The customer the field belongs to |
| `entity_type` | VARCHAR(30) | `PROJECT`, `TASK`, ... |
| `api_name` | VARCHAR(60) | The JSON key, e.g. `cf_customer_tier`. Made from the label once. **Never changes and is never reused** |
| `label` | VARCHAR(100) | What users see. Can be renamed freely |
| `description` | VARCHAR(500) | Help text |
| `field_type` | VARCHAR(20) | See "Types". Cannot change after creation (except safe widenings: number to decimal, single-line to multi-line) |
| `mandatory`, `is_unique`, `sensitive` | BOOLEAN | Required on save / no duplicates / masked and left out of exports |
| `default_value` | JSONB | Applied to new records |
| `config` | JSONB | Per-type rules: `{"min":0,"max":1000,"decimals":2,"maxLength":200,"regex":"..","maxSelections":3}` |
| `status` | VARCHAR(10) | `ACTIVE`, `INACTIVE` (hidden, answers kept), `DELETED` (answers removed, row kept so the key stays reserved) |
| `sort_order` | INT | Position on forms |
| `created_at/by`, `updated_at/by` | | Audit |

`UNIQUE (portal_id, entity_type, api_name)` counts `DELETED` rows too, which is what makes a key unreusable.
Index: `(portal_id, entity_type, status, sort_order)` for loading a form.

### `custom_field_option`: choices of picklists and multi-selects (first release)

`id` (the value a record stores, not the text), `portal_id`, `field_id` (FK), `label`, `color`, `sort_order`,
`is_default`, `active`. An inactive option is hidden from new choices but stays on old records, shown as "inactive".

### `custom_field_unique_value`: only for fields with `is_unique` (first release)

`portal_id`, `field_id`, `value_key` (normalised value), `entity_id`. Primary key `(field_id, value_key)`, so the
database refuses a duplicate. Written in the same transaction as the record.

### The value column (first release)

```sql
ALTER TABLE project ADD COLUMN custom_values JSONB NOT NULL DEFAULT '{}';
CREATE INDEX ix_project_custom_values ON project USING GIN (custom_values);
-- a field people sort by constantly gets its own index, on demand (never automatically):
-- CREATE INDEX CONCURRENTLY ix_project_cf_budget ON project (((custom_values ->> 'cf_budget')::numeric));
```
The default GIN operator class is used (not `jsonb_path_ops`) so that "any of" filters on multi-selects (`?|`) work.
The same column goes on `task` and any other module that gets custom fields.

### Later (V1)

- `custom_field_permission` (`field_id`, `profile_key`, `access`: HIDDEN / READ_ONLY / READ_WRITE)
- `custom_field_history` (entity, field, old and new value, who, when) for the activity stream

### Layouts (Flyway V4)

- `layout` (portal, module, name, description, `is_default`, `active`): at most 3 per module (inactive ones count),
  unique name (any case), exactly one default (partial unique index), never deleted, only deactivated.
- `layout_section` (layout, name, collapsible, order): at least one per layout, unique name within the layout.
- `layout_field` (layout, section, field, order, `mandatory`, `read_only`, `default_value` JSONB, `wide`): a field is
  placed at most once per layout. A null `mandatory` or `wide` means "as the field says"; an override beats the field
  (CUS-BR-04). A pick-one default is the id of one of the field's active choices; unique, person and pick-several fields
  take no layout default.
- `project.layout_id`: the layout a project uses. Fields not placed in it are hidden for that project, but their answers
  stay in `custom_values` (CUS-BR-10) and come back if the field is placed again or the project moves to a layout that
  has them.
- The default layout ("Standard", one section "Details") is created the first time a module needs it, and Flyway V4
  created it for portals that already had projects or fields. A **new field** is appended to the default layout's last
  section; a **deleted field** leaves every layout; a **deactivated field** keeps its place but is not shown.

## Types and how an answer is stored in `custom_values`

| `field_type` | Tier | Stored as |
|---|---|---|
| `TEXT`, `LONG_TEXT` | MVP | `"cf_contract_no": "C-2026-17"` |
| `NUMBER`, `DECIMAL` | MVP | `"cf_budget": 12000.00` (read as `BigDecimal`, never a double) |
| `DATE` | MVP | `"cf_go_live": "2026-12-01"` (ISO string; JSON has no date type) |
| `CHECKBOX` | MVP | `"cf_vip": true` |
| `PICKLIST` | MVP | `"cf_customer_tier": "<option id>"` (id, so renaming an option breaks nothing) |
| `USER` | MVP | `"cf_sponsor": "<user id>"` |
| `MULTI_PICKLIST` | V1 | `"cf_tags": ["<option id>", "<option id>"]` |
| `USER_MULTI`, `EMAIL`, `PHONE`, `URL`, `CURRENCY`, `PERCENT`, `DATETIME`, `RICH_TEXT` | V1 | list / string / string / string / number / number / ISO string / string |
| `LOOKUP`, `FORMULA`, `ATTACHMENT`, dependent picklist | V1 | id / computed (read-only) / file id / see requirements |

A cleared answer **removes the key**; we do not store JSON `null`, so "empty" has one meaning.

## Lifecycle: what happens when a portal adds and removes fields

| Action | What changes in the database |
|---|---|
| Add a field | One `custom_field` row (and option rows). **No record is touched.** Old records read it as empty |
| Make a field mandatory | The definition only. Enforced on saves from then on; old records are not blocked until edited |
| Rename the label | One `custom_field` row. The JSON key (`api_name`) is unchanged |
| Reorder | `sort_order` on definition rows only |
| Deactivate | `status = 'INACTIVE'`. Answers stay; forms, lists, filters and API responses stop showing it; writes are rejected. Activating brings it back |
| Delete for good | Admin sees an impact count first. Then `UPDATE <table> SET custom_values = custom_values - 'cf_x' WHERE portal_id = :p AND custom_values ? 'cf_x'` (in batches for big portals) and `status = 'DELETED'` |
| Change the type | Blocked (except safe widenings). Create a new field instead |
| Remove a picklist option in use | Deactivated, not deleted. Hard delete is blocked until records are fixed, or cleaned with `jsonb_set` |

**Responses are built from the active definitions, not from whatever keys are in the JSON.** A key with no active
definition is ignored when reading, so a half-finished cleanup can never leak values.

**A deleted key is never reused** (the `DELETED` row stays). Otherwise old values could reappear under a newly
created field with the same name.

## How another module uses it (the integration contract)

Other modules (Project now, Task later) use only this module's `api/`:

```java
public interface CustomFieldLookUpService {
    /** Active fields for a module, in form order (the UI draws the form from this). */
    List<CustomFieldResponseDTO> findFields(UUID portalId, String entityType);

    /**
     * The single gate for every save (form, inline edit, import, API). Rejects unknown keys, applies defaults on
     * create, checks type, range, regex, option membership, mandatory and field permissions, and returns the cleaned
     * values. Throws AppException VALIDATION_FAILED with details {api_name: message}.
     */
    Map<String, Object> validate(UUID portalId, String entityType, Map<String, Object> input,
            Map<String, Object> existing, boolean creating);
}
```
The consuming service stores the result on its own row, in the same transaction as the rest of the save.
Unique fields additionally reserve their value through `CustomFieldWriteService.reserveUniqueValues`, and `changedKeys` tells a module which answers differ. The Project module implements `CustomValueHolder` (`ProjectCustomValueHolder`).
This module never calls the consuming modules.

## How updating one field works

Example: set Budget of project WEB from 12000 to 15000.

1. `PATCH /api/work/projects/{id}` with `{"name": ..., "visibility": ..., "customValues": {"cf_budget": 15000}}`. Keys
   left out of `customValues` are untouched; a `null` or empty value clears that key; leaving `customValues` out of the
   request changes none.
2. Security: token, `X-Portal-Id`, `@RequirePermission(PROJECTS_EDIT)`; another portal's project is a 404.
3. In one transaction the project row is read **with a row lock** (`ProjectRepository.lockByIdAndPortalId`, `FOR UPDATE`),
   so two edits of the same project queue instead of overwriting each other.
4. Edit rights (Admin, owner or project Manager), then `CustomFieldLookUpService.validate(...)` for the keys sent.
5. The validated **full map replaces** the stored one (`Project.changeCustomValues`). Because the row is locked and was read
   under the lock, nothing can change in between, so no SQL merge (`||`) is needed. A test of simultaneous edits of
   different fields shows none is lost.
6. In the same transaction: unique reservations (`reserveUniqueValues`), the project activity entry, and a
   `ProjectUpdated` outbox event whose `changes` contain `custom field cf_budget`.

PostgreSQL does not edit in place: it writes a new version of the row and updates the GIN index entries. That is cheap for
the small JSON a record carries (limit: 25 fields per module, CUS-BR-13).

## Reading, filtering, sorting, grouping

```sql
WHERE portal_id = :p AND custom_values @> '{"cf_customer_tier": "<option id>"}'      -- equals (GIN index)
WHERE portal_id = :p AND custom_values @> '{"cf_tags": ["<id>", "<id>"]}'             -- multi-select: has all of
WHERE portal_id = :p AND custom_values -> 'cf_tags' ?| array['<id>', '<id>']          -- multi-select: has any of
ORDER BY (custom_values ->> 'cf_budget')::numeric                                     -- sort (cast; add an expression index if hot)
SELECT custom_values ->> 'cf_customer_tier', count(*) ... GROUP BY 1                  -- group
SELECT t.tag, count(*) FROM project p, jsonb_array_elements_text(p.custom_values -> 'cf_tags') t(tag) ... GROUP BY t.tag
```
Known limits of JSONB to remember: PostgreSQL keeps no statistics on keys inside a JSON column, so a very selective
filter on an unindexed key may get a poor plan (add an index for that field); and JSON `null` differs from a missing
key (we only ever store a missing key).

## Endpoints (all under `/api/work`, `AppResponse` envelope)

| Endpoint | Permission |
|---|---|
| `GET /custom-fields?entityType=PROJECT` (active fields, in form order, with choices) | `CUSTOM_FIELDS_VIEW` |
| `GET /custom-fields/all?entityType=PROJECT` (active and inactive, for the admin screen) | `CUSTOM_FIELDS_MANAGE` |
| `POST /custom-fields` (201) | `CUSTOM_FIELDS_MANAGE` |
| `PATCH /custom-fields/{id}` (the edit form sends label, description, mandatory, sensitive, default, config; never the key, type or "unique") | `CUSTOM_FIELDS_MANAGE` |
| `POST /custom-fields/{id}/deactivate`, `POST /custom-fields/{id}/activate` | `CUSTOM_FIELDS_MANAGE` |
| `GET /custom-fields/{id}/usage` (how many records have an answer: the impact of deleting) | `CUSTOM_FIELDS_MANAGE` |
| `DELETE /custom-fields/{id}` | `CUSTOM_FIELDS_MANAGE` |
| `PUT /custom-fields/reorder` (listed fields first in that order, the rest follow in their current order) | `CUSTOM_FIELDS_MANAGE` |
| `POST /custom-fields/{id}/options`, `PATCH /custom-fields/{id}/options/{optionId}` | `CUSTOM_FIELDS_MANAGE` |

Choices are **deactivated, never deleted** (`active: false`), because old records keep showing them (CUS-BR-03); there is
no endpoint to delete a choice. Only text, number, decimal and date fields can be marked unique. Numbers are kept small
so the JSON round trip is exact: at most 12 digits, DECIMAL at most 6 decimal places.

`CUSTOM_FIELDS_VIEW` (every profile) and `CUSTOM_FIELDS_MANAGE` (Admin) are defined in Platform (`PermissionConstants`,
`MemberConstants.SYSTEM_PROFILES`, Flyway `V9`) and reach Work through the member events.

### Layout endpoints (`/api/work/layouts`)

| Endpoint | Permission |
|---|---|
| `GET ?entityType=PROJECT` (every layout with counts and usage) | `CUSTOM_FIELDS_VIEW` |
| `GET /active?entityType=PROJECT` (active layouts resolved for drawing a form, default first) | `CUSTOM_FIELDS_VIEW` |
| `GET /{id}` (as stored, plus every field that could be placed: the builder) | `CUSTOM_FIELDS_MANAGE` |
| `POST` (blank, or `cloneFromId`) | `CUSTOM_FIELDS_MANAGE` |
| `PUT /{id}` (replaces name, description, sections and placed fields in one go) | `CUSTOM_FIELDS_MANAGE` |
| `POST /{id}/default`, `/activate`, `/deactivate` | `CUSTOM_FIELDS_MANAGE` |

Applying a layout to a project belongs to the project feature: `PUT /api/work/projects/{id}/layout` (`PROJECTS_EDIT` plus the
usual project edit rule). `POST /projects` takes an optional `layoutId`; the detail response carries the resolved `layout`
and `hiddenValues`.

How validation uses a layout: `CustomFieldLookUpService.validate(portalId, entityType, layoutId, input, existing, creating)`
checks only the fields placed in the layout. A key outside the layout is refused; the layout's required flag and default
apply; a read-only field is ignored on create (its default applies) and a change is refused on update.

## Events (Work's outbox)

`LayoutCreated` and `LayoutUpdated` (also for default, activate and deactivate; payload `change`) go to topic
`projecty.layout` (aggregate `LAYOUT`); the project module publishes `ProjectLayoutChanged` on `projecty.project`.

`FieldCreated`, `FieldUpdated` (also used for choice changes and for activating a field), `FieldDeactivated`,
`FieldDeleted`. The aggregate is `FIELD`, so they go to topic `projecty.field` (listed in `projecty.broker.topics`).
Payloads carry `portalId`, `fieldId`, `entityType` and `apiName`, no personal data. Changes to custom values are reported
by the owning module: `ProjectUpdated` lists `custom field <api_name>` in its `changes`. See `docs/07-events.md`.

## Rules this module must enforce

- The key is generated from the label as `cf_` + snake_case, unique per portal and module, never changed, never reused.
- At most 25 non-deleted fields per module per portal (plan limit, checked in code).
- A mandatory field that is Hidden or Read-only for the caller's profile is not required of them, and its default
  applies (CUS-BR-05).
- Hidden fields are left out everywhere for that caller: forms, lists, exports, search, notifications, API responses
  (CUS-BR-12). Field permissions are V1.
- Everything is portal-scoped. Another portal's field or record is a 404.

## Frontend (for context)

The Angular app asks `GET /api/work/custom-fields?entityType=PROJECT` and draws the extra inputs itself, by type
(text box, number, date picker, checkbox, dropdown, chips for multi-select, people picker). A new field appears
without a new release. Admins manage fields under Settings → Custom fields.

## Decisions taken and open questions

- Decided: Projects use the engine first, Tasks when they exist. First types: the MVP set plus multi-select. The V1 tables
  and field types are not built.
- Open: custom modules (a portal-defined record type such as "Risk") are V1 and need a non-fixed `entity_type`; the table
  design already allows it.
- Open: formula language scope (V1), see the requirements.
- Open: the `ProjectCustomValueHolder` cleanup on delete is one `UPDATE` per field; very large portals will need it in
  batches or in the background.
