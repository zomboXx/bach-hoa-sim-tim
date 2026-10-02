-- INV-01 permissions: receipts.read, receipts.write
-- Follows permission matrix in contracts/SPRINT_2_BOUNDARY_DRAFT.md.

INSERT INTO iam.permissions(id, code, description) VALUES
    ('30000000-0000-0000-0000-000000000001', 'receipts.read',  'Read goods receipts within session scope'),
    ('30000000-0000-0000-0000-000000000002', 'receipts.write', 'Confirm goods receipts within session scope'),
    ('30000000-0000-0000-0000-000000000003', 'inventory.read', 'Read inventory balances, batches and movements within session scope');

-- receipts.read  → STOCK, MANAGER, ADMIN
-- receipts.write → STOCK, MANAGER, ADMIN
-- inventory.read → all roles
INSERT INTO iam.role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM iam.roles r
CROSS JOIN iam.permissions p
WHERE
    (p.code = 'inventory.read')
    OR (p.code IN ('receipts.read', 'receipts.write') AND r.code IN ('STOCK', 'MANAGER', 'ADMIN'))
ON CONFLICT DO NOTHING;
