-- SYN-02: stocktake sessions, lines and processed-operation dedup table.
-- Follows docs/architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md.
-- stock_movements already has type IN ('RECEIPT','SALE','ADJUSTMENT'); we
-- add 'STOCKTAKE_ADJUSTMENT' to the CHECK to allow movement references to
-- a stocktake session once INV-03 approves the discrepancy.

-- ── Processed operations (idempotency dedup) ──────────────────────────────────
-- One row per accepted (clientOperationId, organizationId, storeId, actorId).
-- A retry with the same key+scope returns the stored result without re-executing.
CREATE TABLE IF NOT EXISTS inventory.processed_operations (
    client_operation_id uuid        NOT NULL,
    organization_id     uuid        NOT NULL,
    store_id            uuid        NOT NULL,
    actor_id            uuid        NOT NULL,
    payload_hash        bytea       NOT NULL,
    result_status       varchar(32) NOT NULL,
    result_payload      jsonb,
    processed_at        timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (client_operation_id, organization_id, store_id, actor_id),
    FOREIGN KEY (organization_id, store_id)  REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, actor_id)  REFERENCES iam.users(organization_id, id)
);

-- ── Stocktake sessions ────────────────────────────────────────────────────────
-- One per actor/store/day (created lazily on first count).
CREATE TABLE IF NOT EXISTS inventory.stocktakes (
    id              uuid        PRIMARY KEY,
    organization_id uuid        NOT NULL,
    store_id        uuid        NOT NULL,
    actor_id        uuid        NOT NULL,
    status          varchar(16) NOT NULL
                        CHECK (status IN ('OPEN','SUBMITTED','APPROVED','CANCELLED')),
    opened_at       timestamptz NOT NULL DEFAULT now(),
    submitted_at    timestamptz,
    FOREIGN KEY (organization_id, store_id)  REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, actor_id)  REFERENCES iam.users(organization_id, id),
    UNIQUE (organization_id, id)
);
CREATE INDEX stocktakes_actor ON inventory.stocktakes(organization_id, store_id, actor_id, status);

-- ── Stocktake lines ───────────────────────────────────────────────────────────
-- One row per (stocktake_id, batch_id); server enforces uniqueness per session.
CREATE TABLE IF NOT EXISTS inventory.stocktake_lines (
    id                  uuid        PRIMARY KEY,
    stocktake_id        uuid        NOT NULL REFERENCES inventory.stocktakes(id),
    organization_id     uuid        NOT NULL,
    store_id            uuid        NOT NULL,
    product_id          uuid        NOT NULL,
    batch_id            uuid        NOT NULL,
    client_operation_id uuid        NOT NULL,
    expected_quantity   numeric(14,3) NOT NULL CHECK (expected_quantity >= 0),
    actual_quantity     numeric(14,3) NOT NULL CHECK (actual_quantity   >= 0),
    base_version        bigint      NOT NULL,
    note                varchar(500) NOT NULL DEFAULT '',
    status              varchar(16) NOT NULL
                            CHECK (status IN ('PENDING','CONFLICT','APPROVED')),
    conflict_reason     varchar(500),
    counted_at          timestamptz NOT NULL,
    FOREIGN KEY (organization_id, store_id)    REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, product_id)  REFERENCES catalog.products(organization_id, id),
    FOREIGN KEY (organization_id, batch_id)    REFERENCES inventory.product_batches(organization_id, id),
    UNIQUE (stocktake_id, batch_id),
    UNIQUE (organization_id, client_operation_id)
);
CREATE INDEX stocktake_lines_session ON inventory.stocktake_lines(stocktake_id, status);

-- ── Extend stock_movements to allow STOCKTAKE_ADJUSTMENT ─────────────────────
-- The existing CHECK only allows RECEIPT, SALE, ADJUSTMENT.
-- Drop and re-create the constraint to add the new type.
-- (Safe: no existing row has this value; constraint is NOT DEFERRABLE.)
ALTER TABLE inventory.stock_movements
    DROP CONSTRAINT IF EXISTS stock_movements_movement_type_check;
ALTER TABLE inventory.stock_movements
    ADD  CONSTRAINT stock_movements_movement_type_check
    CHECK (movement_type IN ('RECEIPT','SALE','ADJUSTMENT','STOCKTAKE_ADJUSTMENT'));

ALTER TABLE inventory.stock_movements
    DROP CONSTRAINT IF EXISTS stock_movements_reference_type_check;
ALTER TABLE inventory.stock_movements
    ADD  CONSTRAINT stock_movements_reference_type_check
    CHECK (reference_type IN ('GOODS_RECEIPT','INVOICE','ADJUSTMENT','STOCKTAKE'));
