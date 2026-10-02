-- INV-01: goods receipts, product batches, inventory balances, stock movements.
-- Follows docs/architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md.
-- All writes in one transaction per SPRINT_2_BOUNDARY_DRAFT contract rule 1.

CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS audit;

-- ── Goods receipts ────────────────────────────────────────────────────────────
CREATE TABLE inventory.goods_receipts (
    id                  uuid PRIMARY KEY,
    organization_id     uuid NOT NULL REFERENCES core.organizations(id),
    store_id            uuid NOT NULL,
    supplier_id         uuid NOT NULL,
    status              varchar(16) NOT NULL CHECK (status IN ('CONFIRMED')),
    received_at         timestamptz NOT NULL,
    confirmed_by        uuid NOT NULL,
    client_operation_id uuid NOT NULL,
    idempotency_key     uuid NOT NULL,
    payload_hash        bytea NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (organization_id, store_id)     REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, supplier_id)  REFERENCES catalog.suppliers(organization_id, id),
    FOREIGN KEY (organization_id, confirmed_by) REFERENCES iam.users(organization_id, id),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, client_operation_id),
    UNIQUE (idempotency_key)
);

CREATE TABLE inventory.goods_receipt_lines (
    id                  uuid PRIMARY KEY,
    receipt_id          uuid NOT NULL REFERENCES inventory.goods_receipts(id),
    organization_id     uuid NOT NULL,
    product_id          uuid NOT NULL,
    expected_quantity   numeric(14,3) NOT NULL CHECK (expected_quantity > 0),
    delivered_quantity  numeric(14,3) NOT NULL CHECK (delivered_quantity >= 0),
    accepted_quantity   numeric(14,3) NOT NULL CHECK (accepted_quantity >= 0),
    rejected_quantity   numeric(14,3) NOT NULL CHECK (rejected_quantity >= 0),
    unit_cost           bigint NOT NULL CHECK (unit_cost >= 0),
    supplier_lot_number varchar(100),
    expiry_date         date,
    discrepancy_reason  varchar(500),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    CHECK (accepted_quantity + rejected_quantity = delivered_quantity),
    CHECK (rejected_quantity = 0 OR discrepancy_reason IS NOT NULL),
    CHECK (delivered_quantity = expected_quantity OR discrepancy_reason IS NOT NULL)
);

-- ── Product batches ───────────────────────────────────────────────────────────
CREATE TABLE inventory.product_batches (
    id                  uuid PRIMARY KEY,
    organization_id     uuid NOT NULL,
    store_id            uuid NOT NULL,
    product_id          uuid NOT NULL,
    receipt_line_id     uuid NOT NULL REFERENCES inventory.goods_receipt_lines(id),
    batch_number        varchar(100) NOT NULL,
    supplier_lot_number varchar(100),
    expiry_date         date,
    received_date       date NOT NULL,
    status              varchar(16) NOT NULL CHECK (status IN ('AVAILABLE', 'BLOCKED', 'EXHAUSTED')),
    FOREIGN KEY (organization_id, store_id)   REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    UNIQUE (organization_id, id)
);
CREATE INDEX product_batches_product ON inventory.product_batches(organization_id, store_id, product_id, status);

-- ── Inventory balances ────────────────────────────────────────────────────────
-- One row per (organization, store, product, batch); updated atomically with each movement.
CREATE TABLE inventory.inventory_balances (
    id               uuid PRIMARY KEY,
    organization_id  uuid NOT NULL,
    store_id         uuid NOT NULL,
    product_id       uuid NOT NULL,
    batch_id         uuid NOT NULL,
    on_hand_quantity numeric(14,3) NOT NULL DEFAULT 0 CHECK (on_hand_quantity >= 0),
    version          bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    FOREIGN KEY (organization_id, store_id)   REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    FOREIGN KEY (organization_id, batch_id)   REFERENCES inventory.product_batches(organization_id, id),
    UNIQUE (organization_id, batch_id)
);

-- ── Stock movements ───────────────────────────────────────────────────────────
CREATE TABLE inventory.stock_movements (
    id              uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id        uuid NOT NULL,
    product_id      uuid NOT NULL,
    batch_id        uuid NOT NULL,
    movement_type   varchar(16)  NOT NULL CHECK (movement_type IN ('RECEIPT', 'SALE', 'ADJUSTMENT')),
    quantity_delta  numeric(14,3) NOT NULL,
    reference_id    uuid         NOT NULL,
    reference_type  varchar(32)  NOT NULL CHECK (reference_type IN ('GOODS_RECEIPT', 'INVOICE', 'ADJUSTMENT')),
    occurred_at     timestamptz  NOT NULL,
    recorded_by     uuid         NOT NULL,
    FOREIGN KEY (organization_id, store_id)    REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, product_id)  REFERENCES catalog.products(organization_id, id),
    FOREIGN KEY (organization_id, batch_id)    REFERENCES inventory.product_batches(organization_id, id),
    FOREIGN KEY (organization_id, recorded_by) REFERENCES iam.users(organization_id, id)
);
CREATE INDEX stock_movements_ref   ON inventory.stock_movements(reference_id, reference_type);
CREATE INDEX stock_movements_batch ON inventory.stock_movements(batch_id, occurred_at);

-- ── Audit log (shared, append-only) ──────────────────────────────────────────
CREATE TABLE audit.audit_logs (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id uuid NOT NULL,
    actor_id        uuid NOT NULL,
    action          varchar(100) NOT NULL,
    entity_type     varchar(100) NOT NULL,
    entity_id       uuid NOT NULL,
    occurred_at     timestamptz NOT NULL DEFAULT now(),
    detail          jsonb
);
CREATE INDEX audit_logs_entity ON audit.audit_logs(entity_type, entity_id);
CREATE INDEX audit_logs_actor  ON audit.audit_logs(actor_id, occurred_at);
