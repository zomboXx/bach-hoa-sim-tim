# QA-04 — Ma trận truy vết yêu cầu, kiểm thử và bằng chứng

> **Cập nhật 20/10/2026:** sáu dòng S2-GAP-01…06 trong bảng dưới đã được
> kiểm tra lại trên mã tích hợp và chuyển thành Pass; xem
> [QA-02 closeout](QA-02-closeout-2026-10-20.md). Bảng ngày 15/10 là
> ảnh chụp lịch sử. Bảng thực tế có **28 dòng** (22 Pass, 3 Fail, 3 Blocked),
> thay vì tổng 27 ghi nhầm ở tiêu đề cũ; hiện tại **28/28 có bằng chứng Pass**.

- Trạng thái: **21 Pass, 3 Fail, 3 Blocked (Tổng cộng 27 ca truy vết)**
- Ngày cập nhật: 2026-10-15
- Chủ trì: TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`)
- Reviewer kiểm bằng chứng: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer xác nhận: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Reviewer kiểm web: TV3 — Nguyễn Văn Thi (`@thinguyen135`)
- Source test: `Qa04RegressionSecurityTest`, `Qa02E2eTest`, `StocktakeSyncApiTest`, `StocktakeApprovalApiTest`
- Lệnh chạy: `mvnw.cmd "-Dtest=vn.simtim.api.qa.Qa04RegressionSecurityTest" test`

---

## 1. Bảng ma trận truy vết tổng thể

| ID | Yêu cầu / Backlog | Contract / Thiết kế | Automated Test / Oracle | Kết quả thực tế quan sát được | Trạng thái |
|---|---|---|---|---|:---:|
| **QA04-AUT-01** | FR-AUTH-01, NFR-SEC-01 | [AUTH_SESSION_REVIEW](../../contracts/AUTH_SESSION_REVIEW.md) | `Qa04RegressionSecurityTest#securityRbacMatrix_allFourRolesEnforcePermissionsAndDenyByDefault` | Đăng nhập trả Bearer token, session trả đúng vai trò, quyền và store scope; thiếu token trả 401 | **Pass** |
| **QA04-AUT-02** | FR-AUTH-02, NFR-SEC-02 | [AUTH_SESSION_REVIEW](../../contracts/AUTH_SESSION_REVIEW.md) | `Qa04RegressionSecurityTest#securityRbacMatrix_allFourRolesEnforcePermissionsAndDenyByDefault` | 4 vai trò SALES, STOCK, MANAGER, ADMIN bị chặn 403 đối với các thao tác ngoài quyền hạn được cấp | **Pass** |
| **QA04-AUT-03** | FR-AUTH-03, NFR-SEC-03 | [AUTH_SESSION_REVIEW](../../contracts/AUTH_SESSION_REVIEW.md) | `Qa04RegressionSecurityTest#securityRbacMatrix_allFourRolesEnforcePermissionsAndDenyByDefault` | Phản hồi tồn kho cho SALES không chứa `unitCost`, `unit_cost` hay thông tin giá mua của nhà cung cấp | **Pass** |
| **QA04-AUT-04** | Scope Isolation | [AUTH_SESSION_REVIEW](../../contracts/AUTH_SESSION_REVIEW.md) | `Qa04RegressionSecurityTest#storeScopeIsolation_crossStoreOperationsAreRejected` | Yêu cầu bán hàng gửi kèm `storeId` khác với cửa hàng của session bị từ chối 422, không ghi nhận chéo | **Pass** |
| **QA04-CAT-01** | FR-CAT-01..05 | [Sprint 2 boundary](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md) | `CatalogApiTest`, demo seed | Danh mục ngành hàng, đơn vị tính EA/KG, sản phẩm và nhà cung cấp sẵn sàng cho nghiệp vụ | **Pass** |
| **QA04-REC-01** | FR-REC-01..05 | [Inventory OpenAPI](../../contracts/inventory.openapi.yaml) | `Qa04RegressionSecurityTest#fullRegression_receive_stocktakeSyncAndApprove_quoteCheckoutWithPromo_ledgerAndReportsReconcile` | Nhận hàng Apple KG và Rice EA thành công, tạo lô, lưu hạn dùng và cập nhật số dư tồn kho | **Pass** |
| **QA04-REC-02** | INV-01 Replay | [Inventory OpenAPI](../../contracts/inventory.openapi.yaml) | `Qa02E2eTest#fullyRejectedReceiptPersistsTheDiscrepancyButReplayCreatesNoStockTwice` | Gửi lặp phiếu nhận cùng clientOperationId không nhân bản phiếu và không cộng dồn tồn kho | **Pass** |
| **QA04-INV-01** | FR-INV-01..06 | [Inventory OpenAPI](../../contracts/inventory.openapi.yaml) | `InventoryReadApiTest`, `Qa04RegressionSecurityTest` | Quản lý tồn kho theo hạn dùng và phân bổ xuất kho theo nguyên tắc FEFO | **Pass** |
| **QA04-INV-02** | S2-GAP-01 (Precision EA/KG) | [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) | Kiểm tra `SaleService` & `GoodsReceiptService` | Scale tối đa 3 chữ số thập phân đã kiểm soát, nhưng chưa chặn số lượng lẻ đối với đơn vị EA | **Fail** |
| **QA04-STK-01** | FR-INV-07..09, FR-SYNC-01..04 | [SYN-02 Contract](../../contracts/SYN-02_SYNC_API_CONTRACT.md) | `Qa04RegressionSecurityTest` (Step 2), `StocktakeSyncApiTest` | Mở phiên kiểm kê, gửi số đếm thực tế có lệch tồn, xử lý xung đột phiên bản thành công | **Pass** |
| **QA04-STK-02** | INV-03 Stocktake Approval | [INV-03 Contract](../../contracts/INV-03_STOCKTAKE_APPROVAL_CONTRACT.md) | `Qa04RegressionSecurityTest` (Step 2), `StocktakeApprovalApiTest` | Quản lý duyệt kiểm kê, hệ thống sinh biến động `STOCKTAKE_ADJUSTMENT` và cập nhật lại balance | **Pass** |
| **QA04-STK-03** | INV-03 RBAC | [INV-03 Contract](../../contracts/INV-03_STOCKTAKE_APPROVAL_CONTRACT.md) | `StocktakeApprovalApiTest#approve_byStockUser_returns403Forbidden` | Nhân viên STOCK hoặc SALES gọi duyệt kiểm kê bị từ chối 403 Forbidden | **Pass** |
| **QA04-LED-01** | Sổ cái kho (Ledger Invariant) | NFR-DAT-01, ADR 0003 | `Qa04RegressionSecurityTest#fullRegression_...` | Bất biến `inventory_balances.on_hand_quantity = SUM(stock_movements.quantity_delta)` khớp 100% qua cả 3 nghiệp vụ | **Pass** |
| **QA04-SAL-01** | FR-SAL-01..04 | [SALES_INVOICE_REVIEW](../../contracts/SALES_INVOICE_REVIEW.md) | `Qa04RegressionSecurityTest` (Step 3 & 4) | Báo giá (quote) và checkout tiền mặt (CASH checkout), tính tiền thừa và ghi nhận hóa đơn | **Pass** |
| **QA04-SAL-02** | S2-GAP-02 (PRICE_CHANGED) | [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) | DTO CheckoutRequest & SaleService | Chưa có trường `expectedTotal` trong DTO checkout để từ chối khi giá thay đổi giữa quote và checkout | **Fail** |
| **QA04-SAL-03** | S2-GAP-03 (Invoice Privacy) | [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) | `Qa04RegressionSecurityTest#invoicePrivacy_documentsSalesVisibilityBehavior` | Nhân viên SALES vẫn xem được danh sách hóa đơn của nhân viên khác cùng cửa hàng | **Fail** |
| **QA04-SAL-04** | S2-GAP-04 (Checkout Idempotency) | [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) | Schema & SaleController | Bán hàng chưa có `Idempotency-Key` để xử lý retry khi mất kết nối mạng | **Blocked** |
| **QA04-SAL-05** | Rollback an toàn giỏ hàng | NFR-DAT-01 | `Qa04RegressionSecurityTest#rollbackSafety_insufficientStockRollsBackEntireCart` | Khi một mặt hàng thiếu tồn, toàn bộ giỏ hàng bị hủy (409 Conflict), không ghi nhận hóa đơn lẻ | **Pass** |
| **QA04-SAL-06** | Concurrency cạnh tranh lô | NFR-DAT-01 | `Qa04RegressionSecurityTest#concurrency_lastUnitRaceProducesOne201AndOne409` | Hai giao dịch đồng thời tranh chấp đơn vị cuối cùng cho đúng một 201 và một 409; tồn về 0 | **Pass** |
| **QA04-PRO-01** | FR-PRO-01..03 | [SALES_PROMOTIONS_REVIEW](../../contracts/SALES_PROMOTIONS_REVIEW.md) | `Qa04RegressionSecurityTest` (Step 3 & 4) | Áp dụng khuyến mãi 10% gạo, snapshot chiết khấu trên từng dòng hóa đơn và tổng tiền | **Pass** |
| **QA04-REP-01** | FR-REP-01 (Doanh thu) | [Backlog REP-01](../project/governance/BACKLOG.md) | `Qa04RegressionSecurityTest` (Step 5) | Báo cáo doanh thu API khớp chính xác với oracle SQL `SUM(grand_total)` và số lượng hóa đơn | **Pass** |
| **QA04-REP-02** | FR-REP-02..05 (Báo cáo tồn) | [Backlog REP-01](../project/governance/BACKLOG.md) | `Qa04RegressionSecurityTest` (Step 5) | Báo cáo tồn kho khớp chính xác với số dư trên các lô khả dụng | **Pass** |
| **QA04-TRN-01** | NFR-TRN-01 (Đào tạo cô lập) | [ADR 0004](../architecture/adr/0004-training-2d-web.md) | `Qa04RegressionSecurityTest#trainingIsolation_sessionCarriesFlagAndOperationalTablesRemainClean` | Cờ `trainingEnabled` hoạt động; không phát sinh bản ghi nào vào bảng hóa đơn và biến động kho vận hành | **Pass** |
| **QA04-BAC-01** | NFR-BAC-01 (Sao lưu CSDL) | [Compose runbook](../../infra/README.md) | `Qa04RegressionSecurityTest#backupAndRestore_cleanPostgresRestoresAllEntitiesAndPreservesInvariants` | Xuất bản sao lưu dữ liệu sạch bằng `pg_dump` thành công | **Pass** |
| **QA04-BAC-02** | NFR-BAC-01 (Khôi phục CSDL) | [Compose runbook](../../infra/README.md) | `Qa04RegressionSecurityTest#backupAndRestore_cleanPostgresRestoresAllEntitiesAndPreservesInvariants` | Phục hồi thành công vào CSDL PostgreSQL sạch mới hoàn toàn bằng `psql` | **Pass** |
| **QA04-BAC-03** | NFR-BAC-01 (Đối soát tính toàn vẹn) | [Compose runbook](../../infra/README.md) | `Qa04RegressionSecurityTest#backupAndRestore_cleanPostgresRestoresAllEntitiesAndPreservesInvariants` | Đối soát 100% số lượng bảng, số dư tồn, bất biến sổ cái và doanh thu sau phục hồi | **Pass** |
| **QA04-WEB-01** | S2-GAP-05 (PWA API Checkout) | [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) | `apps/web/src/App.vue` | Màn hình bán hàng web API mode vẫn dùng state mô phỏng, chưa nối API backend | **Blocked** |
| **QA04-WEB-02** | S2-GAP-06 (Live E2E nhận-bán-BC) | [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) | Browser Playwright E2E live | Chưa có suite tự động chạy live browser trên API/DB thật cho toàn luồng bán hàng | **Blocked** |

