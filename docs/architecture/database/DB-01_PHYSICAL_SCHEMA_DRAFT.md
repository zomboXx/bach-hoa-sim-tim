# DB-01 — Schema vật lý mục tiêu và đánh giá 3NF

- Trạng thái: **Nhóm đã thống nhất phương án DB-01 theo thông báo của Project Owner ngày 27/09/2026; ba ERD logic đã cập nhật, migration/test Sprint 1 đạt local và PostgreSQL 17 CI; còn chờ review code và tích hợp PR trước khi đóng Issue**.
- Ngày: 27/09/2026.
- Nguồn: Chương 4 bản làm việc `Nhom04_BaoCaoHoanChinh.docx` do Project Owner cung cấp ngày 26/09/2026; [backlog hiện hành](../../project/governance/BACKLOG.md), [quyết định REQ-01](../../project/REQ-01_DECISION_DRAFT.md), [thiết kế logic](01-thiet-ke-csdl-khai-niem.md).
- Bản DDL đầy đủ: [DB-01_schema_proposal.sql](DB-01_schema_proposal.sql). Kiểm tra: [DB-01_schema_checks.sql](DB-01_schema_checks.sql).

## Kết luận hiện trạng và phạm vi

`main` chưa có PostgreSQL schema. Nhánh API `codex/be-01-bootstrap` có Flyway V1 tạo schema `core`, V2 tạo core/IAM và V3 tạo catalog, tổng cộng **14 bảng Sprint 1** cùng dữ liệu demo theo profile. Ba ERD trong `docs/architecture/database` là sơ đồ logic đề xuất. Bản Word mô tả **38 bảng** của mô hình đích, trong đó có chức năng sau MVP. DDL Draft này thể hiện đủ 38 bảng đó và đề xuất thêm **`iam.auth_sessions`** để đăng xuất có hiệu lực phía server; tổng cộng **39 bảng**. Số bảng không đồng nghĩa 39 chức năng đã được chấp nhận.

Không chạy file DDL đích này trên database đang dùng hoặc đặt nguyên file vào Flyway: nó tạo toàn bộ schema từ đầu và bao gồm những bảng chưa đến sprint. Phần Sprint 1 đã được tách thành V2/V3 sau V1; phần còn lại sẽ có migration riêng theo sprint và review. Mỗi migration cần test từ database sạch và test nâng cấp.

Nguyễn Đức Phát duyệt trong cuộc trò chuyện ngày 27/09/2026: giữ bốn vai trò server; dùng 39 bảng làm **schema đích**, chỉ đưa phần của sprint hiện tại vào migration; trước mắt dùng tiền nguyên VND và số lượng `numeric(14,3)` để kiểm thử, ưu tiên sửa theo kết quả nghiệp vụ. Sau đó, Project Owner thông báo **nhóm đã review và hoàn toàn đồng ý** với phương án này. Đây là xác nhận do Project Owner cung cấp trong cuộc trò chuyện; chưa có đường dẫn biên bản hoặc PR review để đối chiếu độc lập. Sự thống nhất bao gồm `iam.auth_sessions` trong schema đích nhưng không tự lên lịch các chức năng sau MVP.

## Danh mục bảng theo thời điểm dự kiến

| Nhóm | Bảng trong bản đích | Thời điểm đề xuất |
|---|---|---|
| `core` (2) | `organizations`, `stores` | Sprint 1, DB-01/BE-01 |
| `iam` (6) | `users`, `auth_sessions`, `roles`, `permissions`, `user_roles`, `role_permissions` | Sprint 1, BE-02; `auth_sessions` là bổ sung đã được nhóm đồng ý trong schema đích |
| `catalog` (6) | `categories`, `units`, `products`, `product_barcodes`, `suppliers`, `product_prices` | Sprint 1, BE-03; thời điểm triển khai lịch sử giá còn chờ PRC-01 |
| `inventory` (9) | `goods_receipts`, `goods_receipt_lines`, `product_batches`, `inventory_balances`, `stock_movements`, `stocktakes`, `stocktake_lines`, `stock_disposals`, `stock_disposal_lines` | Nhận/tồn Sprint 2; kiểm kê Sprint 3; xử lý hàng hỏng/hết hạn chưa lên lịch |
| `sales` (9) | `members`, `promotions`, `promotion_products`, `promotion_batches`, `invoices`, `invoice_lines`, `invoice_line_batches`, `payments`, `loyalty_point_transactions` | Bán/khuyến mãi Sprint 2; thành viên/điểm sau MVP nếu được duyệt |
| `sync` (1) | `processed_operations` | Sprint 3, chỉ cho luồng offline đã chọn |
| `audit` (1) | `audit_logs` | Thêm theo giao dịch nhạy cảm đầu tiên và kiểm thử quyền |
| `training` (5) | `scenarios`, `scenario_steps`, `sessions`, `session_actions`, `session_results` | Sprint 4; không có FK sang giao dịch vận hành |

