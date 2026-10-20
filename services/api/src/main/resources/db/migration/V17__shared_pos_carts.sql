-- One active cart per employee/store; separate devices authenticate independently.
CREATE TABLE sales.pos_carts (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    generation bigint NOT NULL DEFAULT 0 CHECK (generation >= 0),
    UNIQUE (organization_id, store_id, owner_id),
    FOREIGN KEY (organization_id, store_id) REFERENCES core.stores(organization_id, id),
    FOREIGN KEY (organization_id, owner_id) REFERENCES iam.users(organization_id, id)
);
CREATE TABLE sales.pos_cart_lines (
    cart_id uuid NOT NULL REFERENCES sales.pos_carts(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES catalog.products(id),
    quantity numeric(14,3) NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (cart_id, product_id)
);
-- Retain operation receipts across checkouts so a lost HTTP response can be retried safely.
CREATE TABLE sales.pos_cart_operations (
    cart_id uuid NOT NULL REFERENCES sales.pos_carts(id) ON DELETE CASCADE,
    operation_id uuid NOT NULL,
    payload_hash varchar(64) NOT NULL,
    invoice_id uuid REFERENCES sales.invoices(id),
    applied_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (cart_id, operation_id)
);
