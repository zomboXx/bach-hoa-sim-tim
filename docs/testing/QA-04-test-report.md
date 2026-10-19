# QA-04 — Báo cáo kiểm thử Regression, Bảo mật quyền và Backup/Restore

## Lượt kiểm tra tích hợp ngày 19/10/2026

Phần báo cáo bên dưới là kết quả lịch sử của TV2 trước khi TRN-02/03 tích hợp. Lượt bổ sung của TV1 ghép mã TRN-01/02/03 với QA-04 và đã tích hợp qua [PR #70](https://github.com/zomboXx/bach-hoa-sim-tim/pull/70) vào `main` tại `4038abf` ngày 19/10/2026. Chưa có bằng chứng TV4 đã review lượt tích hợp này; Issue #57 còn mở để xác nhận nghiệm thu.

| Kiểm tra | Lệnh và môi trường | Kết quả tại thời điểm ghi |
|---|---|---|
| TRN-01/02/03 trước khi ghép QA-04 | `pwsh -File scripts/verify.ps1`; Java 25, Docker Desktop 29.6.2, PostgreSQL 17 Testcontainers | Repository/links, web demo 33/33, API consumer 42/42, backend 160/160 Pass; `BUILD SUCCESS` tại `9e6dbba` |
| QA-04 và TRN-03 sau khi ghép | `pwsh -File scripts/verify.ps1` trên PostgreSQL 17 Testcontainers, Java 25, Node 25 | Root gate cục bộ tại `cda2ec0`: web demo 33/33, API consumer 42/42, backend 169/169 Pass; trong đó QA-04 9/9 và TRN-03 7/7 Pass |
| Adapter rời ca sau khi mất phản hồi | `npm run verify` trong `apps/web` sau commit `7e013c2` | Lint, format, build, web demo 33/33, API consumer 43/43 Pass; thêm ca test dùng lại key để bỏ phiên đã tạo |
| Compose mới và ca đào tạo thật | `docker compose -p simtim-qa04-20261019004215 --env-file <env riêng> -f infra/compose.demo.yml up --build -d --wait`; web `15175`, API `18081`, PostgreSQL 17 | 3/3 service Healthy; web 200, API `UP`; 2 lựa chọn sai + 8 đúng được 80 điểm/Đạt; retry cuối giữ 1 kết quả/phiên; lịch sử người học đọc được |
| Cách ly vận hành trên Compose | Tạo 1 hóa đơn CASH 22.500 VND rồi chạy thêm ca đào tạo; đối chiếu SQL trước/sau | Hóa đơn `1→1`, doanh thu `22.500→22.500`, tổng tồn `149→149`, movement `3→3` |
| Backup/restore bằng script | `backup-db.ps1` trên project nguồn; `restore-db.ps1` trên PostgreSQL của project sạch `simtim-restore-20261019005043` | Dump 138.282 byte; nguồn/đích khớp 4 user, 2 sản phẩm, 1 hóa đơn, doanh thu 22.500, 2 phiên/20 action/2 kết quả; hash hóa đơn/kết quả và Flyway 16 bản ghi khớp |
| SQL hỏng và rollback | `restore-db.ps1` với SQL tạo probe rồi cú pháp sai trên project khôi phục | Exit code `1`; bảng probe không tồn tại sau rollback |
| Kiểm tra trên mã đã tích hợp | `pwsh -File scripts/verify.ps1` trên tree trùng `main` `4038abf` (Java 25, PostgreSQL 17 Testcontainers); [CI main](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/39186532893) | Local: web demo 33/33, API consumer 43/43, backend 169/169 Pass, 0 fail/error/skip; CI main 4/4 job Pass |

**Đối chiếu số liệu:** `Qa04RegressionSecurityTest` ở commit nội dung ban đầu có 8 test; head PR #64 thêm ca restore lỗi để thành 9. Mô tả PR và nhật ký đóng góp ghi “160 test cases” cho QA-04, nhưng đó là tổng root gate cũ, không phải số case trong class QA-04. TV2 cần xác nhận/sửa bản ghi trước khi dùng làm minh chứng đóng góp. Giữ phần lịch sử bên dưới để truy vết.

- Sáu GAP tại [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) chưa tự chuyển Pass từ test nhánh QA-04. PR #53 còn mở và cần QA chạy lại sau tích hợp.
- Lệnh chạy Compose dùng env file và mật khẩu riêng nằm ngoài Git. Bản dump và log đối soát giữ cục bộ trong `.codex/`; không commit dữ liệu người dùng hoặc secret.
- TV4 kiểm bằng chứng QA, TV1 xác nhận kết luận; chưa có xác nhận review của TV4 cho lượt tích hợp. Root gate cục bộ và CI trên `main` đã đạt; không đóng #57 chỉ từ kết quả kỹ thuật này.

---

- Kết luận: **Core QA-04 gate ĐẠT — Suite hồi quy và bảo mật đạt 8/8 Pass; Sao lưu & phục hồi trên CSDL sạch (NFR-BAC-01) đạt 100%; Ma trận truy vết đạt 21 Pass, 3 Fail, 3 Blocked (ghi nhận trung thực theo Issue #34)**
- Ngày chạy: 2026-10-15 (Asia/Bangkok)
- Owner chạy test: TV2 — Nguyễn Văn Trung (`@nguyentrung01ute-ui`)
- Reviewer kiểm bằng chứng: TV4 — Lê Văn Chiến (`@VanChien11-02`)
- Reviewer xác nhận: TV1 — Nguyễn Đức Phát (`@zomboXx`)
- Reviewer kiểm web: TV3 — Nguyễn Văn Thi (`@thinguyen135`)
- Nhánh thực thi: `test/qa-04-regression-backup`
- Issue liên kết: [QA-04 #57](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57)

---

## 1. Tóm tắt kết quả

1. **Suite kiểm thử tự động QA-04 (`Qa04RegressionSecurityTest`):**
   - Đạt **8/8 tests Pass** (0 failure, 0 error, 0 skipped) trong 25.64 giây trên PostgreSQL 17.11 Testcontainers sạch.
   - Chứng minh toàn vẹn chuỗi nghiệp vụ xuyên suốt Sprints 1, 2, 3: Nhận hàng (INV-01) → Đồng bộ kiểm kê (SYN-02) → Duyệt điều chỉnh tồn (INV-03) → Báo giá & Thanh toán CASH có khuyến mãi (SAL-01, PRO-01B) → Đối soát báo cáo doanh thu và tồn kho với oracle SQL độc lập (REP-01).
   - Kiểm chứng bất biến sổ cái kho: `inventory_balances.on_hand_quantity = SUM(inventory.stock_movements.quantity_delta)` được bảo toàn nghiêm ngặt sau mọi biến động kho.
   - Thẩm tra ma trận quyền Spring Security thật trên 4 vai trò máy chủ (`SALES`, `STOCK`, `MANAGER`, `ADMIN`) và cơ chế từ chối mặc định (`401` khi thiếu token, `403` khi thiếu quyền).
   - Xác nhận che giấu dữ liệu nhạy cảm: `SALES` xem được tồn kho nhưng không thấy giá vốn/giá mua (`unitCost`).
   - Xác nhận cô lập phạm vi cửa hàng: yêu cầu thao tác chéo cửa hàng bị từ chối `422/403`.
   - Xác nhận tính cách ly đào tạo (NFR-TRN-01): cờ `trainingEnabled` hoạt động đúng, không gây ô nhiễm bảng vận hành.
   - Xác nhận an toàn giao dịch: rollback toàn bộ giỏ hàng khi một mặt hàng thiếu tồn và tuần tự hóa hai giao dịch cạnh tranh tranh chấp đơn vị tồn cuối cùng (1 thành công 201, 1 từ chối 409).

2. **Khôi phục dữ liệu trên môi trường sạch (NFR-BAC-01):**
   - Đã kiểm chứng tự động việc sao lưu bằng `pg_dump` và khôi phục bằng `psql` vào một cơ sở dữ liệu hoàn toàn mới và sạch (`simtim_clean_restore`).
   - Đối soát 100% khớp tuyệt đối các chỉ số trước và sau phục hồi:
     * Người dùng & vai trò: 100% khớp.
     * Danh mục & sản phẩm: 100% khớp.
     * Phiếu nhận, lô hàng, số dư tồn kho: 100% khớp.
     * Biến động kho (`stock_movements` gồm RECEIPT, STOCKTAKE_ADJUSTMENT, SALE): 100% khớp.
     * Hóa đơn, chi tiết phân bổ lô và thanh toán: 100% khớp.
     * Báo cáo doanh thu và bất biến sổ cái kho: `on_hand_quantity = SUM(quantity_delta)` khớp trên toàn bộ các lô.
   - Bổ sung công cụ dòng lệnh tiện ích `scripts/backup-db.ps1`, `scripts/restore-db.ps1` và runbook chi tiết tại `infra/README.md`.

3. **Hiện trạng các ngoại lệ Issue #34 và QA-03:**
   - Không tự động chuyển thành Pass khi chưa có mã nguồn sửa lỗi: ghi nhận đúng **3 Fail** (S2-GAP-01 precision EA, S2-GAP-02 PRICE_CHANGED, S2-GAP-03 invoice privacy) và **3 Blocked** (S2-GAP-04 idempotency, S2-GAP-05 PWA API checkout, S2-GAP-06 live web E2E).
   - Toàn bộ ma trận truy vết gồm 27 ca: **21 Pass, 3 Fail, 3 Blocked**.

---

## 2. Môi trường và lệnh thực thi

- **Hệ điều hành:** Windows 11; PowerShell 7 (pwsh).
- **Java:** OpenJDK 64-Bit Server VM 21+ (Java HotSpot release 21).
- **Docker:** Docker Desktop Engine 29.1.3; Testcontainers 1.21.4 (Ryuk 0.12.0).
- **Database Image:** `postgres:17-alpine`, disposable container `simtim_qa04_test` và `simtim_clean_restore`.
- **Flyway:** 15 migration entries (V1 đến V14 + repeatable migration `R__demo_seed.sql`).

### Lệnh chạy suite QA-04 độc lập:

```powershell
Push-Location services/api
.\mvnw.cmd "-Dtest=vn.simtim.api.qa.Qa04RegressionSecurityTest" test
Pop-Location
```

**Kết quả:**
```text
[INFO] Running vn.simtim.api.qa.Qa04RegressionSecurityTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 25.64 s -- in vn.simtim.api.qa.Qa04RegressionSecurityTest
[INFO] Results:
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  31.781 s
[INFO] ------------------------------------------------------------------------
```

### Lệnh chạy đồng thời toàn bộ các suite QA:

```powershell
Push-Location services/api
.\mvnw.cmd "-Dtest=vn.simtim.api.qa.*Test" test
Pop-Location
```

**Kết quả:**
```text
[INFO] Running vn.simtim.api.qa.Qa02E2eTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 35.87 s -- in vn.simtim.api.qa.Qa02E2eTest
[INFO] Running vn.simtim.api.qa.Qa04RegressionSecurityTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 17.01 s -- in vn.simtim.api.qa.Qa04RegressionSecurityTest
[INFO] Results:
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  50.116 s
[INFO] ------------------------------------------------------------------------
```

---

## 3. Chi tiết kiểm chứng các ca kiểm thử QA-04

### 3.1. Ca hồi quy tích hợp E2E (`fullRegression_...`)
- **Nhận hàng:** 10.000 KG Táo (lô có hạn dùng 30 ngày) và 20 EA Gạo. Tạo 2 phiếu nhận, 2 lô hàng, số dư ban đầu 10.000 KG và 20 EA.
- **Kiểm kê & Duyệt tồn:**
  * Nhân viên kho mở phiên kiểm kê, gửi số đếm thực tế: Táo đếm được 9.500 KG (lệch -0.500 KG so với sổ sách).
  * Quản lý duyệt điều chỉnh tồn kho. Hệ thống tạo biến động kho `STOCKTAKE_ADJUSTMENT` với `quantity_delta = -0.500`, cập nhật tồn kho Táo thành 9.500 KG.
  * Bất biến sổ cái kho: `9.500 KG = 10.000 (nhận) - 0.500 (kiểm kê)`.
- **Bán hàng có khuyến mãi:**
  * Áp dụng khuyến mãi 10% trên Gạo.
  * Giỏ hàng: 2.500 KG Táo + 2 EA Gạo.
  * Báo giá: Subtotal = 150,000 VND; Giảm giá khuyến mãi = 5,000 VND; Grand total = 145,000 VND.
  * Khách đưa 150,000 VND tiền mặt.
  * Checkout thành công: Grand total = 145,000 VND; Tiền thừa trả lại = 5,000 VND.
  * Hóa đơn COMPLETED, 1 payment 145,000 VND, 2 dòng hóa đơn, phân bổ trừ tồn kho theo FEFO.
  * Tồn kho sau bán: Táo = 9.500 - 2.500 = 7.000 KG; Gạo = 20 - 2 = 18 EA.
  * Bất biến sổ cái kho: Táo = 7.000 KG khớp `SUM(quantity_delta) = 10.000 - 0.500 - 2.500`. Gạo = 18 EA khớp `SUM(quantity_delta) = 20 - 2`.
- **Đối soát báo cáo:**
  * Báo cáo doanh thu trả về 145,000 VND và 1 hóa đơn, khớp 100% với truy vấn trực tiếp trên bảng `sales.invoices`.
  * Báo cáo tồn kho trả về Táo 7.000 KG và Gạo 18 EA, khớp 100% với số dư trong CSDL.

### 3.2. Ca ma trận phân quyền RBAC (`securityRbacMatrix_...`)
- Yêu cầu không có token: `/api/v1/auth/session`, `/api/v1/inventory/products`, `/api/v1/reports/revenue`, `/api/v1/sales/quote`, `/api/v1/sales/checkout`, `/api/v1/inventory/stocktakes` đều trả về `401 Unauthorized` (`UNAUTHENTICATED`).
- `SALES`:
  * Được phép: `GET /auth/session` (200), `GET /inventory/products` (200), `POST /sales/quote` (200), `POST /sales/checkout` (201).
  * Che giấu dữ liệu: phản hồi `/inventory/products` không chứa `unitCost`, `unit_cost` hay `purchase`.
  * Bị chặn `403 Forbidden`: `POST /inventory/receipts`, `POST /inventory/stocktakes`, `POST /inventory/stocktakes/{id}/approve`, `GET /reports/revenue`, `GET /reports/inventory`, `POST /promotions`.
- `STOCK`:
  * Được phép: `POST /inventory/stocktakes` (201), `POST /inventory/receipts` (201).
  * Bị chặn `403 Forbidden`: `POST /sales/checkout`, `POST /inventory/stocktakes/{id}/approve`, `GET /reports/revenue`, `POST /promotions`.
- `MANAGER` & `ADMIN`:
  * Được phép thực hiện toàn bộ nghiệp vụ trong cửa hàng: nhận hàng, kiểm kê, duyệt điều chỉnh tồn kho, thanh toán, xem báo cáo doanh thu/tồn kho.

### 3.3. Ca cô lập phạm vi cửa hàng (`storeScopeIsolation_...`)
- Client gửi request quote hoặc checkout kèm `storeId` khác với `storeId` của phiên đăng nhập:
- Server phát hiện sai lệch và từ chối xử lý với mã lỗi `422 Unprocessable Entity` ("Cửa hàng không thuộc phiên làm việc"), ngăn chặn rò rỉ dữ liệu hoặc thao tác chéo giữa các cửa hàng.

### 3.4. Ca kiểm chứng quyền riêng tư hóa đơn (`invoicePrivacy_...`)
- Hai nhân viên `SALES` (Sales 1 và Sales 2) cùng làm việc tại cửa hàng MAIN.
- Sales 1 tạo một hóa đơn bán hàng.
- Sales 2 gọi `GET /api/v1/sales/invoices`:
  * Hiện tại server trả về danh sách toàn bộ hóa đơn của cửa hàng, bao gồm cả hóa đơn của Sales 1.
  * Ca kiểm thử ghi nhận trung thực hành vi này để xác nhận **S2-GAP-03 tiếp tục ở trạng thái Fail**, không đánh giá Pass giả tạo.

### 3.5. Ca cô lập đào tạo (`trainingIsolation_...`)
- Phiên của người dùng đào tạo trả về `trainingEnabled: true`.
- Phiên của người dùng bình thường trả về `trainingEnabled: false`.
- Toàn bộ bảng vận hành (`sales.invoices`, `inventory.stock_movements`, `inventory.inventory_balances`) giữ nguyên số lượng bản ghi trước và sau khi thao tác.

### 3.6. Ca an toàn giao dịch (`rollbackSafety_...` & `concurrency_...`)
- Rollback: Khi giỏ hàng gồm 2 sản phẩm (1 sản phẩm đủ tồn, 1 sản phẩm thiếu tồn), request checkout trả về `409 Conflict`. CSDL không ghi nhận bất kỳ hóa đơn, payment hay movement nào; số dư cả 2 sản phẩm giữ nguyên.
- Cạnh tranh: Hai luồng thanh toán đồng thời tranh chấp đơn vị tồn cuối cùng của Táo. Kết quả nhận được đúng một mã `201 Created` và một mã `409 Conflict`. Số dư cuối cùng bằng đúng `0.000 KG`.

### 3.7. Ca sao lưu và phục hồi CSDL trên môi trường sạch (`backupAndRestore_...`)
- Thực hiện sao lưu dữ liệu giao dịch bằng `pg_dump -Fp` ra file `/tmp/backup_qa04.sql`.
- Tạo CSDL sạch mới hoàn toàn `simtim_clean_restore` bên trong PostgreSQL server.
- Khôi phục dữ liệu từ file sao lưu vào `simtim_clean_restore` bằng `psql`.
- Đối soát toàn vẹn trên CSDL được khôi phục:
  * Số lượng users, roles, categories, products, goods_receipts, product_batches, inventory_balances, stock_movements, invoices, payments: **khớp 100% với trước sao lưu**.
  * Doanh thu tích lũy: **khớp 100% với trước sao lưu**.
  * Tồn kho từng sản phẩm: **khớp 100% với trước sao lưu**.
  * Bất biến sổ cái kho: `on_hand_quantity = SUM(quantity_delta)` được duy trì trên toàn bộ các lô sau khôi phục.

---

## 4. Bảng tổng kết đối soát ngoại lệ Issue #34

| Mã | Nội dung | Kết quả QA-02 | Kết quả QA-04 | Đánh giá hiện trạng |
|---|---|:---:|:---:|---|
| **S2-GAP-01** | Ràng buộc quantity EA (nguyên) và KG (tối đa 3 số lẻ) | Fail | **Fail** | Validation scale <= 3 đã có, nhưng chưa chặn số lượng lẻ đối với đơn vị EA. |
| **S2-GAP-02** | Cơ chế `expectedTotal` và lỗi `PRICE_CHANGED` | Fail | **Fail** | DTO checkout chưa có trường `expectedTotal` để từ chối khi giá thay đổi giữa quote và checkout. |
| **S2-GAP-03** | SALES không xem hóa đơn của người khác cùng store | Fail | **Fail** | `SaleController#list` vẫn lọc theo storeId mà chưa lọc theo `soldBy` cho vai trò SALES. |
| **S2-GAP-04** | Khóa idempotency cho checkout và đối soát mất mạng | Blocked | **Blocked** | Endpoint checkout chưa hỗ trợ `Idempotency-Key` (mặc dù phiếu nhận INV-01 và kiểm kê SYN-02 đã có). |
| **S2-GAP-05** | PWA API mode nối bán hàng, checkout thật với backend | Blocked | **Blocked** | Giao diện web bán hàng API mode vẫn dùng state giả lập demo, chưa tích hợp sales API adapter. |
| **S2-GAP-06** | Live web E2E nhận–bán–báo cáo trên API/PostgreSQL thật | Blocked | **Blocked** | Chưa có suite tự động Playwright chạy live browser E2E toàn luồng nhận–bán–báo cáo trên API thật. |

---

## 5. Kết luận và khuyến nghị

1. **Kết luận:**
   - Đầu việc **[QA-04 #57](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57) đã hoàn thành đầy đủ các tiêu chí chấp nhận kỹ thuật P0**:
     * Suite kiểm thử hồi quy và ma trận quyền hạn tự động `Qa04RegressionSecurityTest` đã được bổ sung vào baseline và chạy đạt 100% trên PostgreSQL Testcontainers sạch.
     * Bất biến sổ cái kho và đối soát tài chính được chứng minh độc lập bằng các câu truy vấn oracle trực tiếp vào CSDL.
     * Khả năng sao lưu và khôi phục CSDL trên môi trường sạch (NFR-BAC-01) đã được kiểm chứng tự động và có script, runbook hướng dẫn đầy đủ.
     * Báo cáo ghi nhận trung thực 3 Fail và 3 Blocked của Issue #34 mà không làm giả kết quả Pass.

2. **Khuyến nghị cho các đầu việc tiếp theo:**
   - **DOC-01 (#58):** Sử dụng các kết quả và ma trận truy vết của QA-04 để cập nhật báo cáo tổng kết và ma trận truy vết chính thức `DOC-01-traceability-working.md`.
   - **REL-01 (#59):** Sử dụng commit đã qua kiểm thử này, kết hợp với script backup/restore và Compose runbook để chuẩn bị bản demo và kịch bản vấn đáp release candidate.
   - **Issue #34:** Bàn giao hiện trạng 6 GAP cho Project Owner quyết định việc chấp nhận rủi ro hoặc xếp lịch xử lý trước khi đóng phiên bản.
