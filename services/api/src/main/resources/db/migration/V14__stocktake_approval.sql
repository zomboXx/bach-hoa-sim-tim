-- INV-03: Stocktake approval, permissions and column length extension.
-- 1. Extend movement_type column length to accommodate STOCKTAKE_ADJUSTMENT (20 chars).
ALTER TABLE inventory.stock_movements
    ALTER COLUMN movement_type TYPE varchar(32);

-- 2. Add stocktakes.approve permission
INSERT INTO iam.permissions(id, code, description) VALUES
    ('30000000-0000-0000-0000-000000000006', 'stocktakes.approve',
     'Approve stocktake adjustments within session scope')
ON CONFLICT DO NOTHING;

-- 3. Grant stocktakes.approve to MANAGER and ADMIN
INSERT INTO iam.role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM iam.roles r
CROSS JOIN iam.permissions p
WHERE p.code = 'stocktakes.approve' AND r.code IN ('MANAGER', 'ADMIN')
ON CONFLICT DO NOTHING;
