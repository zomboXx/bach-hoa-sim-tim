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

INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND (p.code='sales.read'
       OR (p.code='sales.write' AND r.code IN ('SALES','MANAGER','ADMIN')))
ON CONFLICT DO NOTHING;

-- PRO-01B: quyền khuyến mãi cho tổ chức demo (V10 đã grant toàn bộ, seed bổ sung cho profile demo).
INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND (p.code='promotions.read'
       OR (p.code='promotions.write' AND r.code IN ('MANAGER','ADMIN')))
ON CONFLICT DO NOTHING;

-- SYN-02 / INV-03: quyền kiểm kê cho tổ chức demo.
INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND (
    (p.code = 'stocktakes.read'  AND r.code IN ('STOCK', 'MANAGER', 'ADMIN'))
    OR (p.code = 'stocktakes.write' AND r.code IN ('STOCK', 'MANAGER'))
    OR (p.code = 'stocktakes.approve' AND r.code IN ('MANAGER', 'ADMIN'))
  )
ON CONFLICT DO NOTHING;


-- TRN-03: role grants are seeded after versioned migrations create the demo roles.
INSERT INTO iam.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM iam.roles r CROSS JOIN iam.permissions p
WHERE r.organization_id='10000000-0000-0000-0000-000000000001'
  AND ((p.code='training.results.read.store' AND r.code='MANAGER')
       OR (p.code='training.results.read.organization' AND r.code='ADMIN'))
ON CONFLICT DO NOTHING;

-- Ca thực hành 01, version 1. Bộ lựa chọn thuộc training, không liên kết sales/inventory.
INSERT INTO training.scenarios(id,organization_id,code,scenario_version,name,summary,status,created_at)
VALUES ('10000000-0000-0000-0000-000000000090',
        '10000000-0000-0000-0000-000000000001',
        'SCN-RETAIL-01',1,'Ca đầu tiên ở Sim Tím',
        'Chào khách, lấy hàng, thanh toán và xử lý hàng hết hạn','ACTIVE',now())
ON CONFLICT DO NOTHING;

INSERT INTO training.scenario_steps(id,organization_id,scenario_id,step_order,
    stage,action_type,target_key,instruction_text)
SELECT gen_random_uuid(),'10000000-0000-0000-0000-000000000001',
       '10000000-0000-0000-0000-000000000090',v.step_order,
       v.stage,v.action_type,v.target_key,v.instruction_text
FROM (VALUES
    (1,'greet','greet_customer','customer_linh','Xác nhận khách cần hai hộp sữa ít đường'),
    (2,'milk','pick_product','shelf_milk','Lấy đúng hai hộp sữa ít đường'),
    (3,'checkout','scan_item','pos_scanner','Quét hộp sữa thứ nhất'),
    (4,'checkout','scan_item','pos_scanner','Quét hộp sữa thứ hai'),
    (5,'checkout','tender_payment','pos_cash','Trả đúng tiền thừa và giao hóa đơn'),
    (6,'cooler','inspect_cooler','cooler_display','Tách hộp sữa chua hết hạn khỏi tủ'),
    (7,'quarantine','quarantine_product','quarantine_bin','Gắn nhãn hàng hết hạn ở khay riêng'),
    (8,'mentor','report_mentor','mentor_mai','Báo cáo lại với Mentor Mai')
) AS v(step_order,stage,action_type,target_key,instruction_text)
ON CONFLICT (scenario_id,step_order) DO NOTHING;

INSERT INTO training.scenario_choices(id,organization_id,scenario_id,scenario_step_id,
    choice_key,is_correct,feedback_text)
SELECT gen_random_uuid(),'10000000-0000-0000-0000-000000000001',
       '10000000-0000-0000-0000-000000000090',step.id,
       v.choice_key,v.is_correct,v.feedback_text
FROM (VALUES
    (1,'greet_discount',false,'Hãy hỏi nhu cầu của khách trước khi giới thiệu khuyến mãi.'),
    (1,'greet_confirm_need',true,'Đã xác nhận đúng hai hộp sữa ít đường.'),
    (1,'greet_self_service',false,'Khách đang cần hỗ trợ tìm hàng.'),
    (2,'milk_pick_2_low_sugar',true,'Đã lấy đúng loại và số lượng.'),
    (2,'milk_pick_2_sweetened',false,'Khách cần loại ít đường.'),
    (2,'milk_pick_1_low_sugar',false,'Khách cần hai hộp.'),
    (3,'scan_item_1',true,'Đã quét hộp thứ nhất.'),
    (4,'scan_item_1',false,'Mã thứ nhất đã được quét; hãy quét hộp thứ hai.'),
    (4,'scan_item_2',true,'Đã quét đủ hai hộp khác nhau.'),
    (5,'tender_2k_change',false,'Tiền thừa đúng là 3.000đ.'),
    (5,'tender_3k_receipt',true,'Đã trả đúng tiền thừa và giao hóa đơn.'),
    (5,'tender_5k_no_receipt',false,'Cần trả đúng tiền và giao hóa đơn.'),
    (6,'cooler_discount',false,'Hàng hết hạn không được tiếp tục bán.'),
    (6,'cooler_quarantine_expired',true,'Đã lấy hàng hết hạn khỏi khu bán.'),
    (6,'cooler_push_inside',false,'Cần tách hàng hết hạn khỏi tủ.'),
    (7,'quarantine_label_expired',true,'Đã tách riêng và gắn nhãn hàng hết hạn.'),
    (7,'quarantine_mixed_with_new',false,'Không để hàng hết hạn lẫn với hàng mới.'),
    (8,'mentor_full_report',true,'Đã bàn giao đầy đủ tình trạng hàng.'),
    (8,'mentor_report_nothing',false,'Cần báo lại loại hàng và vị trí đã tách.')
) AS v(step_order,choice_key,is_correct,feedback_text)
JOIN training.scenario_steps step ON step.scenario_id='10000000-0000-0000-0000-000000000090'
    AND step.step_order=v.step_order
ON CONFLICT (scenario_step_id,choice_key) DO NOTHING;

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
