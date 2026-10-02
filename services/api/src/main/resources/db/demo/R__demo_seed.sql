-- Only loaded with the demo profile. Never seed login credentials in a shared database.
INSERT INTO core.organizations (id, code, name, status)
VALUES ('10000000-0000-0000-0000-000000000001', 'SIMTIM', 'Bách Hóa Sim Tím', 'ACTIVE')
ON CONFLICT DO NOTHING;


INSERT INTO core.stores (id, organization_id, code, name, status)
VALUES ('10000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001', 'MAIN', 'Cửa hàng mẫu', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO iam.roles (id, organization_id, code, name) VALUES
    ('10000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'SALES', 'Nhân viên bán hàng'),
    ('10000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'STOCK', 'Nhân viên hàng hóa'),
    ('10000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'MANAGER', 'Quản lý cửa hàng'),
    ('10000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'ADMIN', 'Quản trị viên')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.categories (id, organization_id, code, name, status)
VALUES ('10000000-0000-0000-0000-000000000021',
        '10000000-0000-0000-0000-000000000001', 'GROCERY', 'Hàng tạp hóa', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.units (id, organization_id, code, name, precision_scale) VALUES
    ('10000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000001', 'EA', 'Cái', 0),
    ('10000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000001', 'KG', 'Kilôgam', 3)
ON CONFLICT DO NOTHING;

INSERT INTO catalog.products
    (id, organization_id, category_id, base_unit_id, sku, name, tracks_expiry, status) VALUES
    ('10000000-0000-0000-0000-000000000041', '10000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000031',
     'ST-RICE-01', 'Gạo gói 1 kg', false, 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000042', '10000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000032',
     'ST-APPLE-01', 'Táo cân ký', true, 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.product_barcodes (id, organization_id, product_id, barcode, is_primary)
VALUES ('10000000-0000-0000-0000-000000000051',
        '10000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000041', '8930000000001', true)
ON CONFLICT DO NOTHING;

INSERT INTO catalog.suppliers (id, organization_id, code, name, status)
VALUES ('10000000-0000-0000-0000-000000000061',
        '10000000-0000-0000-0000-000000000001', 'SUP-001', 'Nhà cung cấp mẫu', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.product_prices
    (id, organization_id, store_id, product_id, sale_price, effective_from) VALUES
    ('10000000-0000-0000-0000-000000000071', '10000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000041',
     25000, '2020-01-01T00:00:00Z'),
    ('10000000-0000-0000-0000-000000000072', '10000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000042',
     40000, '2020-01-01T00:00:00Z')
ON CONFLICT DO NOTHING;

-- Repeatable seed runs after migrations, including on an empty demo database.
INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND (
    p.code IN ('catalog.read', 'inventory.read')
    OR (p.code IN ('catalog.write', 'receipts.read', 'receipts.write') AND r.code IN ('STOCK','MANAGER','ADMIN'))
    OR (p.code = 'reports.read' AND r.code IN ('MANAGER','ADMIN'))
  )
ON CONFLICT DO NOTHING;

<<<<<<< HEAD
-- PRO-01B: quyền khuyến mãi cho tổ chức demo (V5 đã grant toàn bộ, seed bổ sung cho profile demo).
INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND (p.code='promotions.read'
       OR (p.code='promotions.write' AND r.code IN ('MANAGER','ADMIN')))
ON CONFLICT DO NOTHING;

-- PRO-01B: khuyến mãi mẫu — giảm 10% trên gạo tại cửa hàng MAIN.
INSERT INTO sales.promotions
    (id, organization_id, store_id, code, name, discount_type, discount_value,
     starts_at, ends_at, status)
VALUES
    ('10000000-0000-0000-0000-000000000081',
     '10000000-0000-0000-0000-000000000001',
     '10000000-0000-0000-0000-000000000002',
     'PROMO-RICE-10PCT', 'Giảm 10% gạo tháng 10',
     'PERCENT', 10,
     '2026-10-01T00:00:00Z', '2026-10-31T23:59:59Z',
     'ACTIVE')
ON CONFLICT DO NOTHING;

-- Phạm vi sản phẩm của khuyến mãi mẫu: chỉ áp dụng cho gạo gói 1kg.
INSERT INTO sales.promotion_products (organization_id, promotion_id, product_id)
VALUES ('10000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000081',
        '10000000-0000-0000-0000-000000000041')
ON CONFLICT DO NOTHING;
=======
INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND (p.code='sales.read'
       OR (p.code='sales.write' AND r.code IN ('SALES','MANAGER','ADMIN')))
ON CONFLICT DO NOTHING;

>>>>>>> origin/main
