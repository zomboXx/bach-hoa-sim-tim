-- Sprint 1 catalog subset of docs/architecture/database/DB-01_schema_proposal.sql.
CREATE SCHEMA catalog;

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

-- PostgreSQL 17 ships the trusted btree_gist extension.
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE catalog.product_prices ADD CONSTRAINT product_price_no_overlap
    EXCLUDE USING gist (
        store_id WITH =,
        product_id WITH =,
        tstzrange(effective_from, effective_to, '[)') WITH &&
    );
