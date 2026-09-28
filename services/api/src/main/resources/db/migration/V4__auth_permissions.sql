-- BE-02 permission catalog; role grants apply to existing organizations.
INSERT INTO iam.permissions(id,code,description) VALUES
 ('20000000-0000-0000-0000-000000000001','catalog.read','Read catalog within session scope'),
 ('20000000-0000-0000-0000-000000000002','catalog.write','Change catalog within session scope');

INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE p.code='catalog.read' OR (p.code='catalog.write' AND r.code IN ('STOCK','MANAGER','ADMIN'))
ON CONFLICT DO NOTHING;
