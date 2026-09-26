-- DB-01 physical schema proposal, 2026-09-27. NOT an accepted Flyway migration.
-- Target model: 38 Word tables plus one proposed auth session table for server logout.
-- Monetary bigint VND and quantity numeric(14,3) are reviewable choices, not frozen contracts.
-- Run only against a fresh disposable PostgreSQL 17+ database.

CREATE SCHEMA core;
CREATE SCHEMA iam;
CREATE SCHEMA catalog;
CREATE SCHEMA inventory;
CREATE SCHEMA sales;
CREATE SCHEMA sync;
CREATE SCHEMA audit;
CREATE SCHEMA training;

CREATE TABLE core.organizations (
    id uuid PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE core.stores (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(40) NOT NULL,
    name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code)
);

CREATE TABLE iam.users (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    username varchar(80) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    training_enabled boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, username)
);
CREATE UNIQUE INDEX user_username_normalized ON iam.users(organization_id, lower(username));

-- Proposed addition to the Word model: revoke a login without storing raw tokens.
CREATE TABLE iam.auth_sessions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    user_id uuid NOT NULL,
    store_id uuid,
    token_hash bytea NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32),
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    last_seen_at timestamptz,
    FOREIGN KEY (organization_id, user_id) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    CHECK (expires_at > issued_at),
    CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);
CREATE INDEX auth_session_user_expiry ON iam.auth_sessions(user_id, expires_at);

CREATE TABLE iam.roles (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(16) NOT NULL CHECK (code IN ('SALES', 'STOCK', 'MANAGER', 'ADMIN')),
    name varchar(100) NOT NULL,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code)
);

CREATE TABLE iam.permissions (
    id uuid PRIMARY KEY,
    code varchar(100) NOT NULL UNIQUE,
    description varchar(255) NOT NULL
);

-- organization_id repeats parent scope so composite FKs reject cross-tenant grants.
CREATE TABLE iam.user_roles (
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    store_id uuid NOT NULL,
    assigned_at timestamptz NOT NULL,
    assigned_by uuid NOT NULL,
    PRIMARY KEY (user_id, role_id, store_id),
    FOREIGN KEY (organization_id, user_id) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, role_id) REFERENCES iam.roles(organization_id, id),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, assigned_by) REFERENCES iam.users(organization_id, id)
);

CREATE TABLE iam.role_permissions (
    role_id uuid NOT NULL REFERENCES iam.roles(id),
    permission_id uuid NOT NULL REFERENCES iam.permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE catalog.categories (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    parent_id uuid,
    code varchar(40) NOT NULL,
    name varchar(200) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code),
    FOREIGN KEY (organization_id, parent_id) REFERENCES catalog.categories(organization_id, id),
    CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE UNIQUE INDEX category_code_normalized ON catalog.categories(organization_id, lower(code));

CREATE TABLE catalog.units (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(40) NOT NULL,
    name varchar(100) NOT NULL,
    precision_scale smallint NOT NULL CHECK (precision_scale BETWEEN 0 AND 3),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code)
);

CREATE TABLE catalog.products (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    category_id uuid NOT NULL,
    base_unit_id uuid NOT NULL,
    sku varchar(80) NOT NULL,
    name varchar(200) NOT NULL,
    tracks_expiry boolean NOT NULL DEFAULT false,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, sku),
    FOREIGN KEY (organization_id, category_id) REFERENCES catalog.categories(organization_id, id),
    FOREIGN KEY (organization_id, base_unit_id) REFERENCES catalog.units(organization_id, id)
);
CREATE UNIQUE INDEX product_sku_normalized ON catalog.products(organization_id, lower(sku));

CREATE TABLE catalog.product_barcodes (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    product_id uuid NOT NULL,
    barcode varchar(100) NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    UNIQUE (organization_id, barcode),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id)
);
CREATE UNIQUE INDEX product_one_primary_barcode
    ON catalog.product_barcodes(product_id) WHERE is_primary;

CREATE TABLE catalog.suppliers (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(40) NOT NULL,
    name varchar(200) NOT NULL,
    phone varchar(40),
    email varchar(255),
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code)
);
CREATE UNIQUE INDEX supplier_code_normalized ON catalog.suppliers(organization_id, lower(code));