Trong migration Sprint 2, nếu thành viên chưa được duyệt thì `sales.invoices.member_id` và FK của nó sẽ được thêm sau, không tạo tính năng thành viên ngầm. Tương tự, cột nguồn xử lý hàng trong `inventory.stock_movements` chỉ thêm khi nghiệp vụ xử lý hàng được lên lịch. Bản DDL đích giúp thấy quan hệ cuối cùng; migration thực tế phải theo thứ tự backlog. Bảng giá phiên bản hóa đã nằm trong V3 để bảo đảm ràng buộc không chồng lấn; API thay đổi giá vẫn chờ contract/ưu tiên của PRC-01.

## Data dictionary của 14 bảng đã tạo trong Sprint 1

Tên, kiểu, `NULL`/`NOT NULL` và giá trị mặc định của từng cột nằm trong [Flyway V2](../../../services/api/src/main/resources/db/migration/V2__core_and_iam.sql) và [V3](../../../services/api/src/main/resources/db/migration/V3__catalog.sql). Bảng dưới ghi nghĩa nghiệp vụ và khóa để review; `organization_id` giới hạn dữ liệu theo tổ chức, còn `store_id` giới hạn theo cửa hàng. Thời gian dùng `timestamptz`, ID dùng UUID.

| Bảng | Dữ liệu và ý nghĩa | Khóa/ràng buộc chính |
|---|---|---|
| `core.organizations` | `code`, `name`, `status`: mã, tên và trạng thái tổ chức. | `id` PK; `code` duy nhất; trạng thái active/inactive. |
| `core.stores` | `organization_id`, `code`, `name`, `status`: cửa hàng thuộc tổ chức. | `id` PK; `(organization_id, code)` duy nhất; FK tới tổ chức. |
| `iam.users` | `username`, `password_hash`, `full_name`, `status`, `training_enabled`, `version`: thông tin đăng nhập, tên, trạng thái nhân viên, quyền vào đào tạo và phiên bản cập nhật. | `id` PK; username duy nhất trong tổ chức, không phân biệt hoa/thường; không có vai trò `LEARNER`. |
| `iam.auth_sessions` | `user_id`, `store_id`, `token_hash`, `issued_at`, `expires_at`, `revoked_at`, `last_seen_at`: phiên đăng nhập, phạm vi cửa hàng, thời hạn và thu hồi. | `id` PK; token chỉ lưu hash 32 byte; FK ghép giữ cùng tổ chức; hạn sau lúc cấp. |
| `iam.roles` | `code`, `name`: vai trò server và tên hiển thị. | `id` PK; mỗi tổ chức chỉ có một mã trong `SALES`, `STOCK`, `MANAGER`, `ADMIN`. |
| `iam.permissions` | `code`, `description`: quyền kỹ thuật dùng cho API. | `id` PK; mã quyền duy nhất toàn hệ thống. |
| `iam.user_roles` | `user_id`, `role_id`, `store_id`, `assigned_at`, `assigned_by`: gán vai trò cho nhân viên tại cửa hàng, kèm người gán và thời điểm. | PK `(user_id, role_id, store_id)`; FK ghép buộc nhân viên, vai trò, cửa hàng và người gán cùng tổ chức. |
| `iam.role_permissions` | `role_id`, `permission_id`: quyền được cấp cho một vai trò. | PK ghép; FK tới vai trò và quyền. |
| `catalog.categories` | `parent_id`, `code`, `name`, `status`: nhóm sản phẩm và nhóm cha tùy chọn. | `id` PK; mã duy nhất trong tổ chức không phân biệt hoa/thường; cha phải cùng tổ chức và không là chính nó. |
| `catalog.units` | `code`, `name`, `precision_scale`: đơn vị và số chữ số thập phân cho phép khi nhập số lượng. | `id` PK; mã duy nhất trong tổ chức; scale từ 0 đến 3. |
| `catalog.products` | `category_id`, `base_unit_id`, `sku`, `name`, `tracks_expiry`, `status`, `version`: phân loại, đơn vị cơ sở, mã, hạn dùng và phiên bản sản phẩm. | `id` PK; SKU duy nhất trong tổ chức không phân biệt hoa/thường; nhóm và đơn vị cùng tổ chức. |
| `catalog.product_barcodes` | `product_id`, `barcode`, `is_primary`: các mã vạch của sản phẩm và mã chính. | `id` PK; mã vạch duy nhất trong tổ chức; mỗi sản phẩm nhiều nhất một mã chính. |
| `catalog.suppliers` | `code`, `name`, `phone`, `email`, `status`: mã, liên hệ và trạng thái nhà cung cấp. | `id` PK; mã duy nhất trong tổ chức không phân biệt hoa/thường. |
| `catalog.product_prices` | `store_id`, `product_id`, `sale_price`, `effective_from`, `effective_to`: giá bán VND cho sản phẩm/cửa hàng trong khoảng hiệu lực `[from, to)`. | `id` PK; giá không âm; FK ghép cùng tổ chức; các khoảng giá cùng cửa hàng/sản phẩm không chồng lấn. |

