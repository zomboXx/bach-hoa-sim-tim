# QA-04 — Kế hoạch kiểm thử Regression, Bảo mật quyền và Backup/Restore

- Trạng thái: **Ready for review — đã thực thi trên commit tích hợp**
- Ngày cập nhật: 2026-10-15
- Chủ trì (Owner): TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`)
- Reviewer kiểm bằng chứng: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer xác nhận: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Reviewer kiểm web: TV3 — Nguyễn Văn Thi (`@thinguyen135`)
- Issue: [QA-04 #57](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57)
- Nhánh thực hiện: `test/qa-04-regression-backup`
- Phụ thuộc: Các sprint trước (Sprint 1 nền tảng, Sprint 2 nghiệp vụ cốt lõi, Sprint 3 kiểm kê)

---

## 1. Mục tiêu và phạm vi

Mục tiêu của QA-04 là đánh giá độc lập tính ổn định tổng thể của hệ thống tại điểm tích hợp Sprint 4, bảo đảm không có hồi quy nghiệp vụ, kiểm chứng ma trận phân quyền và xác nhận khả năng sao lưu/khôi phục CSDL trên môi trường sạch.

1. **Regression xuyên suốt các module đã tích hợp (Sprints 1, 2, 3):**
   - Xác thực & Phiên (IAM): Đăng nhập, session, Bearer token ngẫu nhiên, gắn quyền theo tổ chức và cửa hàng.
   - Danh mục & Sản phẩm (Catalog): Phân loại đơn vị tính EA (`precision_scale=0`) và KG (`precision_scale=3`), quản lý sản phẩm có hạn và không hạn dùng.
   - Nhận hàng (Goods Receipt - INV-01): Nhận đủ, ghi nhận sai lệch/từ chối, tạo lô hàng với hạn dùng theo giờ Việt Nam (`Asia/Ho_Chi_Minh`), replay an toàn chống tạo trùng tồn kho.
   - Tồn kho & Lô hàng (Inventory - INV-02): Quản lý tồn khả dụng, phân bổ xuất kho theo FEFO (ưu tiên lô hết hạn sớm nhất, bỏ qua lô hết hạn hoặc bị khóa).
   - Kiểm kê offline & duyệt điều chỉnh (Stocktake - SYN-02 & INV-03): Đồng bộ số đếm từ hàng đợi ngoại tuyến, xử lý xung đột phiên bản và quản lý duyệt điều chỉnh tồn kho, ghi nhận biến động `STOCKTAKE_ADJUSTMENT`.
   - Bán hàng & Khuyến mãi (Sales & Promotions - SAL-01, PRO-01B): Báo giá (quote), thanh toán tiền mặt (CASH checkout), snapshot chương trình khuyến mãi, tính tiền thừa, trừ tồn kho nguyên tử.
   - Báo cáo tài chính & tồn kho (Reports - REP-01): Báo cáo doanh thu và báo cáo tồn kho đối soát trực tiếp với oracle SQL trong CSDL.
   - Bất biến sổ cái kho (Ledger Invariant): `inventory_balances.on_hand_quantity = SUM(inventory.stock_movements.quantity_delta)` được duy trì nghiêm ngặt qua mọi nghiệp vụ (nhận hàng, điều chỉnh kiểm kê, bán hàng).
   - An toàn giao dịch: Rollback toàn bộ giỏ hàng khi một mặt hàng thiếu tồn; tuần tự hóa giao dịch cạnh tranh tranh chấp đơn vị tồn cuối cùng.

2. **Bảo mật quyền (Authorization & RBAC Matrix):**
   - Kiểm thử chuỗi bảo mật thật Spring Security qua HTTP: kiểm tra đầy đủ 4 vai trò máy chủ `SALES`, `STOCK`, `MANAGER`, `ADMIN`.
   - Quy tắc từ chối mặc định (deny-by-default): request không có token trả về `401 Unauthorized`; vai trò không có quyền tương ứng trả về `403 Forbidden`.
   - Che giấu dữ liệu nhạy cảm (Data Masking): nhân viên `SALES` xem được tồn kho và lô hàng nhưng không được xem giá vốn/giá nhập (`unit_cost`).
   - Cô lập phạm vi cửa hàng (Store Scope Isolation): từ chối xử lý khi request gửi kèm `storeId` khác với cửa hàng trong phiên đăng nhập.
   - Độc lập đào tạo (Training Isolation - NFR-TRN-01): cờ `trainingEnabled` nằm trong phiên người dùng, không ghi dữ liệu hoặc làm thay đổi số dư vận hành (`sales.*`, `inventory.*`).

3. **Sao lưu và phục hồi dữ liệu (Backup & Restore - NFR-BAC-01):**
   - Thực hiện sao lưu dữ liệu bằng `pg_dump` trên CSDL đã phát sinh giao dịch thật.
   - Kiểm chứng lệnh và quy trình khôi phục (`psql` / `pg_restore`) trên môi trường CSDL sạch không có dữ liệu cũ.
   - Chứng minh toàn vẹn: đối soát 100% số lượng thực thể, số dư tồn kho, bất biến sổ cái và doanh thu báo cáo trước và sau khi phục hồi.

---

## 2. Nguyên tắc và tiêu chí kết luận

Tuân thủ nghiêm ngặt quy định kiểm thử và nghiệm thu của repository:
- **Pass:** Chỉ được ghi khi ca kiểm thử được chạy trực tiếp trên source tích hợp xác định và oracle đối soát đạt 100%.
- **Fail:** Ghi nhận khi hành vi thực tế khác với đặc tả yêu cầu hoặc kiểm tra chứng minh thiếu hành vi bắt buộc.
- **Blocked:** Ghi nhận khi chức năng hoặc thành phần phụ thuộc chưa được tích hợp hoặc chưa có giao diện/consumer để chạy.
- **Không làm giả kết quả (No Fake Pass):** Không đổi các ngoại lệ của [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) (S2-GAP-01..06) hoặc [QA-03 #47](https://github.com/zomboXx/bach-hoa-sim-tim/issues/47) thành `Pass` nếu mã nguồn chưa có cài đặt hoặc chưa đạt tiêu chí đóng. QA-04 ghi nhận trung thực hiện trạng để Project Owner quyết định phạm vi release.

---

## 3. Môi trường và công cụ kiểm thử

- **Hệ điều hành:** Windows, PowerShell 7 (pwsh).
- **Runtime:** Java 21+ (Java HotSpot 64-Bit), Maven Wrapper (`mvnw.cmd`).
- **Nền tảng ảo hóa:** Docker Desktop (Engine 29+, Compose v2).
- **Hệ quản trị CSDL:** PostgreSQL 17 (`postgres:17-alpine`) chạy cô lập qua Testcontainers disposable và Docker Compose demo.
- **Công cụ sao lưu/khôi phục:** `pg_dump` và `psql` tích hợp trong image PostgreSQL 17 chính thức.
- **Suite kiểm thử tự động:**
  * `vn.simtim.api.qa.Qa04RegressionSecurityTest`: Suite độc lập kiểm tra toàn diện regression, RBAC, scope, training isolation và backup/restore tự động.
  * `vn.simtim.api.qa.Qa02E2eTest`: Suite E2E hồi quy nhận–bán–báo cáo.
  * Các test provider Sprint 3: `StocktakeSyncApiTest`, `StocktakeApprovalApiTest`.
- **Scripts hỗ trợ:** `scripts/backup-db.ps1`, `scripts/restore-db.ps1`, `scripts/verify.ps1`.

---

## 4. Các nhóm ca kiểm thử chính

| Nhóm ca | Mã kiểm thử | Nội dung kiểm tra | Oracle mong đợi |
|---|---|---|---|
| **E2E Flow** | QA04-REG-01 | Nhận hàng (Apple KG + Rice EA) → Đồng bộ kiểm kê (SYN-02) → Duyệt điều chỉnh (INV-03) → Khuyến mãi & Bán hàng CASH (SAL-01, PRO-01B) → Báo cáo (REP-01) | Chuỗi nghiệp vụ thành công; tồn khả dụng và sổ cái khớp; doanh thu báo cáo khớp SUM(grand_total) |
| **Ledger** | QA04-LED-01 | Bất biến sổ cái sau mọi loại biến động (RECEIPT, STOCKTAKE_ADJUSTMENT, SALE) | `inventory_balances.on_hand_quantity = SUM(stock_movements.quantity_delta)` đúng theo từng lô |
| **RBAC** | QA04-SEC-01 | Yêu cầu không có Bearer token | HTTP `401 Unauthorized`, mã `UNAUTHENTICATED` |
| **RBAC** | QA04-SEC-02 | Quyền hạn vai trò SALES | Đọc danh mục, đọc tồn (ẩn giá vốn), quote, checkout CASH; từ chối `403` khi nhận hàng, kiểm kê, báo cáo, tạo KM |
| **RBAC** | QA04-SEC-03 | Quyền hạn vai trò STOCK | Nhận hàng, tạo phiên kiểm kê, gửi số đếm; từ chối `403` khi checkout, duyệt kiểm kê, xem báo cáo |
| **RBAC** | QA04-SEC-04 | Quyền hạn vai trò MANAGER | Được phép thực hiện toàn bộ nghiệp vụ trong cửa hàng: nhận hàng, duyệt kiểm kê, checkout, báo cáo, KM |
| **RBAC** | QA04-SEC-05 | Quyền hạn vai trò ADMIN | Toàn quyền quản trị trong phạm vi cửa hàng |
| **Data Masking** | QA04-SEC-06 | Ẩn giá vốn khi SALES xem tồn kho | Phản hồi JSON không chứa `unitCost`, `unit_cost`, `purchase` |
| **Scope** | QA04-SCO-01 | Cô lập cửa hàng: gửi `storeId` khác với phiên làm việc | Trả về lỗi `422` hoặc `403`, không ghi nhận hoặc tiết lộ dữ liệu cửa hàng khác |
| **Privacy** | QA04-PRV-01 | Nhân viên SALES xem danh sách hóa đơn cùng cửa hàng | Đối chiếu với ngoại lệ S2-GAP-03: hiện ghi nhận xem được toàn bộ hóa đơn cùng cửa hàng (Fail) |
| **Training** | QA04-TRN-01 | Cờ `trainingEnabled` trong session principal và tính cô lập bảng vận hành (NFR-TRN-01) | Session trả đúng cờ boolean; số lượng bản ghi `sales.invoices` và `inventory.stock_movements` không bị thay đổi |
| **Rollback** | QA04-TXN-01 | Thiếu tồn kho mặt hàng thứ 2 trong giỏ hàng | Trả về `409 Conflict`, rollback toàn bộ giỏ, không tạo hóa đơn, payment hay movement lẻ |
| **Concurrency** | QA04-TXN-02 | Hai giao dịch thanh toán đồng thời tranh chấp đơn vị tồn cuối cùng | Đúng một giao dịch thành công (201) và một giao dịch bị từ chối (409); tồn kho cuối cùng bằng 0 |
| **Backup/Restore** | QA04-BAC-01 | Sao lưu CSDL đã phát sinh giao dịch bằng `pg_dump` | Lệnh thoát mã 0, file dump tồn tại và dung lượng > 0 |
| **Backup/Restore** | QA04-BAC-02 | Khôi phục bản sao lưu vào CSDL PostgreSQL sạch | Lệnh thoát mã 0, tái tạo toàn bộ schema, bảng, ràng buộc và dữ liệu |
| **Backup/Restore** | QA04-BAC-03 | Đối soát tính toàn vẹn và bất biến sổ cái sau khi khôi phục | Toàn bộ số lượng bản ghi, số dư tồn, báo cáo doanh thu và bất biến `on_hand_quantity = SUM(quantity_delta)` khớp 100% với trước sao lưu |

---

## 5. Tài liệu liên quan

- [Ma trận phân quyền QA-04](QA-04-authorize.md)
- [Ma trận truy vết QA-04](QA-04-traceability.md)
- [Báo cáo kiểm thử QA-04](QA-04-test-report.md)
- [Hướng dẫn Compose Runbook & Phục hồi](../../infra/README.md)
- [Nghiệm thu Sprint 2 có ngoại lệ](../project/SPRINT_2_ACCEPTANCE_2026-10-07.md)
- [Sprint 4 Kickoff](../project/SPRINT_4_KICKOFF.md)
