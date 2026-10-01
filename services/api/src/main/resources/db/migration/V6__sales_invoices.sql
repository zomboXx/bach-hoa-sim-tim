-- SAL-01: sales schema (invoice, payment) + stock_movements cross-schema.
-- sales.invoices is the authoritative record of a completed sale.
-- invoice_lines hold per-product snapshots; invoice_line_batches record FEFO allocation.
-- stock_movements is placed here (not V5) because it holds a FK to sales.invoice_line_batches.

CREATE SCHEMA sales;

CREATE TABLE sales.invoices (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid NOT NULL,
    invoice_no varchar(60) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'COMPLETED', 'VOIDED')),
    sold_by uuid NOT NULL,
    sold_at timestamptz NOT NULL,
    subtotal bigint NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    discount_total bigint NOT NULL DEFAULT 0 CHECK (discount_total >= 0),
    grand_total bigint NOT NULL DEFAULT 0 CHECK (grand_total >= 0),
    paid_total bigint NOT NULL DEFAULT 0 CHECK (paid_total >= 0),
    voided_by uuid,
    void_reason varchar(500),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, store_id, id),
    UNIQUE (organization_id, store_id, invoice_no),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, sold_by) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, voided_by) REFERENCES iam.users(organization_id, id),
    CHECK (discount_total <= subtotal),
    CHECK (grand_total = subtotal - discount_total),
    CHECK (status <> 'VOIDED' OR (voided_by IS NOT NULL AND void_reason IS NOT NULL))
);

-- Snapshot columns preserve what the customer paid when catalog data later changes.
CREATE TABLE sales.invoice_lines (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    product_id uuid NOT NULL,
    sku_snapshot varchar(80) NOT NULL,
    product_name_snapshot varchar(200) NOT NULL,
    quantity numeric(14,3) NOT NULL CHECK (quantity > 0),
    unit_price bigint NOT NULL CHECK (unit_price >= 0),
    discount_amount bigint NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    line_total bigint NOT NULL CHECK (line_total >= 0),
    applied_promotion_id uuid,
    UNIQUE (organization_id, store_id, id, product_id),
    FOREIGN KEY (organization_id, store_id, invoice_id)
        REFERENCES sales.invoices(organization_id, store_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id)
    -- FK to sales.promotions deferred to PRO-01B migration (promotions table not yet present)
);

CREATE TABLE sales.invoice_line_batches (
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    invoice_line_id uuid NOT NULL,
    product_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    quantity numeric(14,3) NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (invoice_line_id, product_batch_id),
    UNIQUE (organization_id, store_id, invoice_line_id, product_batch_id),
    FOREIGN KEY (organization_id, store_id, invoice_line_id, product_id)
        REFERENCES sales.invoice_lines(organization_id, store_id, id, product_id),
    FOREIGN KEY (organization_id, store_id, product_batch_id, product_id)
        REFERENCES inventory.product_batches(organization_id, store_id, id, product_id)
);

CREATE TABLE sales.payments (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    method varchar(24) NOT NULL CHECK (method IN ('CASH', 'TRANSFER', 'CARD', 'OTHER')),
    status varchar(16) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    amount bigint NOT NULL CHECK (amount > 0),
    reference_code varchar(100),
    paid_at timestamptz,
    FOREIGN KEY (organization_id, store_id, invoice_id)
        REFERENCES sales.invoices(organization_id, store_id, id),
    CHECK (status <> 'COMPLETED' OR paid_at IS NOT NULL)
);

-- stock_movements placed here because it holds a cross-schema FK to sales.invoice_line_batches.
CREATE TABLE inventory.stock_movements (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    movement_type varchar(24) NOT NULL CHECK (movement_type IN
        ('RECEIPT', 'SALE', 'STOCKTAKE_ADJUSTMENT', 'DISPOSAL', 'SALE_VOID', 'REVERSAL')),
    quantity_delta numeric(14,3) NOT NULL CHECK (quantity_delta <> 0),
    receipt_line_id uuid UNIQUE,
    invoice_line_id uuid,
    stocktake_line_id uuid UNIQUE,
    disposal_line_id uuid UNIQUE,
    reversed_movement_id uuid UNIQUE,
    occurred_at timestamptz NOT NULL,
    actor_user_id uuid NOT NULL,
    UNIQUE (organization_id, store_id, id, product_batch_id),
    UNIQUE (invoice_line_id, product_batch_id),
    FOREIGN KEY (organization_id, store_id, product_batch_id)
        REFERENCES inventory.product_batches(organization_id, store_id, id),
    FOREIGN KEY (organization_id, store_id, product_batch_id, receipt_line_id)
        REFERENCES inventory.product_batches(organization_id, store_id, id, receipt_line_id),
    FOREIGN KEY (organization_id, store_id, invoice_line_id, product_batch_id)
        REFERENCES sales.invoice_line_batches(organization_id, store_id, invoice_line_id, product_batch_id),
    FOREIGN KEY (organization_id, actor_user_id) REFERENCES iam.users(organization_id, id),
    CHECK (num_nonnulls(receipt_line_id, invoice_line_id, stocktake_line_id,
        disposal_line_id, reversed_movement_id) = 1),
    CHECK (reversed_movement_id IS NULL OR reversed_movement_id <> id),
    CHECK ((movement_type = 'RECEIPT' AND receipt_line_id IS NOT NULL)
        OR (movement_type = 'SALE' AND invoice_line_id IS NOT NULL)
        OR (movement_type = 'STOCKTAKE_ADJUSTMENT' AND stocktake_line_id IS NOT NULL)
        OR (movement_type = 'DISPOSAL' AND disposal_line_id IS NOT NULL)
        OR (movement_type IN ('SALE_VOID', 'REVERSAL') AND reversed_movement_id IS NOT NULL)),
    CHECK ((movement_type IN ('RECEIPT', 'SALE_VOID') AND quantity_delta > 0)
        OR (movement_type IN ('SALE', 'DISPOSAL') AND quantity_delta < 0)
        OR movement_type IN ('STOCKTAKE_ADJUSTMENT', 'REVERSAL'))
);

CREATE INDEX invoice_history ON sales.invoices(store_id, sold_at DESC);
CREATE INDEX stock_movement_history ON inventory.stock_movements(store_id, product_batch_id, occurred_at DESC);

-- Permissions for SAL-01: sales.*
INSERT INTO iam.permissions(id, code, description) VALUES
    ('30000000-0000-0000-0000-000000000001', 'sales.read',  'Read sales invoices and preview quotes within session scope'),
    ('30000000-0000-0000-0000-000000000002', 'sales.write', 'Create and checkout sales invoices within session scope');

-- sales.read: all roles; sales.write: SALES, MANAGER, ADMIN
INSERT INTO iam.role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE p.code = 'sales.read'
   OR (p.code = 'sales.write' AND r.code IN ('SALES', 'MANAGER', 'ADMIN'))
ON CONFLICT DO NOTHING;