CREATE TABLE catalog.product_prices (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid NOT NULL,
    product_id uuid NOT NULL,
    sale_price bigint NOT NULL CHECK (sale_price >= 0),
    effective_from timestamptz NOT NULL,
    effective_to timestamptz,
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    CHECK (effective_to IS NULL OR effective_to > effective_from)
);
-- PostgreSQL's bundled btree_gist enforces this under concurrent writes.
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE catalog.product_prices ADD CONSTRAINT product_price_no_overlap
    EXCLUDE USING gist (
        store_id WITH =,
        product_id WITH =,
        tstzrange(effective_from, effective_to, '[)') WITH &&
    );

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

-- A transactional read model derived from immutable stock movements.
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

CREATE TABLE inventory.stocktakes (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid NOT NULL,
    stocktake_no varchar(60) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN
        ('DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    started_by uuid NOT NULL,
    started_at timestamptz NOT NULL,
    submitted_at timestamptz,
    approved_by uuid,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, store_id, id),
    UNIQUE (organization_id, store_id, stocktake_no),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, started_by) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, approved_by) REFERENCES iam.users(organization_id, id),
    CHECK (status <> 'APPROVED' OR approved_by IS NOT NULL)
);

CREATE TABLE inventory.stocktake_lines (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    stocktake_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    system_quantity numeric(14,3) NOT NULL CHECK (system_quantity >= 0),
    counted_quantity numeric(14,3) CHECK (counted_quantity >= 0),
    difference_quantity numeric(14,3)
        GENERATED ALWAYS AS (counted_quantity - system_quantity) STORED,
    base_version bigint NOT NULL CHECK (base_version >= 0),
    UNIQUE (stocktake_id, product_batch_id),
    UNIQUE (organization_id, store_id, id, product_batch_id),
    FOREIGN KEY (organization_id, store_id, stocktake_id)
        REFERENCES inventory.stocktakes(organization_id, store_id, id),
    FOREIGN KEY (organization_id, store_id, product_batch_id)
        REFERENCES inventory.product_batches(organization_id, store_id, id)
);

-- Planned after the core MVP; present here so the Word target model is complete.
CREATE TABLE inventory.stock_disposals (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid NOT NULL,
    disposal_no varchar(60) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    reason varchar(500) NOT NULL,
    created_by uuid NOT NULL,
    approved_by uuid,
    created_at timestamptz NOT NULL,
    approved_at timestamptz,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (organization_id, store_id, id),
    UNIQUE (organization_id, store_id, disposal_no),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, created_by) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, approved_by) REFERENCES iam.users(organization_id, id),
    CHECK (status <> 'APPROVED' OR (approved_by IS NOT NULL AND approved_at IS NOT NULL))
);

CREATE TABLE inventory.stock_disposal_lines (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    disposal_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    quantity numeric(14,3) NOT NULL CHECK (quantity > 0),
    reason varchar(500) NOT NULL,
    UNIQUE (organization_id, store_id, id),
    UNIQUE (organization_id, store_id, id, product_batch_id),
    FOREIGN KEY (organization_id, store_id, disposal_id)
        REFERENCES inventory.stock_disposals(organization_id, store_id, id),
    FOREIGN KEY (organization_id, store_id, product_batch_id)
        REFERENCES inventory.product_batches(organization_id, store_id, id)
);

-- Members and points belong to the later-sprint target, not the MVP migration.
CREATE TABLE sales.members (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    member_code varchar(60) NOT NULL,
    full_name varchar(200) NOT NULL,
    phone varchar(40),
    email varchar(255),
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    joined_at timestamptz NOT NULL,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, member_code)
);

CREATE TABLE sales.promotions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid,
    code varchar(60) NOT NULL,
    name varchar(200) NOT NULL,
    discount_type varchar(16) NOT NULL CHECK (discount_type IN ('AMOUNT', 'PERCENT')),
    discount_value numeric(14,2) NOT NULL CHECK (discount_value > 0),
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, code),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    CHECK (ends_at > starts_at),
    CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
    CHECK (discount_type <> 'AMOUNT' OR discount_value = trunc(discount_value))
);