---

## 2. Tổng kết theo trạng thái

| Trạng thái | Số lượng | Tỷ lệ | Diễn giải |
|---|:---:|:---:|---|
| **Pass** | **21** | **77.8%** | Đã có code tích hợp và kiểm thử tự động đạt 100% trên PostgreSQL Testcontainers sạch. |
| **Fail** | **3** | **11.1%** | 3 khoảng trống thuộc [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34): S2-GAP-01 (precision EA), S2-GAP-02 (price changed), S2-GAP-03 (invoice privacy). |
| **Blocked** | **3** | **11.1%** | 3 khoảng trống thuộc [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34): S2-GAP-04 (checkout idempotency), S2-GAP-05 (PWA API checkout), S2-GAP-06 (live web E2E). |
| **Tổng cộng** | **27** | **100%** | Báo cáo phản ánh trung thực toàn bộ hiện trạng tích hợp của repository tại Sprint 4. |

---

## 3. Kết luận về các ngoại lệ Issue #34 và QA-03

1. **Về Issue #34 (6 GAP P0 từ Sprint 2):**
   - Không tự động đánh dấu hoàn thành (không fake Pass) cho bất kỳ GAP nào khi mã nguồn chưa xử lý.
   - 3 ca Fail (S2-GAP-01, S2-GAP-02, S2-GAP-03) và 3 ca Blocked (S2-GAP-04, S2-GAP-05, S2-GAP-06) tiếp tục được theo dõi tại Issue gốc.
   - Quyết định phạm vi release đối với các điểm này thuộc thẩm quyền của Project Owner tại mốc REL-01.

2. **Về QA-03 #47 (Kiểm kê offline và responsive):**
   - Các API nền tảng phía backend gồm SYN-02 (đồng bộ số đếm) và INV-03 (quản lý duyệt tồn kho) đã được tích hợp và kiểm chứng **Pass** 100% trong QA-04.
   - Bằng chứng trải nghiệm kiểm kê ngoại tuyến trên thiết bị di động thật và kiểm thử reconnect ngoại tuyến vẫn do TV4 phụ trách tại Issue gốc #47.