`precision_scale` là quy tắc nhập cho đơn vị; V2/V3 chưa có cột số lượng giao dịch. API ở sprint sở hữu giao dịch phải chặn số lượng quá ba chữ số thập phân trước khi ghi vào `numeric(14,3)`. Nhóm đồng ý dùng kiểu tiền/số lượng hiện hành để triển khai; quy tắc làm tròn nửa VND cần được đặc tả và kiểm thử ở contract bán hàng trước migration giao dịch, vì schema hiện chưa tự tính tiền hóa đơn.

## Chuẩn hóa và các ngoại lệ có chủ đích

Các bảng thực thể dùng khóa UUID; thuộc tính mô tả phụ thuộc vào khóa của chính thực thể. Quan hệ nhiều–nhiều dùng bảng nối (`user_roles`, `role_permissions`, `promotion_products`, `promotion_batches`, `invoice_line_batches`). Mã nghiệp vụ có UNIQUE trong phạm vi tổ chức/cửa hàng. Giá được tách khỏi sản phẩm thành khoảng hiệu lực; điểm thưởng là sổ giao dịch, không nhét số dư vào thành viên. Cách chia này hướng tới 3NF và giảm phụ thuộc bắc cầu trong dữ liệu gốc.

| Dữ liệu có thể suy ra từ nơi khác | Lý do giữ và cách tránh sai lệch |
|---|---|
| `organization_id` trên bảng đã có `store_id` hoặc khóa cha | Lặp có chủ đích để FK ghép chặn truy cập chéo tổ chức/cửa hàng ngay ở CSDL. Cùng một transaction luôn ghi cả hai; FK ghép kiểm tra tính nhất quán. Đây là ngoại lệ so với 3NF nghiêm ngặt. |
| `product_id` trên lô và phân bổ lô hóa đơn | FK ghép buộc lô, dòng nhận và dòng bán cùng sản phẩm; tránh xuất nhầm lô. |
| `inventory_balances.quantity_on_hand` | Bản đọc nhanh từ sổ `stock_movements`; cập nhật cùng transaction và đối soát tổng biến động. Không coi số dư là nguồn lịch sử độc lập. |
| Tên/SKU/giá và tổng tiền chụp trên hóa đơn | Chứng từ phải giữ đúng dữ liệu tại lúc bán dù danh mục/giá thay đổi. Khi hoàn tất, dữ liệu này bất biến; API tính và kiểm thử lại tổng. |
| `stocktake_lines.difference_quantity` | PostgreSQL sinh tự động từ số đếm và số hệ thống, không cho ứng dụng ghi riêng. |
| `training.sessions.state_data`, `scenarios.fixture_data` | JSONB là trạng thái mô phỏng phiên bản hóa, không phải bản sao các bảng bán/tồn. Phiên đào tạo không có FK hoặc transaction ghi vào dữ liệu vận hành. |
| `training.session_actions.scenario_id` | FK ghép bảo đảm bước được đánh giá thuộc đúng kịch bản của phiên. |

Vì những ngoại lệ trên, không tuyên bố **toàn bộ 39 bảng đạt 3NF nghiêm ngặt**. Ưu tiên chính là ràng buộc đúng phạm vi dữ liệu, lưu lịch sử không đổi và giao dịch nhận/bán/kiểm kê nguyên tử. Các ngoại lệ phải có test đối soát; không dùng 3NF như lý do bỏ kiểm tra nghiệp vụ.

## Thay đổi cụ thể so với Chương 4 của Word

