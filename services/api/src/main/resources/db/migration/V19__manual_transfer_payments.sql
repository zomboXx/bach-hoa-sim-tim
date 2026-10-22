-- Holds are separate from physical stock and do not create movements or revenue.
ALTER TABLE inventory.inventory_balances
    ADD COLUMN reserved_quantity numeric(14,3) NOT NULL DEFAULT 0,
    ADD CONSTRAINT balance_reservation_valid CHECK (reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity);

CREATE TABLE sales.transfer_orders (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    cart_id uuid NOT NULL REFERENCES sales.pos_carts(id),
    invoice_id uuid NOT NULL UNIQUE,
    operation_id uuid NOT NULL,
    cart_version bigint NOT NULL,
    reference varchar(40) NOT NULL UNIQUE,
    amount bigint NOT NULL CHECK (amount > 0),
    status varchar(16) NOT NULL CHECK (status IN ('PENDING','COMPLETED','CANCELLED','EXPIRED')),
    bank_bin varchar(6) NOT NULL,
    account_number varchar(19) NOT NULL,
    account_name varchar(100) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    resolved_at timestamptz,
    resolved_by uuid,
    bank_reference varchar(100),
    resolution_note varchar(500),
    UNIQUE (organization_id,store_id,owner_id,operation_id),
    FOREIGN KEY (organization_id,store_id,invoice_id) REFERENCES sales.invoices(organization_id,store_id,id),
    FOREIGN KEY (organization_id,owner_id) REFERENCES iam.users(organization_id,id),
    FOREIGN KEY (organization_id,resolved_by) REFERENCES iam.users(organization_id,id),
    CHECK (expires_at > created_at),
    CHECK ((status = 'PENDING' AND resolved_at IS NULL) OR (status <> 'PENDING' AND resolved_at IS NOT NULL)),
    CHECK (status <> 'COMPLETED' OR (resolved_by IS NOT NULL AND bank_reference IS NOT NULL))
);
CREATE UNIQUE INDEX transfer_one_pending_cart ON sales.transfer_orders(cart_id) WHERE status='PENDING';
-- A bank receipt must not pay two orders in the same organization/beneficiary account.
CREATE UNIQUE INDEX transfer_bank_receipt ON sales.transfer_orders(organization_id,bank_bin,account_number,bank_reference)
    WHERE status='COMPLETED';
CREATE INDEX transfer_history ON sales.transfer_orders(organization_id,store_id,created_at DESC);
CREATE INDEX transfer_expiry ON sales.transfer_orders(expires_at) WHERE status='PENDING';
