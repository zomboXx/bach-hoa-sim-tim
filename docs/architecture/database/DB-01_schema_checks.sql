-- Run after DB-01_schema_proposal.sql on a disposable database.
-- ON_ERROR_STOP=1 is required. This transaction rolls back all test records.
BEGIN;

DO $$
BEGIN
    IF (SELECT count(*) FROM information_schema.tables
        WHERE table_type = 'BASE TABLE'
          AND table_schema IN ('core','iam','catalog','inventory','sales','sync','audit','training')) <> 39 THEN
        RAISE EXCEPTION 'Expected 38 Word tables and one auth session table';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class child ON child.oid = c.conrelid
        JOIN pg_namespace child_ns ON child_ns.oid = child.relnamespace
        JOIN pg_class parent ON parent.oid = c.confrelid
        JOIN pg_namespace parent_ns ON parent_ns.oid = parent.relnamespace
        WHERE c.contype = 'f' AND child_ns.nspname = 'training'
          AND parent_ns.nspname IN ('inventory', 'sales')
    ) THEN
        RAISE EXCEPTION 'Training must not reference operational transactions';
    END IF;
END $$;

INSERT INTO core.organizations VALUES
    ('00000000-0000-0000-0000-000000000001', 'ORG_A', 'Organization A', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', 'ORG_B', 'Organization B', 'ACTIVE');
INSERT INTO core.stores VALUES
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'STORE_A', 'Store A', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002', 'STORE_B', 'Store B', 'ACTIVE');
INSERT INTO iam.users (id, organization_id, username, password_hash, full_name, status) VALUES
    ('00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000001', 'USER_A', 'test-only-hash', 'User A', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000002', 'USER_B', 'test-only-hash', 'User B', 'ACTIVE');
INSERT INTO iam.roles VALUES
    ('00000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000001', 'SALES', 'Sales'),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000002', 'STOCK', 'Stock');

DO $$
BEGIN
    BEGIN
        INSERT INTO iam.roles VALUES
            ('00000000-0000-0000-0000-000000000033', '00000000-0000-0000-0000-000000000001', 'LEARNER', 'Learner');
        RAISE EXCEPTION 'A fifth server role was accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO iam.user_roles VALUES
            ('00000000-0000-0000-0000-000000000001',
             '00000000-0000-0000-0000-000000000021',
             '00000000-0000-0000-0000-000000000032',
             '00000000-0000-0000-0000-000000000011', now(),
             '00000000-0000-0000-0000-000000000021');
        RAISE EXCEPTION 'Cross-tenant role assignment was accepted';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
END $$;

INSERT INTO catalog.categories VALUES
    ('00000000-0000-0000-0000-000000000041', '00000000-0000-0000-0000-000000000001', NULL, 'CAT', 'Category', 'ACTIVE');
INSERT INTO catalog.units VALUES
    ('00000000-0000-0000-0000-000000000051', '00000000-0000-0000-0000-000000000001', 'EA', 'Each', 0);
INSERT INTO catalog.products (id, organization_id, category_id, base_unit_id, sku, name, status) VALUES
    ('00000000-0000-0000-0000-000000000061', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000041', '00000000-0000-0000-0000-000000000051', 'SKU_A', 'Product A', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000062', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000041', '00000000-0000-0000-0000-000000000051', 'SKU_B', 'Product B', 'ACTIVE');
INSERT INTO catalog.product_prices VALUES
    ('00000000-0000-0000-0000-000000000071', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000061',
     10000, '2026-01-01T00:00:00Z', '2026-02-01T00:00:00Z');

DO $$
BEGIN
    BEGIN
        INSERT INTO catalog.product_prices VALUES
            ('00000000-0000-0000-0000-000000000072', '00000000-0000-0000-0000-000000000001',
             '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000061',
             12000, '2026-01-15T00:00:00Z', '2026-02-15T00:00:00Z');
        RAISE EXCEPTION 'Overlapping prices were accepted';
    EXCEPTION WHEN exclusion_violation THEN NULL;
    END;
END $$;

INSERT INTO catalog.suppliers VALUES
    ('00000000-0000-0000-0000-000000000081', '00000000-0000-0000-0000-000000000001',
     'SUP_A', 'Supplier A', NULL, NULL, 'ACTIVE');
INSERT INTO inventory.goods_receipts
    (id, organization_id, store_id, supplier_id, receipt_no, status, received_at, created_by)
VALUES ('00000000-0000-0000-0000-000000000091', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000081',
    'REC_A', 'DRAFT', now(), '00000000-0000-0000-0000-000000000021');

DO $$
BEGIN
    BEGIN
        INSERT INTO inventory.goods_receipt_lines
            (id, organization_id, store_id, receipt_id, product_id, expected_quantity,
             delivered_quantity, accepted_quantity, rejected_quantity, unit_cost)
        VALUES ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001',
            '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000091',
            '00000000-0000-0000-0000-000000000061', 10, 8, 8, 0, 5000);
        RAISE EXCEPTION 'Missing delivery reason was accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
END $$;

INSERT INTO inventory.goods_receipt_lines
    (id, organization_id, store_id, receipt_id, product_id, expected_quantity,
     delivered_quantity, accepted_quantity, rejected_quantity, unit_cost, discrepancy_reason)
VALUES ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000091',
    '00000000-0000-0000-0000-000000000061', 10, 8, 8, 0, 5000, 'Two units missing');

DO $$
BEGIN
    BEGIN
        INSERT INTO inventory.product_batches
            (id, organization_id, store_id, product_id, receipt_line_id, internal_batch_code, received_date, status)
        VALUES ('00000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000001',
            '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000062',
            '00000000-0000-0000-0000-000000000101', 'BATCH_A', '2026-01-01', 'AVAILABLE');
        RAISE EXCEPTION 'Batch product differed from its receipt line';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
END $$;

INSERT INTO inventory.product_batches
    (id, organization_id, store_id, product_id, receipt_line_id, internal_batch_code, received_date, status)
VALUES ('00000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000061',
    '00000000-0000-0000-0000-000000000101', 'BATCH_A', '2026-01-01', 'AVAILABLE');
INSERT INTO sales.invoices
    (id, organization_id, store_id, invoice_no, status, sold_by, sold_at)
VALUES ('00000000-0000-0000-0000-000000000131', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000011', 'INV_A', 'DRAFT',
    '00000000-0000-0000-0000-000000000021', now());
INSERT INTO sales.invoice_lines
    (id, organization_id, store_id, invoice_id, product_id, sku_snapshot,
     product_name_snapshot, quantity, unit_price, line_total)
VALUES ('00000000-0000-0000-0000-000000000141', '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000131',
    '00000000-0000-0000-0000-000000000062', 'SKU_B', 'Product B', 1, 0, 0);

DO $$
BEGIN
    BEGIN
        INSERT INTO sales.invoice_line_batches
            (organization_id, store_id, invoice_line_id, product_id, product_batch_id, quantity)
        VALUES ('00000000-0000-0000-0000-000000000001',
            '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000141',
            '00000000-0000-0000-0000-000000000062', '00000000-0000-0000-0000-000000000111', 1);
        RAISE EXCEPTION 'Invoice allocated a batch of another product';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO inventory.stock_movements
            (id, organization_id, store_id, product_batch_id, movement_type,
             quantity_delta, receipt_line_id, occurred_at, actor_user_id)
        VALUES ('00000000-0000-0000-0000-000000000151',
            '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000011',
            '00000000-0000-0000-0000-000000000111', 'SALE', -1,
            '00000000-0000-0000-0000-000000000101', now(),
            '00000000-0000-0000-0000-000000000021');
        RAISE EXCEPTION 'Sale movement accepted a receipt source';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
END $$;

ROLLBACK;