CREATE TABLE sales.promotion_products (
    organization_id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    product_id uuid NOT NULL,
    PRIMARY KEY (promotion_id, product_id),
    FOREIGN KEY (organization_id, promotion_id) REFERENCES sales.promotions(organization_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id)
);

CREATE TABLE sales.promotion_batches (
    organization_id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    PRIMARY KEY (promotion_id, product_batch_id),
    FOREIGN KEY (organization_id, promotion_id) REFERENCES sales.promotions(organization_id, id),
    FOREIGN KEY (organization_id, product_batch_id)
        REFERENCES inventory.product_batches(organization_id, id)
);

CREATE TABLE sales.invoices (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid NOT NULL,
    member_id uuid,
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
    FOREIGN KEY (organization_id, member_id) REFERENCES sales.members(organization_id, id),
    FOREIGN KEY (organization_id, sold_by) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (organization_id, voided_by) REFERENCES iam.users(organization_id, id),
    CHECK (discount_total <= subtotal),
    CHECK (grand_total = subtotal - discount_total),
    CHECK (status <> 'VOIDED' OR (voided_by IS NOT NULL AND void_reason IS NOT NULL))
);

-- Snapshot columns preserve what the customer paid when catalog data changes.
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
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id),
    FOREIGN KEY (organization_id, applied_promotion_id)
        REFERENCES sales.promotions(organization_id, id)
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

CREATE TABLE sales.loyalty_point_transactions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    member_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    transaction_type varchar(16) NOT NULL CHECK (transaction_type IN ('EARN', 'REVERSE')),
    points_delta bigint NOT NULL CHECK (points_delta <> 0),
    reversed_transaction_id uuid UNIQUE,
    occurred_at timestamptz NOT NULL,
    actor_user_id uuid NOT NULL,
    UNIQUE (invoice_id, transaction_type),
    FOREIGN KEY (organization_id, member_id) REFERENCES sales.members(organization_id, id),
    FOREIGN KEY (organization_id, store_id, invoice_id)
        REFERENCES sales.invoices(organization_id, store_id, id),
    FOREIGN KEY (organization_id, actor_user_id) REFERENCES iam.users(organization_id, id),
    FOREIGN KEY (reversed_transaction_id) REFERENCES sales.loyalty_point_transactions(id),
    CHECK ((transaction_type = 'EARN' AND points_delta > 0 AND reversed_transaction_id IS NULL)
        OR (transaction_type = 'REVERSE' AND points_delta < 0 AND reversed_transaction_id IS NOT NULL))
);

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
    FOREIGN KEY (organization_id, store_id, stocktake_line_id, product_batch_id)
        REFERENCES inventory.stocktake_lines(organization_id, store_id, id, product_batch_id),
    FOREIGN KEY (organization_id, store_id, disposal_line_id, product_batch_id)
        REFERENCES inventory.stock_disposal_lines(organization_id, store_id, id, product_batch_id),
    FOREIGN KEY (organization_id, store_id, reversed_movement_id, product_batch_id)
        REFERENCES inventory.stock_movements(organization_id, store_id, id, product_batch_id),
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

CREATE TABLE sync.processed_operations (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid,
    client_operation_id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    device_id varchar(100),
    operation_type varchar(40) NOT NULL,
    aggregate_type varchar(40) NOT NULL,
    aggregate_id uuid,
    base_version bigint CHECK (base_version >= 0),
    status varchar(16) NOT NULL CHECK (status IN ('PROCESSING', 'APPLIED', 'CONFLICT', 'REJECTED')),
    request_hash varchar(128) NOT NULL,
    result_data jsonb,
    error_code varchar(80),
    received_at timestamptz NOT NULL,
    processed_at timestamptz,
    UNIQUE (organization_id, client_operation_id),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, actor_user_id) REFERENCES iam.users(organization_id, id),
    CHECK ((status = 'PROCESSING') = (processed_at IS NULL))
);

CREATE TABLE audit.audit_logs (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    store_id uuid,
    actor_user_id uuid,
    action varchar(80) NOT NULL,
    entity_type varchar(80) NOT NULL,
    entity_id uuid NOT NULL,
    old_values jsonb,
    new_values jsonb,
    reason varchar(500),
    correlation_id uuid,
    occurred_at timestamptz NOT NULL,
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, actor_user_id) REFERENCES iam.users(organization_id, id)
);

