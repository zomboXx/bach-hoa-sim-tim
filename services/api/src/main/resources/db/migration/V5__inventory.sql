-- SAL-01: inventory schema — goods receipt chain, product batches and balance.
-- stock_movements is created in V6 because it holds a cross-schema FK to sales.invoice_line_batches.
-- INV-01 (TV2) will add API endpoints on top of these tables; the migration itself is owned here
-- because SAL-01 is the first sprint to read batches and reduce balance atomically.

CREATE SCHEMA inventory;

CREATE TABLE inventory.goods_receipts (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    receipt_no varchar(60) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    received_at timestamptz NOT NULL,
    created_by uuid NOT NULL,
    confirmed_by uuid,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, store_id, id),
    UNIQUE (organization_id, store_id, receipt_no),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, supplier_id) REFERENCES catalog.suppliers(organization_id, id),
    FOREIGN KEY (organization_id, created_by) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, confirmed_by) REFERENCES iam.users(organization_id, id),
    CHECK ((status = 'CONFIRMED') = (confirmed_by IS NOT NULL))
);

CREATE TABLE inventory.goods_receipt_lines (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    product_id uuid NOT NULL,
    expected_quantity numeric(14,3) CHECK (expected_quantity >= 0),
    delivered_quantity numeric(14,3) NOT NULL CHECK (delivered_quantity >= 0),
    accepted_quantity numeric(14,3) NOT NULL CHECK (accepted_quantity >= 0),
    rejected_quantity numeric(14,3) NOT NULL CHECK (rejected_quantity >= 0),
    unit_cost bigint NOT NULL CHECK (unit_cost >= 0),
    supplier_lot_number varchar(100),
    expiry_date date,
    discrepancy_reason varchar(500),
    UNIQUE (organization_id, store_id, id, product_id),
    FOREIGN KEY (organization_id, store_id, receipt_id)
        REFERENCES inventory.goods_receipts(organization_id, store_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    CHECK (accepted_quantity + rejected_quantity <= delivered_quantity),
    CHECK (discrepancy_reason IS NULL OR btrim(discrepancy_reason) <> ''),
    CHECK (discrepancy_reason IS NOT NULL OR
        (rejected_quantity = 0 AND (expected_quantity IS NULL OR expected_quantity = delivered_quantity)))
);

CREATE TABLE inventory.product_batches (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_id uuid NOT NULL,
    receipt_line_id uuid NOT NULL UNIQUE,
    internal_batch_code varchar(100) NOT NULL,
    supplier_lot_number varchar(100),
    received_date date NOT NULL,
    expiry_date date,
    status varchar(16) NOT NULL CHECK (status IN ('AVAILABLE', 'BLOCKED', 'DEPLETED')),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, store_id, id),
    UNIQUE (organization_id, store_id, id, product_id),
    UNIQUE (organization_id, store_id, id, receipt_line_id),
    UNIQUE (organization_id, store_id, internal_batch_code),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    FOREIGN KEY (organization_id, store_id, receipt_line_id, product_id)
        REFERENCES inventory.goods_receipt_lines(organization_id, store_id, id, product_id),
    CHECK (expiry_date IS NULL OR expiry_date >= received_date)
);

-- Transactional read model derived from immutable stock_movements.
CREATE TABLE inventory.inventory_balances (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_batch_id uuid NOT NULL UNIQUE,
    quantity_on_hand numeric(14,3) NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    FOREIGN KEY (organization_id, store_id, product_batch_id)
        REFERENCES inventory.product_batches(organization_id, store_id, id)
);

CREATE INDEX batch_fefo ON inventory.product_batches(store_id, product_id, expiry_date ASC NULLS LAST, id);
