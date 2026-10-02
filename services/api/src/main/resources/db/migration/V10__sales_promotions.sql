-- Sprint 2, PRO-01B: khuyến mãi cơ bản theo sản phẩm.
-- sales.promotion_batches thêm trong migration INV-01 khi inventory.product_batches tồn tại.
-- sales.invoices và bảng bán hàng khác thêm trong migration SAL-01 (TV3).
CREATE SCHEMA IF NOT EXISTS sales;

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
CREATE UNIQUE INDEX promotion_code_normalized ON sales.promotions(organization_id, lower(code));

-- Phạm vi sản phẩm: nếu không có dòng nào → khuyến mãi áp dụng toàn bộ sản phẩm.
CREATE TABLE sales.promotion_products (
    organization_id uuid NOT NULL,
    promotion_id uuid NOT NULL,
    product_id uuid NOT NULL,
    PRIMARY KEY (promotion_id, product_id),
    FOREIGN KEY (organization_id, promotion_id) REFERENCES sales.promotions(organization_id, id),
    FOREIGN KEY (organization_id, product_id) REFERENCES catalog.products(organization_id, id)
);

-- Quyền khuyến mãi: MANAGER/ADMIN ghi, tất cả vai trò đọc.
INSERT INTO iam.permissions(id, code, description) VALUES
    ('20000000-0000-0000-0000-000000000004', 'promotions.read',  'Read promotions within session scope'),
    ('20000000-0000-0000-0000-000000000005', 'promotions.write', 'Manage promotions within session scope');

INSERT INTO iam.role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE p.code = 'promotions.read'
   OR (p.code = 'promotions.write' AND r.code IN ('MANAGER', 'ADMIN'))
ON CONFLICT DO NOTHING;