CREATE TABLE training.scenarios (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES core.organizations(id),
    code varchar(80) NOT NULL,
    name varchar(200) NOT NULL,
    scenario_version integer NOT NULL CHECK (scenario_version > 0),
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    fixture_data jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, id, scenario_version),
    UNIQUE (organization_id, code, scenario_version)
);

CREATE TABLE training.scenario_steps (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    scenario_id uuid NOT NULL,
    step_order integer NOT NULL CHECK (step_order > 0),
    action_type varchar(60) NOT NULL,
    target_key varchar(100) NOT NULL,
    instruction_text text NOT NULL,
    validation_rule jsonb NOT NULL,
    score_weight numeric(8,2) NOT NULL CHECK (score_weight >= 0),
    hint_text text,
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, scenario_id, id),
    UNIQUE (scenario_id, step_order),
    FOREIGN KEY (organization_id, scenario_id) REFERENCES training.scenarios(organization_id, id)
);

CREATE TABLE training.sessions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    scenario_id uuid NOT NULL,
    scenario_version integer NOT NULL CHECK (scenario_version > 0),
    learner_user_id uuid NOT NULL,
    attempt_number integer NOT NULL CHECK (attempt_number > 0),
    status varchar(16) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    state_data jsonb NOT NULL,
    started_at timestamptz NOT NULL,
    completed_at timestamptz,
    total_score numeric(10,2) CHECK (total_score >= 0),
    UNIQUE (organization_id, id),
    UNIQUE (organization_id, scenario_id, id),
    UNIQUE (scenario_id, learner_user_id, attempt_number),
    FOREIGN KEY (organization_id, scenario_id, scenario_version)
        REFERENCES training.scenarios(organization_id, id, scenario_version),
    FOREIGN KEY (organization_id, learner_user_id) REFERENCES iam.users(organization_id, id),
    CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);

CREATE TABLE training.session_actions (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    scenario_id uuid NOT NULL,
    session_id uuid NOT NULL,
    scenario_step_id uuid,
    action_sequence integer NOT NULL CHECK (action_sequence > 0),
    action_type varchar(60) NOT NULL,
    target_key varchar(100),
    action_data jsonb,
    evaluation_status varchar(20) NOT NULL CHECK (evaluation_status IN
        ('PENDING', 'CORRECT', 'INCORRECT', 'IGNORED')),
    feedback_text text,
    occurred_at timestamptz NOT NULL,
    UNIQUE (session_id, action_sequence),
    FOREIGN KEY (organization_id, scenario_id, session_id)
        REFERENCES training.sessions(organization_id, scenario_id, id),
    FOREIGN KEY (organization_id, scenario_id, scenario_step_id)
        REFERENCES training.scenario_steps(organization_id, scenario_id, id)
);

CREATE TABLE training.session_results (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    session_id uuid NOT NULL,
    metric_code varchar(80) NOT NULL,
    score numeric(10,2) NOT NULL CHECK (score >= 0),
    passed boolean NOT NULL,
    detail_data jsonb,
    UNIQUE (session_id, metric_code),
    FOREIGN KEY (organization_id, session_id) REFERENCES training.sessions(organization_id, id)
);

CREATE INDEX product_lookup_name ON catalog.products(organization_id, lower(name));
CREATE INDEX batch_fefo ON inventory.product_batches(store_id, product_id, expiry_date, id);
CREATE INDEX stock_movement_history ON inventory.stock_movements(store_id, product_batch_id, occurred_at DESC);
CREATE INDEX invoice_history ON sales.invoices(store_id, sold_at DESC);
CREATE INDEX stocktake_status ON inventory.stocktakes(store_id, status, started_at DESC);
CREATE INDEX processed_operation_status ON sync.processed_operations(organization_id, status, received_at);
CREATE INDEX audit_entity_history ON audit.audit_logs(entity_type, entity_id, occurred_at DESC);
CREATE INDEX training_learner_history ON training.sessions(learner_user_id, started_at DESC);
