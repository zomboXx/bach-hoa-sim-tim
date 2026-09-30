CREATE SCHEMA IF NOT EXISTS sales;
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE IF NOT EXISTS sales.invoices (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    status varchar(16) NOT NULL,
    sold_at timestamptz NOT NULL,
    grand_total bigint NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS catalog.products (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    sku varchar(100),
    name varchar(200)
);

CREATE TABLE IF NOT EXISTS inventory.product_batches (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_id uuid NOT NULL,
    expiry_date date,
    status varchar(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory.inventory_balances (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_batch_id uuid NOT NULL,
    quantity_on_hand numeric(14,3) NOT NULL
);
