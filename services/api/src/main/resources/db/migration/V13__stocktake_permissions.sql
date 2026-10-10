-- SYN-02 permissions: stocktakes.read, stocktakes.write
-- STOCK và MANAGER có quyền ghi (mở phiên, gửi số đếm).
-- STOCK, MANAGER và ADMIN có quyền đọc.
-- Follows permission matrix in contracts/SYN-02_SYNC_API_CONTRACT.md.

INSERT INTO iam.permissions(id, code, description) VALUES
    ('30000000-0000-0000-0000-000000000004', 'stocktakes.read',
     'Read stocktake sessions and lines within session scope'),
    ('30000000-0000-0000-0000-000000000005', 'stocktakes.write',
     'Open stocktake sessions and submit counts within session scope')
ON CONFLICT DO NOTHING;

-- stocktakes.write → STOCK, MANAGER
-- stocktakes.read  → STOCK, MANAGER, ADMIN
INSERT INTO iam.role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM iam.roles r
CROSS JOIN iam.permissions p
WHERE
    (p.code = 'stocktakes.read'  AND r.code IN ('STOCK', 'MANAGER', 'ADMIN'))
    OR (p.code = 'stocktakes.write' AND r.code IN ('STOCK', 'MANAGER'))
ON CONFLICT DO NOTHING;
