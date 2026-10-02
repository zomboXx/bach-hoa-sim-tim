-- BE-xx permission catalog; role grants apply to existing organizations.
INSERT INTO iam.permissions(id,code,description) VALUES
 ('20000000-0000-0000-0000-000000000003','reports.read','Read reports within session scope');

INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE p.code='reports.read' AND r.code IN ('MANAGER','ADMIN')
ON CONFLICT DO NOTHING;