1. Chỉ có bốn mã vai trò server `SALES`, `STOCK`, `MANAGER`, `ADMIN`; `iam.users.training_enabled` là trạng thái truy cập đào tạo. Không tạo vai trò `LEARNER`.
2. Thêm `iam.auth_sessions` với hash của mã phiên, thời hạn và thời điểm thu hồi. Bảng đã nằm trong V2; BE-02 vẫn cần duyệt contract session trước khi triển khai xác thực và đăng xuất.
3. `goods_receipt_lines.expected_quantity` và `discrepancy_reason` cho phép ghi thiếu, thừa, hư hỏng bằng số lượng cùng lý do. Số chấp nhận cộng số từ chối không vượt số giao; tại bước xác nhận, service phải xử lý hết số giao hoặc từ chối chứng từ.
4. `stock_movements` dùng FK nguồn có kiểu theo dòng nhận, phân bổ lô bán, dòng kiểm kê, dòng xử lý hoặc biến động được đảo. Cách này thay `source_type/source_id` đa hình của Word để DB kiểm tra nguồn thực sự tồn tại và cùng lô/cửa hàng.
5. Khoảng hiệu lực giá dùng exclusion constraint để chặn chồng lấn ngay cả khi hai yêu cầu ghi đồng thời; cần `btree_gist` của PostgreSQL. Nếu nhóm không muốn extension, phải thiết kế khóa giao dịch và test cạnh tranh tương đương.
6. Tiền `bigint` VND và số lượng `numeric(14,3)` chỉ là **phương án hiện hành**. Project Owner đã cho phép DB-01 cân nhắc phương án khác. Nếu giữ, cần chốt làm tròn nửa VND, giới hạn JSON number an toàn và validation trước khi ép kiểu ở CSDL.
7. Username, mã loại, mã sản phẩm và mã nhà cung cấp có chỉ mục duy nhất theo chữ thường để từ chối khác biệt chữ hoa/thường; API vẫn phải chuẩn hóa khoảng trắng và thông báo lỗi trùng rõ ràng.

## Ràng buộc phải có ngoài DDL

- Xác nhận phiếu nhận: khóa chứng từ, kiểm tra ít nhất một dòng và chênh lệch; tạo lô, tăng balance và ghi movement trong cùng transaction. Không xóa chứng từ đã xác nhận.
- Hoàn tất hóa đơn: khóa balance, kiểm tra lô còn hạn, tổng lượng lô xuất bằng lượng bán, snapshot giá, tổng tiền/giảm giá/thanh toán khớp và giảm tồn nguyên tử. Hủy đã hoàn tất phải tạo biến động đảo theo đúng lô.
- Duyệt kiểm kê: so `base_version`, không tự ghi đè khi tồn thay đổi; mỗi `client_operation_id` được xử lý một lần. Duyệt và biến động tồn cùng transaction.
- RBAC: quyền được kiểm tra ở server trên từng API; `training_enabled` không cấp quyền bán/tồn. Thu hồi phiên khi đăng xuất/khóa tài khoản.
- Giá trị tiền/số lượng: API kiểm tra scale và làm tròn trước khi PostgreSQL ép kiểu, vì `numeric(14,3)` có thể tự làm tròn đầu vào nhiều chữ số thập phân.

## Bằng chứng kiểm tra và việc còn lại

Ngày 27/09/2026, DDL đích đã áp dụng thành công trên database PostgreSQL 18.6 tạm, sạch; catalog cho thấy 39 bảng. `DB-01_schema_checks.sql` chạy trong transaction rollback và kiểm tra bốn vai trò, FK chéo tổ chức, khoảng giá chồng lấn, lý do giao thiếu, sản phẩm lô khớp dòng nhận, không phân bổ lô sai sản phẩm, loại nguồn biến động và ranh giới training. Flyway V1–V3 và seed demo đã chạy trên database PostgreSQL 18.6 sạch và database đã có V1; integration test kiểm tra 14 bảng, bốn vai trò, giá không chồng lấn và health. [CI run của PR #11](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/36330835039) đã đạt cả ba job, trong đó API bootstrap dùng Testcontainers PostgreSQL 17 theo ADR 0002. CI kiểm tra **14 bảng đã migrate**, không chạy nguyên DDL đích 39 bảng; test nghiệp vụ API của các sprint sau vẫn cần thực hiện khi triển khai.

Ba ERD logic và ảnh tương ứng đã cập nhật những quan hệ MVP bị thay đổi bởi schema vật lý: phiên đăng nhập, nguồn biến động tồn có kiểu và phiên đào tạo. DDL là nguồn chi tiết cho các bảng thuộc giai đoạn sau MVP chưa vẽ trên ERD MVP. Trước khi đóng Issue theo Definition of Done của repo, [PR #11](https://github.com/zomboXx/bach-hoa-sim-tim/pull/11) cần được reviewer kiểm tra và merge; PR liên kết Issue #3/#4 và kết quả CI. Khi triển khai các API liên quan, contract còn phải đặc tả: (1) làm tròn nửa VND khi có lượng lẻ/khuyến mãi; (2) thời điểm triển khai API lịch sử giá; (3) quyền admin theo cửa hàng hay toàn tổ chức; (4) lịch của thành viên, điểm và xử lý hàng; (5) ràng buộc nào do DB và service đảm nhiệm. Những quyết định API/sprint này không ngầm thay đổi phạm vi DB-01 đã được nhóm đồng ý. Nếu test nghiệp vụ yêu cầu đổi kiểu tiền/số lượng, ghi quyết định thay thế có ngày và cập nhật DDL/ERD trước migration bị ảnh hưởng.
