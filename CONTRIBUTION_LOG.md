# Nhật ký đóng góp

Mỗi lần hoàn thành một thay đổi, thêm một mục mới ở đầu file theo mẫu sau:

```markdown
---
Tên - YYYY-MM-DD HH:mm:ss

Tóm tắt nội dung thay đổi.

- `đường/dẫn/file`: +? -?
- `đường/dẫn/file-khác`: +? -?
---
```

Nếu công việc không tạo ra diff dòng, ghi tên đầu ra và `N/A`, ví dụ:

```markdown
- `Biên bản họp ngày 2026-09-10`: N/A
```

<!-- Thêm nội dung điểm danh mới ngay dưới dòng này. -->

---
TV4 — Lê Văn Chiến - 2026-10-17 21:15:43

Sửa review PR #53 trên base `7ae6112`: API sale lấy và hiển thị quote server trước checkout, chỉ cho phép CASH, không hiển thị tồn/đơn vị giả; khi mất response, PWA khóa operation và retry đúng payload + `Idempotency-Key`. Bổ sung consumer/live browser test bấm lần hai sau commit, oracle PostgreSQL đếm độc lập invoice/payment/SALE movement và tách output live build/test để tránh lỗi file lock trên Windows. Giữ nguyên kết luận lịch sử QA-02 và chưa đánh dấu #34 Pass trước merge.

Validation: policy/Markdown/lint/format/typecheck/build Pass; demo `16/16`, API consumer `49/49`, backend `165/165`, `Qa02E2eTest` `11/11`; GAP-05 live `2/2` với `invoice_delta=2; payment_delta=2; sale_movement_delta=2; balance_mismatches=0`; GAP-06 live `1/1` với receipt/invoice/payment/SALE movement cùng tăng một và ledger khớp.

GitHub Actions [`39040328554`](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/39040328554) trên commit `9f01dc8` đạt 5/5 job, gồm `QA-02 live receive-sale-report` với oracle SALE movement mới.

- `apps/web/src/App.vue`: quote trước checkout, CASH-only và pending checkout replay
- `apps/web/tests/api-sale.spec.ts`: consumer test giá/tồn và retry cùng key/payload
- `apps/web/live-tests/qa02-sales-report.live.spec.ts`: mất response sau commit rồi bấm lại
- `apps/web/playwright.live.config.ts`, `apps/web/vite.config.ts`, `scripts/test-fe02-live.ps1`: output live cô lập và oracle trước/sau
- `scripts/qa02-gap06-db-summary.sql`: bổ sung đếm SALE movement
- `.github/workflows/ci.yml`: đọc oracle sáu cột và kiểm `sale_movement_delta=1`
- `docs/testing`: bổ sung evidence và follow-up traceability/report
- `CONTRIBUTION_LOG.md`: bổ sung mục này

---
TV4 — Lê Văn Chiến - 2026-10-17 10:30:38

Đồng bộ `origin/main` (`18f32a5`) vào branch PR #53 từ head QA-02 `3148efb`, giải quyết sáu file conflict mà vẫn giữ cả luồng bán hàng/GAP-06 của QA-02 và kiểm kê/QA-03, TRN-01 cùng hồ sơ Sprint 4 trên `main`. Loại khai báo `VN_ZONE` bị trùng do auto-merge; giữ oracle ngày bán theo múi giờ Việt Nam và fixture hạn `+8` của QA-02.

Validation trên JDK 24.0.2, Docker Desktop và PostgreSQL 17.11 disposable: policy, Markdown, lint, format, typecheck và build đạt; demo E2E báo `16/16 ok`, API consumer báo `49/49 ok`; backend Maven `165/165 Pass`, riêng `Qa02E2eTest 11/11 Pass`; live GAP-05 `2/2 Pass`; live GAP-06 chạy lại `1/1 Pass` với oracle `receipt_delta=1; invoice_delta=1; payment_delta=1; revenue_delta=40000; balance_mismatches=0`. Lần GAP-06 đầu timeout khi máy vừa chạy toàn bộ Testcontainers, lần chạy lại trên DB sạch đạt; root Playwright local đã báo đủ ca `ok` nhưng tiến trình preview Windows không tự thoát, nên dùng CI của head mới làm bằng chứng gate cuối.

- `CHANGELOG.md`: resolve conflict, giữ QA-02 và nội dung mới từ `main`
- `CONTRIBUTION_LOG.md`: resolve conflict và bổ sung mục này
- `apps/web/README.md`: resolve conflict, giữ runbook QA-02/QA-03
- `apps/web/package.json`: resolve conflict, giữ runner QA-02/QA-03
- `apps/web/src/App.vue`: resolve conflict, tích hợp sale adapter và stocktake adapter
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: resolve conflict, giữ oracle QA-02

---
TV4 — Lê Văn Chiến - 2026-10-15 17:23:43

Hoàn thành QA-03 trên branch riêng từ `origin/main` `19104cb`: nối queue kiểm kê offline của PWA với API SYN-02/INV-03, bổ sung `version`/`unitCode`, xử lý retry idempotent, conflict, quyền, duyệt và đối soát tồn/movement trên PostgreSQL; bàn giao plan, authorize matrix, traceability, report và ảnh viewport. Implementation/evidence ở commit `44b341b`; không gộp sáu ngoại lệ QA-02 của Issue #34.

Đã chạy thật `scripts/test-qa03-live.ps1` (`2/2 Pass`) và root gate `scripts/verify.ps1 -SkipInstall`: policy/link/lint/format/build Pass, web demo `14/14`, web API `39/39`, backend PostgreSQL 17.11 Testcontainers `152/152`, Maven `BUILD SUCCESS`.

- `CHANGELOG.md`: +2 -0
- `apps/web`: +547 -31
- `services/api`: +16 -5
- `scripts/test-qa03-live.ps1`: +148 -0
- `docs/testing` (Markdown): +121 -0
- `docs/testing/evidence/qa-03` (2 PNG): N/A
- `CONTRIBUTION_LOG.md`: +16 -0
TV3 — Nguyễn Văn Thi - 2026-10-15 00:25:00

Hoàn thiện scene cửa hàng 2D Vue/SVG cho hạng mục TRN-01 (#54). Tích hợp đồng thời điều khiển bàn phím (WASD/phím mũi tên/E), chuột (click-to-move BFS và click trạm/NPC) và chạm cảm ứng (Touch D-Pad tức thời/nhấn giữ và nút E tương tác). Tối ưu va chạm sliding AABB và giới hạn di chuyển trong cửa hàng. Tích hợp bộ đo hiệu năng thời gian thực perf-telemetry (FPS, Frame Time ms, Viewport). Toàn bộ dữ liệu ca học được cô lập hoàn toàn (Sandbox) trong bộ nhớ phiên, tách rời các API bán, nhận và tồn kho vận hành.

Bổ sung E2E Playwright test `tests/trn-01-training-scene.spec.ts` kiểm thử trên Desktop (1280x720) và Mobile (390x844), xác nhận không tràn ngang và khẳng định 0 request ghi vận hành. Lập báo cáo benchmark thực tế `docs/TRN-01_BENCHMARK_REPORT.md` trên máy cấu hình yếu. Toàn bộ 16/16 test Playwright, linter, typecheck, format check và build đều đạt 100%.

- `CHANGELOG.md`: +1 -0
- `apps/web/src/App.vue`: +41 -1
- `apps/web/src/training/TrainingGame.vue`: +120 -10
- `apps/web/src/training/training.css`: +54 -0
- `apps/web/tests/trn-01-training-scene.spec.ts`: +106 -0
- `docs/TRN-01_BENCHMARK_REPORT.md`: +40 -0
- `CONTRIBUTION_LOG.md`: +20 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-10-13 17:09:25

Sửa bản chuẩn bị Sprint 4 sau khi phát hiện ADR 0004 đã tồn tại ở nhánh cục bộ trước đó: dùng `0004-training-2d-web.md` làm bản quyết định duy nhất, bỏ bản ADR 0004 tạo trùng và giữ các mục nhật ký cũ để truy vết. Khởi động Sprint 4 theo sáu mã backlog, đồng bộ kickoff, Scrum, phân công và điểm vào tài liệu; giữ QA-03/#34 là việc riêng chưa nghiệm thu.

Tạo milestone Sprint 4 không hạn kết thúc, nhãn `sprint:4`, Issue #54–#59 với owner/reviewer, mức ưu tiên và tiêu chí kiểm tra; GitHub tự thêm sáu Issue vào Project `Bách Hóa Sim Tím` ở Backlog.

Root gate `pwsh -File scripts/verify.ps1 -SkipInstall` đạt sau khi khởi động Docker Desktop: repository policy/Markdown links, web demo E2E 14/14, API consumer 39/39, backend PostgreSQL Testcontainers 152/152; Maven `BUILD SUCCESS`. Lần chạy trước đó dừng ở Testcontainers do Docker Engine chưa chạy.

- `CHANGELOG.md`: +3 -3
- `README.md`: +3 -2
- `apps/web/ARCHITECTURE.md`: +1 -1
- `docs/README.md`: +2 -2
- `docs/architecture/adr/0004-training-2d-web.md`: +31 -0
- `docs/architecture/adr/0004-training-experience.md`: +0 -20
- `docs/architecture/database/01-thiet-ke-csdl-khai-niem.md`: +1 -1
- `docs/project/README.md`: +3 -3
- `docs/project/SPRINT_4_KICKOFF.md`: +12 -12
- `docs/project/governance/BACKLOG.md`: +7 -7
- `docs/project/governance/SCRUM.md`: +3 -1
- `docs/project/governance/TEAM.md`: +2 -0
- `CONTRIBUTION_LOG.md`: +24 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-10-13 05:18:00

Đối chiếu quyết định nền tảng đào tạo với ADR 0002/0003: hai ADR cũ không chốt engine, nên chuyển bản quyết định ngày 13/10 thành ADR 0004 để chỉ có một nơi ghi quyết định kiến trúc. Chuẩn bị kickoff Sprint 4 theo đúng sáu mã backlog, nêu đầu ra, thứ tự, điểm giao API/schema và quyết định còn mở; chưa mở Issue/milestone hay tự ghi Sprint 3 hoàn tất.

Root gate `pwsh -File scripts/verify.ps1` đạt: repository policy/Markdown links, web demo E2E 14/14, API consumer 39/39, backend PostgreSQL Testcontainers 152/152; Maven `BUILD SUCCESS`.

- `CHANGELOG.md`: +2 -0
- `apps/web/ARCHITECTURE.md`: +1 -1
- `docs/README.md`: +2 -1
- `docs/architecture/adr/0004-training-experience.md`: +20 -0
- `docs/architecture/database/01-thiet-ke-csdl-khai-niem.md`: +1 -1
- `docs/project/README.md`: +2 -2
- `docs/project/SPRINT_4_KICKOFF.md`: +40 -0
- `docs/project/TRAINING_PLATFORM_DECISION_2026-10-13.md`: +0 -15
- `docs/project/governance/BACKLOG.md`: +1 -1
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-10-13 04:58:30

Chuẩn bị phạm vi Sprint 4 theo quyết định bỏ Godot: ghi bản quyết định, căn lại TRN-01/02 theo game 2D Vue/SVG hiện có trong PWA, giữ mã/ưu tiên/phân công của backlog và tách rõ việc chuẩn bị với kickoff/nghiệm thu. Đồng bộ mô tả kiến trúc, CSDL, hướng dẫn web và dòng giới thiệu trong game; làm rõ nhịp review tuần trong Scrum không tự đặt hạn cho milestone. Giữ QA-03 #47 và sáu GAP P0 ở #34 là việc còn mở.

Root gate `pwsh -File scripts/verify.ps1` đạt: repository policy/Markdown links, web demo E2E 14/14, API consumer 39/39, backend PostgreSQL Testcontainers 152/152; Maven `BUILD SUCCESS`.

- `CHANGELOG.md`: +1 -0
- `apps/web/ARCHITECTURE.md`: +8 -10
- `apps/web/README.md`: +3 -3
- `apps/web/src/training/TrainingGame.vue`: +2 -2
- `docs/README.md`: +1 -0
- `docs/architecture/database/01-thiet-ke-csdl-khai-niem.md`: +1 -1
- `docs/project/README.md`: +6 -0
- `docs/project/TRAINING_PLATFORM_DECISION_2026-10-13.md`: +15 -0
- `docs/project/governance/BACKLOG.md`: +4 -2
- `docs/project/governance/SCRUM.md`: +1 -1
- `CONTRIBUTION_LOG.md`: +20 -0
---

---
Lê Văn Chiến - 2026-10-13 15:35:19

S2-GAP-06: bổ sung live E2E nhận hàng → bán CASH → hóa đơn → báo cáo và oracle PostgreSQL độc lập cho QA-02 (PR #53, Refs #34).

Test `QA02-WEB-004` đăng nhập lần lượt bằng STOCK, SALES và MANAGER trên cùng API/DB sạch; nhận một lô, bán đúng sản phẩm vừa nhận, đọc hóa đơn và đối chiếu revenue/inventory report. Runner PowerShell chờ seed hoàn tất, chụp baseline trước test rồi query trực tiếp PostgreSQL sau test để bắt buộc tăng đúng một receipt, một invoice, một payment, doanh thu dương và không có chênh lệch giữa balance với tổng movement. Workflow CI có job riêng, dựng/xóa volume Compose ở mỗi run và dùng chung SQL oracle với local runner. Hồ sơ QA ghi rõ đây là checkpoint trước merge; giữ nguyên kết quả lịch sử 17 Pass, 3 Fail, 3 Blocked cho tới khi PR merge, rerun trên `main`, có URL CI và TV1 review.

Validation: `npm run lint`, `npm run format:check`, `npm run typecheck`, PowerShell parse, repository policy, Markdown links và `git diff --check` đạt. `pwsh -File scripts/test-qa02-gap06-live.ps1` đạt 1/1; DB oracle: `receipt_delta=1; invoice_delta=1; payment_delta=1; revenue_delta=40000; balance_mismatches=0`. Root gate trên cùng provider/consumer head đạt demo 14/14, API consumer 49/49, backend 165/165 và `Qa02E2eTest` 11/11 trên PostgreSQL 17.11 Testcontainers. GitHub Actions `38370638776` đạt 5/5 job, gồm `QA-02 live receive-sale-report`.

- `.github/workflows/ci.yml`: +69 -1
- `CHANGELOG.md`: +1 -0
- `apps/web/README.md`: +2 -0
- `apps/web/live-tests/qa02-receive-sale-report.live.spec.ts`: +109 -0
- `apps/web/package.json`: +1 -0
- `docs/testing/QA-02-gap-checkpoint-2026-10-11.md`: +13 -4
- `docs/testing/QA-02-gap06-evidence-2026-10-13.md`: +41 -0
- `docs/testing/QA-02-test-report.md`: +5 -0
- `docs/testing/QA-02-traceability.md`: +6 -0
- `docs/testing/README.md`: +1 -0
- `scripts/qa02-gap06-db-summary.sql`: +35 -0
- `scripts/test-fe02-live.ps1`: +51 -2
- `scripts/test-qa02-gap06-live.ps1`: +4 -0
- `CONTRIBUTION_LOG.md`: +24 -0

---
Nguyễn Văn Thi - 2026-10-13 09:42:00

S2-GAP-05: Bán hàng trên PWA API mode (FE + contract) — nối quote, checkout CASH và xem hóa đơn qua API thật (Refs #34).

Vấn đề: PWA trước đây chỉ chạy bán hàng trên mock/demo state cục bộ, chưa tích hợp với API backend bán hàng (`/api/v1/sales/*`), chưa xử lý quote giá động, phân bổ FEFO, biến động giá `PRICE_CHANGED`, hết hàng `INSUFFICIENT_STOCK`, mất kết nối timeout và không áp dụng header `Idempotency-Key` theo S2-GAP-04.
Giải pháp:
1. Tạo module bán hàng `apps/web/src/modules/sale/` theo port/adapter: `SalePort` (domain interface), `ApiSaleAdapter` (gọi `POST /sales/quote`, `POST /sales/checkout`, `GET /sales/invoices`) và `DemoSaleAdapter` (đóng gói logic demo hiện tại trong `api.ts`, giữ cô lập và không ảnh hưởng demo mode).
2. Tích hợp `App.vue`: khi ở API mode, nạp danh mục sản phẩm từ `GET /api/v1/products`, hỗ trợ tìm kiếm/lọc danh mục. Luồng thanh toán `pay()` nguyên tử: gọi `quote()` lấy tổng tiền server tính, gọi `checkout()` với UUID `Idempotency-Key` mới và `expectedTotal`. Bắt và hiển thị thông báo thân thiện bằng tiếng Việt cho `PRICE_CHANGED`, `INSUFFICIENT_STOCK`, `IDEMPOTENCY_KEY_REUSED` và cảnh báo mất mạng (hướng dẫn kiểm tra hóa đơn trước khi thử lại).
3. Hóa đơn API mode: sau checkout tự động nạp lại hóa đơn qua `loadInvoices()` và chuyển trang; trang hóa đơn có trạng thái loading, error (nút Thử lại), empty state và dialog xem chi tiết hóa đơn `api-invoice`.
4. Phân quyền: cập nhật `routesFor()` trong `adapter.ts` cho phép truy cập route `sale` và `invoices` theo các quyền `sales.create`, `sales.write`, `sales.read.*`.
5. Kiểm thử: bổ sung 10 consumer tests trong `tests/api-sale.spec.ts` (10/10 Pass) và 2 live tests trong `live-tests/qa02-sales-report.live.spec.ts` (QA02-WEB-002, QA02-WEB-003).

Validation: `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run build` đạt; `npm run test:e2e:api` (49/49 Pass); `npm run test:e2e:demo` (14/14 Pass); repository policy và markdown links OK.

- `CHANGELOG.md`: +1 -0
- `apps/web/live-tests/qa02-sales-report.live.spec.ts`: +67 -11
- `apps/web/src/App.vue`: +259 -40
- `apps/web/src/adapter.ts`: +10 -0
- `apps/web/src/modules/sale/domain/sale.ts`: +134 -0
- `apps/web/src/modules/sale/infrastructure/api-sale-adapter.ts`: +98 -0
- `apps/web/src/modules/sale/infrastructure/demo-sale-adapter.ts`: +137 -0
- `apps/web/tests/api-sale.spec.ts`: +385 -0
- `CONTRIBUTION_LOG.md`: +25 -0

---
TV2 — nguyentrung01ute-ui - 2026-10-12 23:07:00

S2-GAP-04: Checkout idempotency — header `Idempotency-Key` trên `POST /api/v1/sales/checkout` (commit `deb94a1`, refs #53, #34).

Vấn đề: hệ thống chưa chống replay khi client gửi lại request checkout với cùng UUID key, dẫn đến tạo nhiều hóa đơn và trừ tồn nhiều lần. Thêm cột nullable `idempotency_key uuid` vào `sales.invoices` (migration V15) với unique partial index trên `(organization_id, store_id, idempotency_key) WHERE idempotency_key IS NOT NULL`. Bổ sung `findInvoiceByIdempotencyKey` vào port `SaleRepository`, JPA repository và adapter. Trong `SaleService.checkout`: nếu key đã tồn tại với cùng payload (payload fingerprint qua `changeAmount`) thì trả về invoice cũ; nếu key tồn tại với payload khác thì ném `SaleConflictException("IDEMPOTENCY_KEY_REUSED")` → HTTP 409; nếu không có key thì luồng không thay đổi (backward-compatible). Controller đọc header `Idempotency-Key` (optional UUID) và chuyển xuống service. Tốt nghiệp kịch bản `checkoutReplayWritesOneInvoiceAndRejectsKeyReuseWithAnotherPayload` vào root gate `Qa02E2eTest`; dọn `Qa02GapReproduction` — không còn GAP backend nào tồn đọng.

Validation: `test-compile` exit 0; `verify -DskipTests` exit 0. Test E2E cần Docker Desktop (Testcontainers); môi trường hiện tại không có Docker nên chưa chạy được `Qa02E2eTest` trực tiếp — kết quả CI sẽ là bằng chứng Pass chính thức theo điều kiện tại #34.

- `CHANGELOG.md`: +1 -0
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleController.java`: +2 -1
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +67 -3
- `services/api/src/main/java/vn/simtim/api/sale/domain/Invoice.java`: +13 -1
- `services/api/src/main/java/vn/simtim/api/sale/domain/SaleRepository.java`: +3 -0
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/InvoiceJpa.java`: +4 -1
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/InvoiceJpaRepository.java`: +7 -0
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/SaleRepositoryAdapter.java`: +6 -0
- `services/api/src/main/resources/db/migration/V15__sales_checkout_idempotency.sql`: +14 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +39 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02GapReproduction.java`: +3 -52
- `CONTRIBUTION_LOG.md`: +22 -0

---
Nguyễn Văn Thi - 2026-10-12 17:45:00

S2-GAP-03: Phân quyền xem hóa đơn bán hàng — SALES chỉ xem hóa đơn của chính mình (PR #53, Refs #34).

Vấn đề: hệ thống chưa phân quyền truy cập hóa đơn (`sales.invoices`) theo người bán, dẫn đến nhân viên bán hàng (`SALES`) có thể đọc danh sách và chi tiết hóa đơn do người bán khác tạo trong cùng cửa hàng. Cập nhật `SaleController` để tự động gán bộ lọc người bán `filterSellerId = principal.userId()` đối với các vai trò không có quyền xem toàn cửa hàng (như `SALES`), trong khi `MANAGER` và `ADMIN` (hoặc có `sales.read.store`) được xem toàn bộ. Trong `SaleService`, hỗ trợ lọc theo `sellerId` ở `listInvoices` và `getInvoice`; ném `SaleNotFoundException` (HTTP 404) khi truy cập hóa đơn của người bán khác hoặc hóa đơn ngoài phạm vi cửa hàng. Bổ sung method và query `findByOrganizationIdAndStoreIdAndSoldBy` trong `InvoiceJpaRepository` và `SaleRepositoryAdapter`. Tốt nghiệp kịch bản `salesCannotReadAnotherSellerInvoiceInTheSameStore` vào root gate `Qa02E2eTest`; bổ sung 2 integration tests trong `SaleApiTest` xác nhận cách ly danh sách/chi tiết giữa hai nhân viên bán hàng cùng cửa hàng và chặn truy cập xuyên cửa hàng.

Validation: compile đạt; `Qa02E2eTest` (9/9 tests Pass) và `SaleApiTest` (31/31 tests Pass) trên PostgreSQL 17 Testcontainers; `Qa02GapReproduction#gap03` Pass.

- `CHANGELOG.md`: +1 -0
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleController.java`: +10 -2
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +19 -3
- `services/api/src/main/java/vn/simtim/api/sale/domain/SaleRepository.java`: +1 -0
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/InvoiceJpaRepository.java`: +6 -0
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/SaleRepositoryAdapter.java`: +6 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +30 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02GapReproduction.java`: +3 -28
- `services/api/src/test/java/vn/simtim/api/sale/SaleApiTest.java`: +120 -1
- `CONTRIBUTION_LOG.md`: +20 -0

---
Nguyễn Văn Thi - 2026-10-12 17:25:00

S2-GAP-02: Thống nhất expectedTotal và lỗi PRICE_CHANGED giữa provider/consumer khi giá thay đổi sau quote (PR #53, Refs #34).

Vấn đề: hệ thống chưa kiểm tra biến động giá giữa thời điểm tạo quote và lúc checkout, dẫn đến nguy cơ khách hàng checkout với giá cũ hoặc hóa đơn bị tính sai so với tổng tiền đã quote. Bổ sung trường tùy chọn `expectedTotal` vào `CheckoutRequest`. Trong `SaleService.checkout`, tính toán lại tổng tiền `grandTotal` từ giá hiện hành (`catalog.product_prices`); nếu `expectedTotal` được cung cấp và khác `grandTotal`, ném `SaleConflictException` với mã lỗi `PRICE_CHANGED` trước khi thực hiện bất kỳ thao tác ghi nào (không tạo hóa đơn, thanh toán hay biến động tồn). Nâng cấp `SaleConflictException` hỗ trợ mã lỗi cụ thể để `SaleExceptionHandler` trả về HTTP 409 `PRICE_CHANGED`. Bổ sung test E2E `priceChangedAfterQuoteRejectsCheckoutAndRollsBack` vào root gate `Qa02E2eTest` xác thực rollback toàn bộ và dọn dẹp kịch bản gap02 đã tốt nghiệp trong `Qa02GapReproduction`.

Validation: compile đạt; `Qa02E2eTest` (8/8 tests Pass) và `SaleApiTest` (29/29 tests Pass) trên PostgreSQL 17 Testcontainers.

- `CHANGELOG.md`: +1 -0
- `services/api/src/main/java/vn/simtim/api/sale/api/CheckoutRequest.java`: +1 -0
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleController.java`: +1 -1
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleExceptionHandler.java`: +3 -1
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +7 -1
- `services/api/src/main/java/vn/simtim/api/sale/domain/SaleConflictException.java`: +16 -1
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +40 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02GapReproduction.java`: +5 -44
- `CONTRIBUTION_LOG.md`: +19 -0

---
Nguyễn Văn Thi - 2026-10-12 17:15:00

S2-GAP-01: Ràng buộc precision số lượng EA/KG trong quote và checkout bán hàng (PR #53, Refs #34).

Vấn đề: hệ thống chưa xác thực precision theo đơn vị tính của sản phẩm (`catalog.units.precision_scale`) trong luồng bán hàng, dẫn đến request quote và checkout với số lượng lẻ cho đơn vị nguyên EA (precision = 0) vẫn được xử lý. Bổ sung `precisionScale` vào `ProductSnapshot`, truy vấn `catalog.units.precision_scale` trong `SaleRepositoryAdapter.findProductSnapshot`, xác thực `isValidPrecision` trong `SaleService.validateItems()` cho cả `quote` và `checkout`, trả về HTTP 422 `QUANTITY_PRECISION` khi vi phạm. `SaleValidationException` hỗ trợ mang error code và được `SaleExceptionHandler` ánh xạ đúng wire code. Bổ sung 3 integration test trong `SaleApiTest` (từ chối EA lẻ ở quote và checkout kèm rollback, chấp nhận KG hợp lệ) và đưa kịch bản `fractionalEaSaleIsRejectedWithoutAnyWrite` vào root gate `Qa02E2eTest`.

Validation: `git diff --check` đạt; `powershell -File scripts/verify.ps1 -SkipInstall` đạt toàn bộ (web lint/format/build/e2e và backend Maven verify 159/159 tests Pass trên PostgreSQL 17 Testcontainers).

- `CHANGELOG.md`: +1 -1
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleExceptionHandler.java`: +2 -1
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +20 -3
- `services/api/src/main/java/vn/simtim/api/sale/domain/ProductSnapshot.java`: +5 -1
- `services/api/src/main/java/vn/simtim/api/sale/domain/SaleValidationException.java`: +15 -1
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/SaleRepositoryAdapter.java`: +7 -3
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +28 -0
- `services/api/src/test/java/vn/simtim/api/sale/SaleApiTest.java`: +58 -6
- `CONTRIBUTION_LOG.md`: +19 -0

---
TV2 — nguyentrung01ute-ui - 2026-10-12 16:32:00

S2-GAP-01: Xác thực precision số lượng EA/KG trong phiếu nhận hàng (PR #53, Refs #34).

Vấn đề: hệ thống chưa kiểm tra `catalog.units.precision_scale` khi xác nhận phiếu nhận hàng, dẫn đến số lượng lẻ của đơn vị EA (precision = 0) vẫn được ghi vào DB. Thêm port `getPrecisionScale()` vào `ProductExistenceChecker`, triển khai bằng JDBC truy vấn `catalog.units`, gọi kiểm tra trước khi ghi trong `GoodsReceiptService.validateLines()`, trả 422 `QUANTITY_PRECISION` khi vi phạm và rollback toàn bộ. Đưa kịch bản `fractionalEaReceiptIsRejectedWithoutAnyWrite` vào root gate `Qa02E2eTest`; thêm 2 provider regression test (EA reject + KG accept) vào `InventoryReceiptApiTest`; chuẩn hóa `Qa02GapReproduction.gap01` thành wrapper của helper root gate.

Validation: `git diff --check` đạt; compile không lỗi; 2 provider test mới và 1 E2E root-gate test xác nhận đúng hành vi 422 rollback và 201 KG hợp lệ.

- `CHANGELOG.md`: +1 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/InventoryExceptionHandler.java`: +1 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/GoodsReceiptService.java`: +21 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/ProductExistenceChecker.java`: +5 -1
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/JdbcProductExistenceChecker.java`: +14 -0
- `services/api/src/test/java/vn/simtim/api/inventory/InventoryReceiptApiTest.java`: +60 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +41 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02GapReproduction.java`: +0 -37

---
TV4 — Lê Văn Chiến - 2026-10-12 14:06:43

Xử lý review của TV1 trên PR #53: tách ca live sales expected-red S2-GAP-05 khỏi lệnh live FE-02 mặc định bằng tag Playwright, đồng thời thêm runner riêng tạo API/PostgreSQL disposable để tái hiện có chủ đích. Cập nhật checkpoint #34 theo owner/reviewer và thứ tự triển khai đã chốt; không đổi kết quả QA-02 lịch sử và không đánh dấu GAP nào Pass.

Validation: ESLint, Prettier và TypeScript đạt; hai PowerShell runner parse hợp lệ; Playwright `--list` xác nhận lệnh FE-02 mặc định không chọn GAP-05 và lệnh riêng chỉ chọn đúng một ca sales; live FE-02 mặc định chạy lại 2/2 Pass trên API/PostgreSQL disposable; repository policy, Markdown links và `git diff --check` đạt.

- `CHANGELOG.md`: +1 -0
- `apps/web/README.md`: +2 -0
- `apps/web/live-tests/qa02-sales-report.live.spec.ts`: +3 -1
- `apps/web/package.json`: +2 -1
- `docs/testing/QA-02-gap-checkpoint-2026-10-11.md`: +11 -8
- `docs/testing/QA-02-test-report.md`: +5 -0
- `docs/testing/QA-02-traceability.md`: +1 -1
- `scripts/test-fe02-live.ps1`: +8 -2
- `scripts/test-qa02-gap05-live.ps1`: +4 -0
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-11 15:10:14

Bắt đầu follow-up QA-02 theo quyết định của TV1 tại Issue #34: giữ sáu GAP trong một issue, không tự tạo lại issue con hoặc nhận sửa toàn bộ mã FE/BE. Bổ sung checkpoint ngày 11/10, cập nhật traceability nhưng giữ nguyên báo cáo lịch sử 17 Pass, 3 Fail, 3 Blocked; tạo bốn expected-red API specs với oracle PostgreSQL độc lập và hai live PWA specs cho sales/report.

Đã chạy expected-red backend trên PostgreSQL 17.11 Testcontainers: 9 tests gồm 5 core Pass và 4 GAP Fail đúng hành vi cần sửa, 0 error/skipped. Root gate đạt repository policy, Markdown, web lint/format/typecheck/build, demo 14/14, API consumer 39/39 và backend 152/152. Live PWA mới compile đạt nhưng chưa chạy vì cần API local cùng tài khoản/password demo do người chạy cung cấp; chưa đổi GAP nào sang Pass và PR sau khi mở phải dùng `Refs #34`.

- `CHANGELOG.md`: +1 -0
- `apps/web/live-tests/qa02-sales-report.live.spec.ts`: +66 -0
- `docs/testing/QA-02-gap-checkpoint-2026-10-11.md`: +58 -0
- `docs/testing/QA-02-test-plan.md`: +6 -0
- `docs/testing/QA-02-test-report.md`: +4 -0
- `docs/testing/QA-02-traceability.md`: +13 -6
- `docs/testing/README.md`: +1 -0
- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +21 -21
- `services/api/src/test/java/vn/simtim/api/qa/Qa02GapReproduction.java`: +177 -0
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
TV2 — Nguyễn Văn Trung - 2026-10-10 21:40:00

Hoàn thành phạm vi Issue #46 (INV-03): Duyệt điều chỉnh tồn kiểm kê.
- Migration V14: Mở rộng độ dài cột `inventory.stock_movements.movement_type` lên varchar(32) để chứa kiểu `STOCKTAKE_ADJUSTMENT` và cấp quyền `stocktakes.approve` cho vai trò MANAGER, ADMIN. Cập nhật `R__demo_seed.sql` cấp quyền cho demo org.
- Security: Cấu hình `AuthSecurity` bảo vệ route `POST /api/v1/inventory/stocktakes/*/approve` yêu cầu quyền `stocktakes.approve`.
- Entity & Port: Sửa `StockMovementJpa` (length=32), bổ sung port `StockAdjustmentPort` và adapter `StockAdjustmentAdapter` xử lý khóa bi quan `PESSIMISTIC_WRITE` balance, cập nhật số dư tồn, ghi nhận biến động `STOCKTAKE_ADJUSTMENT` và ghi `audit.audit_logs`.
- Domain & Service: Mở rộng `Stocktake` và `StocktakeLine` với helper approve/conflict, `StocktakeRepository` bổ sung `updateSessionStatus`. Triển khai `StocktakeService.approveStocktake` thực thi giao dịch nguyên tử: kiểm tra trạng thái phiên, kiểm tra phiên bản tồn kho so với `baseVersion`, tính chênh lệch delta, cập nhật balance, ghi movement và audit log; từ chối duyệt lặp (409), từ chối tồn đã đổi (409) kèm rollback không ghi một phần.
- Controller & Exception: Bổ sung endpoint `POST /api/v1/inventory/stocktakes/{id}/approve` trong `StocktakeController` và ánh xạ mã lỗi trong `StocktakeExceptionHandler`.
- Contract: Tạo hợp đồng chính thức `contracts/INV-03_STOCKTAKE_APPROVAL_CONTRACT.md`.
- Testing: Bổ sung bộ 9 provider integration tests trong `StocktakeApprovalApiTest` chạy trên PostgreSQL 17 thật qua Testcontainers, kiểm tra tăng tồn, giảm tồn, count bằng tồn, thiếu quyền, duyệt lặp, tồn đổi, cross-store isolation, rollback nguyên tử và đối soát balance với ledger độc lập `SUM(quantity_delta)`. Toàn bộ 148 tests của module API pass 100%.

- `CHANGELOG.md`: +1 -0
- `contracts/INV-03_STOCKTAKE_APPROVAL_CONTRACT.md`: +123 -0
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +2 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeController.java`: +18 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeExceptionHandler.java`: +7 -5
- `services/api/src/main/java/vn/simtim/api/inventory/application/InventoryReadService.java`: +1 -1
- `services/api/src/main/java/vn/simtim/api/inventory/application/StockAdjustmentPort.java`: +28 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/StocktakeService.java`: +99 -3
- `services/api/src/main/java/vn/simtim/api/inventory/domain/Stocktake.java`: +6 -0
- `services/api/src/main/java/vn/simtim/api/inventory/domain/StocktakeLine.java`: +13 -1
- `services/api/src/main/java/vn/simtim/api/inventory/domain/StocktakeRepository.java`: +5 -1
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StockAdjustmentAdapter.java`: +73 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StockMovementJpa.java`: +1 -1
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StocktakeRepositoryAdapter.java`: +13 -0
- `services/api/src/main/resources/db/demo/R__demo_seed.sql`: +3 -1
- `services/api/src/main/resources/db/migration/V14__stocktake_approval.sql`: +18 -0
- `services/api/src/test/java/vn/simtim/api/inventory/StocktakeApprovalApiTest.java`: +585 -0
---

---
TV2 — Nguyễn Văn Trung - 2026-10-10 09:57:44

Hoàn thành phạm vi Issue #45 (SYN-02): Triển khai API đồng bộ kiểm kê và xử lý xung đột.
- Migration V12: Tạo bảng `stocktakes`, `stocktake_lines`, `processed_operations` và mở rộng CHECK `stock_movements`.
- Migration V13: Thêm quyền `stocktakes.read/write` cho STOCK/MANAGER/ADMIN.
- Domain: Bổ sung `Stocktake`, `StocktakeLine`, `StocktakeRepository`, `StocktakeException`.
- Application: `StocktakeService` hỗ trợ open/get session, `submitCount`, `InventoryBalanceReader` port, `StocktakeConflictException`.
- Infrastructure: JPA entities và `StocktakeRepositoryAdapter` implement `StocktakeRepository` và `InventoryBalanceReader`.
- API: Cung cấp 3 route `POST /stocktakes`, `GET /stocktakes/{id}`, `POST /stocktakes/{id}/counts`; cấu hình `StocktakeExceptionHandler` trả về 409 CONFLICT kèm thông tin chi tiết. Đảm bảo idempotency theo `clientOperationId` trong phạm vi org/store và conflict detection bằng `inventory_balances.version`.
- Contract: Bổ sung và đánh dấu Accepted cho `contracts/SYN-02_SYNC_API_CONTRACT.md`.
- Tests: Đạt 10 cases trong `StocktakeSyncApiTest` (compile ok).

- `.idea/workspace.xml`: +9 -7
- `CHANGELOG.md`: +1 -0
- `contracts/SYN-02_SYNC_API_CONTRACT.md`: +191 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeController.java`: +110 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeCountRequest.java`: +27 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeExceptionHandler.java`: +64 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeForbiddenException.java`: +6 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/StocktakeResponse.java`: +56 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/InventoryBalanceReader.java`: +14 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/StocktakeConflictException.java`: +20 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/StocktakeService.java`: +177 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/SubmitCountCommand.java`: +18 -0
- `services/api/src/main/java/vn/simtim/api/inventory/domain/Stocktake.java`: +29 -0
- `services/api/src/main/java/vn/simtim/api/inventory/domain/StocktakeException.java`: +14 -0
- `services/api/src/main/java/vn/simtim/api/inventory/domain/StocktakeLine.java`: +25 -0
- `services/api/src/main/java/vn/simtim/api/inventory/domain/StocktakeRepository.java`: +33 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/InventoryBalanceJpaRepository.java`: +7 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StocktakeJpa.java`: +20 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StocktakeJpaRepository.java`: +25 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StocktakeLineJpa.java`: +30 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StocktakeLineJpaRepository.java`: +20 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/StocktakeRepositoryAdapter.java`: +137 -0
- `services/api/src/main/resources/db/migration/V12__stocktake_sync.sql`: +81 -0
- `services/api/src/main/resources/db/migration/V13__stocktake_permissions.sql`: +22 -0
- `services/api/src/test/java/vn/simtim/api/inventory/StocktakeSyncApiTest.java`: +324 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-10 09:07:19

Hoàn thành phạm vi Issue #44 (SYN-01): bổ sung hàng đợi IndexedDB chỉ dành cho thao tác kiểm kê offline, giữ nguyên `clientOperationId` qua reload và retry, hiển thị trạng thái chờ/xung đột, đồng thời chặn gửi lại khi session, actor, tổ chức, cửa hàng hoặc quyền không còn khớp. Không áp dụng queue cho bán hàng/nhận hàng; API write vẫn được khóa chờ contract provider SYN-02 và đã ghi rõ điểm giao review.

Kiểm tra lint, format, typecheck, build và toàn bộ root gate đạt; Playwright demo đạt 14/14, API consumer đạt 39/39 và backend đạt 129/129. Kịch bản browser đã bao phủ mất mạng → nhập → reload → reconnect, retry, đổi tài khoản/cửa hàng, chống gửi trùng và không ghi đè conflict.

- `CHANGELOG.md`: +1 -0
- `apps/web/README.md`: +8 -0
- `apps/web/src/App.vue`: +172 -42
- `apps/web/src/api.ts`: +1 -0
- `apps/web/src/modules/inventory/domain/stocktake.ts`: +79 -0
- `apps/web/src/modules/inventory/infrastructure/stocktake-operation-queue.ts`: +93 -0
- `apps/web/src/modules/inventory/presentation/StocktakeWorkspace.vue`: +19 -1
- `apps/web/tests/stocktake-offline-queue.spec.ts`: +170 -0
- `contracts/README.md`: +1 -0
- `contracts/SYN-01_OFFLINE_QUEUE_REVIEW.md`: +54 -0
- `CONTRIBUTION_LOG.md`: +20 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-09 17:36:11

Hoàn thành phạm vi Issue #43 (MOB-01): bổ sung workspace kiểm kê responsive theo cửa hàng và lô, hiển thị số đếm, trạng thái lưu, lỗi theo dòng; hỗ trợ nhập theo lô, kiểm tra precision theo đơn vị (KG tối đa 3 chữ số thập phân), trạng thái loading/lỗi quyền và khóa thao tác ghi trong API mode cho tới khi contract provider SYN-02 được chốt. Tách domain stocktake, nối dữ liệu đọc INV-02, bổ sung tài liệu consumer review và cập nhật hướng dẫn/changelog.

Kiểm tra policy, liên kết Markdown, lint, format, typecheck và build đạt; Playwright demo đạt 10/10, API consumer đạt 39/39 trên các viewport đã công bố, gồm dữ liệu đúng/sai, loading và lỗi quyền. Root gate đã chạy tới backend nhưng Testcontainers không thể khởi tạo vì Docker Desktop daemon chưa hoạt động; chưa ghi nhận bằng chứng provider/live write API và giữ trạng thái chờ SYN-02 đúng ranh giới contract.

- `CHANGELOG.md`: +2 -0
- `apps/web/README.md`: +8 -0
- `apps/web/src/App.vue`: +76 -128
- `apps/web/src/adapter.ts`: +1 -1
- `apps/web/src/api.ts`: +10 -0
- `apps/web/src/modules/inventory/domain/inventory.ts`: +1 -0
- `apps/web/src/modules/inventory/domain/stocktake.ts`: +53 -0
- `apps/web/src/modules/inventory/infrastructure/demo-inventory-adapter.ts`: +1 -0
- `apps/web/src/modules/inventory/presentation/StocktakeWorkspace.vue`: +887 -0
- `apps/web/src/style.css`: +0 -55
- `apps/web/tests/api-stocktake.spec.ts`: +151 -0
- `apps/web/tests/stocktake-responsive.spec.ts`: +83 -0
- `contracts/MOB-01_STOCKTAKE_CONSUMER_REVIEW.md`: +40 -0
- `contracts/README.md`: +1 -0
- `CONTRIBUTION_LOG.md`: +24 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-10-08 18:41:02

Điều phối khởi động Sprint 3 theo năm mã đã chốt trong backlog: MOB-01 #43, SYN-01 #44, SYN-02 #45, INV-03 #46 và QA-03 #47. Gán owner, reviewer, nhãn P1/Sprint 3, milestone không có ngày kết thúc; giữ sáu ngoại lệ QA-02 ở #34. Viết kickoff và đồng bộ điểm vào tài liệu, phân công, backlog; phương án sáu Issue sai trước đó được lưu là lịch sử và không đưa vào cam kết mới.

- `CHANGELOG.md`: +1 -0
- `docs/README.md`: +2 -1
- `docs/project/README.md`: +8 -2
- `docs/project/SPRINT_3_KICKOFF.md`: +63 -0
- `docs/project/governance/BACKLOG.md`: +7 -5
- `docs/project/governance/TEAM.md`: +2 -0
- `CONTRIBUTION_LOG.md`: +14 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-10-07 23:13:01

Đối chiếu kết quả Sprint 2 sau khi các PR đã tích hợp: ghi quyết định nghiệm thu có ngoại lệ và mở Issue #34 theo dõi 3 Fail, 3 Blocked của QA-02, chưa phân công Sprint 3. Cập nhật README bắt đầu nhanh và hướng dẫn chạy demo web/API/PostgreSQL bằng một lệnh Docker Compose, tài khoản server, kịch bản nhận/tồn/báo cáo và checkout API; CI kiểm tra Compose trên PR. Đồng bộ các điểm vào tài liệu còn ghi trạng thái Sprint 1/2 cũ.

Kiểm tra Compose config đạt; build hai image và khởi động ba service healthy. Smoke test qua web proxy: login MANAGER, quote 22.500 VND, checkout một hóa đơn 22.500 VND và báo cáo ghi doanh thu 22.500 VND, một hóa đơn. Root gate `pwsh -File scripts/verify.ps1 -SkipInstall` đạt policy/links, lint/format/typecheck/build, demo E2E 6/6, API consumer 36/36 và backend 129/129 trên PostgreSQL 17 Testcontainers. Dữ liệu demo và mật khẩu local nằm trong file bị ignore và named volume, không thuộc commit.

- `.github/workflows/ci.yml`: +26 -0
- `CHANGELOG.md`: +9 -0
- `README.md`: +38 -31
- `apps/web/.dockerignore`: +6 -0
- `apps/web/Dockerfile.demo`: +23 -0
- `apps/web/README.md`: +2 -2
- `apps/web/nginx.demo.conf`: +18 -0
- `docs/README.md`: +3 -2
- `docs/project/README.md`: +8 -2
- `docs/project/SPRINT_2_ACCEPTANCE_2026-10-07.md`: +22 -0
- `docs/project/governance/BACKLOG.md`: +1 -1
- `docs/testing/README.md`: +1 -1
- `infra/.env.example`: +4 -1
- `infra/README.md`: +75 -6
- `infra/compose.demo.yml`: +49 -0
- `services/api/.dockerignore`: +4 -0
- `services/api/Dockerfile.demo`: +18 -0
- `services/api/README.md`: +4 -4
- `CONTRIBUTION_LOG.md`: +28 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-07 14:53:55

Xử lý hai nhận xét independent review của TV1 trên PR #33 cho QA-02: bỏ hoàn toàn nhánh `SIMTIM_TEST_DB_URL` để suite chỉ có thể chạy trên PostgreSQL Testcontainers disposable; nâng oracle doanh thu thành một hóa đơn hai dòng APPLE KG + RICE EA, assert hai invoice lines/hai allocation và đối chiếu `SUM(grand_total)` cùng `COUNT(*)` trực tiếp trên `sales.invoices`. Cập nhật test plan, traceability và report để ghi đúng reviewer, evidence, hai lần lỗi khởi tạo môi trường không tới assertion, quy tắc dùng `Refs #24` và việc các gap P0 còn lại phải giao đúng owner.

Chạy lại `Qa02E2eTest` đạt 5/5 và root gate `pwsh -File scripts/verify.ps1 -SkipInstall` đạt policy/links, web lint/format/typecheck/build, demo E2E 6/6, API consumer 36/36 và backend 129/129 trên PostgreSQL 17.11 Testcontainers. Kết luận tổng thể vẫn là 17 Pass, 3 Fail, 3 Blocked; chưa đóng Issue #24.

- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +29 -26
- `docs/testing/QA-02-test-plan.md`: +1 -1
- `docs/testing/QA-02-traceability.md`: +4 -4
- `docs/testing/QA-02-test-report.md`: +16 -8
- `docs/testing/README.md`: +1 -1
- `CHANGELOG.md`: +1 -1
- `CONTRIBUTION_LOG.md`: +16 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-07 10:32:07

Thực hiện issue [QA-02] E2E luồng nhận–bán (#24). Bổ sung suite `Qa02E2eTest` chạy qua HTTP thật và PostgreSQL 17 Testcontainers sạch, kiểm chứng nhận đủ/từ chối/replay, CASH checkout–hóa đơn–báo cáo, rollback giỏ nhiều sản phẩm, hai checkout cạnh tranh đơn vị cuối, RBAC bốn vai trò và đối chiếu độc lập `balance = SUM(movement)` cùng doanh thu `SUM(invoice)` một lần. Suite mới đã phát hiện query báo cáo tồn REP-01 lệch migration; sửa tên cột/khóa/trạng thái thành `on_hand_quantity`, `batch_id`, `EXHAUSTED` và ràng buộc join organization/store/product. Đồng thời xóa import trùng trong `App.vue` do merge để root gate chạy được.

Hoàn thiện test plan, ma trận quyền, ma trận backlog–contract–test–evidence và test report. Chạy đúng `pwsh -File scripts/verify.ps1` đạt policy/links, web lint/format/typecheck/build, demo E2E 6/6, API-consumer E2E 36/36 và backend 129/129 trên PostgreSQL 17.11; riêng QA-02 đạt 5/5. Báo cáo giữ kết luận chưa nghiệm thu P0 với 17 Pass, 3 Fail và 3 Blocked do còn unit precision EA/KG, `PRICE_CHANGED`, invoice privacy SALES, checkout idempotency và live PWA sales E2E; việc TV4 tự chạy không thay thế review độc lập của TV1. `npm audit` còn một cảnh báo dependency mức high và được ghi riêng, không bị che bởi kết quả gate.

- `services/api/src/test/java/vn/simtim/api/qa/Qa02E2eTest.java`: +384 -0
- `services/api/src/main/java/vn/simtim/api/reports/infrastructure/JdbcReportsRepository.java`: +9 -5
- `apps/web/src/App.vue`: +0 -1
- `docs/testing/QA-02-authorize.md`: +16 -0
- `docs/testing/QA-02-test-plan.md`: +68 -0
- `docs/testing/QA-02-test-report.md`: +83 -0
- `docs/testing/QA-02-traceability.md`: +41 -0
- `docs/testing/README.md`: +11 -0
- `CHANGELOG.md`: +2 -0
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
Nguyễn Văn Thi - 2026-10-06 09:42:00

Hoàn thiện hợp đồng API, exception handler và integration tests cho review PR #25 (Issue #23): (1) Cập nhật wire contracts — bổ sung các endpoints quản lý target BATCH (`GET/POST/DELETE /api/v1/sales/promotions/{id}/batches`) và trường `batchIds` vào `contracts/sales-promotions.openapi.yaml`; bổ sung 5 trường snapshot khuyến mãi (`appliedPromotionId`, `appliedPromotionCode`, `appliedPromotionName`, `promotionDiscountType`, `promotionDiscountValue`) vào schema `InvoiceLineResponse` trong `contracts/sales-invoices.openapi.yaml`. (2) Xử lý ngoại lệ — ánh xạ `InventorySaleException` trong `SaleExceptionHandler` sang HTTP 409, 404, 422. (3) Bổ sung integration tests — thêm 6 test cases trong `PromotionApiTest` (CRUD batch target, phân quyền ghi 403, cấm trộn target BATCH/PRODUCT trả 422, trùng batch 409) và 5 test cases trong `SaleApiTest` (quote preview giảm giá, checkout chiết khấu theo sản phẩm và theo lô FEFO, đơn 0 VND khi giảm 100%, snapshot bất biến trên `sales.invoice_lines`, chặn xóa khuyến mãi đã dùng trả 409); cấu hình `@TestMethodOrder` và cô lập dữ liệu giữa các test. (4) Cập nhật `contracts/SALES_PROMOTIONS_REVIEW.md` ghi nhận bằng chứng kiểm thử và phạm vi component PR.

- `.gitignore`: +1 -0
- `contracts/SALES_PROMOTIONS_REVIEW.md`: +17 -7
- `contracts/sales-invoices.openapi.yaml`: +5 -0
- `contracts/sales-promotions.openapi.yaml`: +86 -0
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleExceptionHandler.java`: +12 -0
- `services/api/src/test/java/vn/simtim/api/promotion/PromotionApiTest.java`: +155 -0
- `services/api/src/test/java/vn/simtim/api/sale/SaleApiTest.java`: +188 -0
- `CONTRIBUTION_LOG.md`: +15 -0
---

---
Nguyễn Văn Thi - 2026-10-06 01:16:00

Hoàn thiện bổ sung theo review Issue #23: (1) Target BATCH — tạo bảng `sales.promotion_batches`, domain record `PromotionBatch`, JPA entities, repository và 3 endpoints quản lý phạm vi lô (`/promotions/{id}/batches`). Thêm quy tắc ràng buộc không cho phép trộn lẫn phạm vi BATCH và PRODUCT. (2) Tích hợp bán hàng FEFO — trong `SaleService.quote` và `checkout`, sau khi giải thuật FEFO phân bổ lô hàng, gọi `bestPromotion` để chọn mức giảm tối ưu nhất cho từng dòng theo sản phẩm/lô và khung giờ hiệu lực. (3) Snapshot hóa đơn bất biến — bổ sung 4 cột `applied_promotion_code`, `applied_promotion_name`, `promotion_discount_type`, `promotion_discount_value` vào `sales.invoice_lines`, thêm DB CHECK constraint bảo toàn giá trị và hỗ trợ giao dịch giảm 100% (0 VND). (4) Cập nhật migration Flyway V11 và tài liệu ranh giới `contracts/SALES_PROMOTIONS_REVIEW.md`.

- `CHANGELOG.md`: +1 -0
- `contracts/SALES_PROMOTIONS_REVIEW.md`: +13 -10
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionController.java`: +49 -4
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionResponse.java`: +13 -2
- `services/api/src/main/java/vn/simtim/api/promotion/application/PromotionService.java`: +34 -2
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionBatch.java`: +9 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionRepository.java`: +9 -1
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionBatchId.java`: +32 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionBatchJpa.java`: +30 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionBatchJpaRepository.java`: +29 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionJpaRepository.java`: +20 -2
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionRepositoryAdapter.java`: +43 -13
- `services/api/src/main/java/vn/simtim/api/sale/api/InvoiceResponse.java`: +9 -2
- `services/api/src/main/java/vn/simtim/api/sale/api/QuoteResponse.java`: +12 -4
- `services/api/src/main/java/vn/simtim/api/sale/application/QuoteResult.java`: +8 -1
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +174 -160
- `services/api/src/main/java/vn/simtim/api/sale/domain/InvoiceLine.java`: +5 -2
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/InvoiceLineJpa.java`: +16 -4
- `services/api/src/main/resources/db/migration/V11__promotion_batch_targets_and_invoice_snapshots.sql`: +31 -0
- `CONTRIBUTION_LOG.md`: +27 -0
---

---
Nguyễn Văn Thi - 2026-10-05 17:03:00

Khắc phục lỗi mã khuyến mãi trùng giữa hai cửa hàng cùng tổ chức theo review inline của Project Owner: (1) Thống nhất phạm vi kiểm tra trùng mã khuyến mãi (precheck) với DB constraint `UNIQUE (organization_id, code)` và unique index `(organization_id, lower(code))` trên bảng `sales.promotions`. (2) Bỏ tham số `storeId` khỏi `existsByCodeInsensitive` và `existsByCodeInsensitiveExcluding` trong `PromotionJpaRepository`, `PromotionRepository`, `PromotionRepositoryAdapter` và `PromotionService` để kiểm tra trùng mã trên toàn tổ chức thay vì chỉ trong cùng cửa hàng. MANAGER ở store B khi tạo hoặc cập nhật mã đã dùng ở store A sẽ nhận lỗi HTTP 409 có cấu trúc từ tầng nghiệp vụ, thay vì vượt qua precheck rồi vấp DB constraint văng lỗi 500. (3) Bổ sung 2 integration test cross-store `crossStore_sameOrg_duplicateCode_returns409` và `crossStore_sameOrg_duplicateCode_caseInsensitive_returns409`. Biên dịch thành công mã nguồn chính và test (`mvnw compile test-compile` đạt 0).

- `CHANGELOG.md`: +4 -0
- `services/api/src/main/java/vn/simtim/api/promotion/application/PromotionService.java`: +2 -2
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionRepository.java`: +3 -2
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionJpaRepository.java`: +8 -4
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionRepositoryAdapter.java`: +4 -4
- `services/api/src/test/java/vn/simtim/api/promotion/PromotionApiTest.java`: +34 -0
- `CONTRIBUTION_LOG.md`: +14 -0
---

---
Nguyễn Văn Thi - 2026-10-05 00:43:12

Khắc phục triệt để các phản hồi review của Project Owner trên PR #25 (Issue #23): (1) Đảm bảo tính nhất quán giữa OpenAPI contract, DTO và implementation — loại bỏ hoàn toàn query param `storeId` ở endpoint `/promotions/applicable` và trường `storeId` trong `PromotionRequest`, làm rõ cơ chế suy diễn store từ session. (2) Siết chặt phạm vi store-scope trên mọi route và layer (`PromotionRepository`, `PromotionJpaRepository`, `PromotionRepositoryAdapter`, `PromotionService`, `PromotionController`): thay thế `listByOrg` bằng `listByStore`, kiểm tra `(organizationId, storeId)` cho tất cả các thao tác `getById`, `update`, `delete`, `addProduct`, `removeProduct`, `listProducts`, ngăn hoàn toàn MANAGER ở store A truy cập/sửa/xóa khuyến mãi của store B cùng tổ chức. (3) Bổ sung 4 integration test cô lập cross-store cùng tổ chức (`crossStore_list`, `crossStore_get`, `crossStore_delete`, `crossStore_applicable`). Chạy toàn bộ test suite API đạt 101/101 tests và toàn bộ kiểm thử `scripts/verify.ps1` đều đạt.

- `CHANGELOG.md`: +1 -1
- `contracts/sales-promotions.openapi.yaml`: +15 -8
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionController.java`: +25 -20
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionRequest.java`: +4 -4
- `services/api/src/main/java/vn/simtim/api/promotion/application/PromotionService.java`: +21 -18
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionRepository.java`: +11 -6
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionJpaRepository.java`: +14 -8
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionRepositoryAdapter.java`: +17 -12
- `services/api/src/test/java/vn/simtim/api/promotion/PromotionApiTest.java`: +83 -14
- `CONTRIBUTION_LOG.md`: +18 -0
---

---
Nguyễn Văn Thi - 2026-10-04 17:36:46

Fix hai lỗi bảo mật PRO-01B theo review của Project Owner (Issue #23): (1) Ràng buộc phạm vi đọc/ghi theo `SessionPrincipal` — bỏ `storeId` khỏi query param `applicable` và khỏi body `create`/`update`; mọi endpoint lấy `organizationId`/`storeId` từ session; thêm `requireSameOrg()` trả 403 nếu `X-Organization-Id` header không khớp principal, chặn cross-store spoofing. (2) Kiểm tra `scale ≤ 2` cho `discountValue` kiểu PERCENT trước khi lưu — tránh `numeric(14,2)` làm tròn ngầm giá trị 3+ chữ số thập phân hoặc biến giá trị rất nhỏ thành 0 và gây lỗi DB. Bổ sung 6 integration test mới (cross-org 403, applicable từ principal không cần storeId, scale 3 bậc từ chối, scale 2 bậc hợp lệ). Tổng 4 file thay đổi, 147 dòng thêm / 25 dòng xóa.

- `CHANGELOG.md`: +2 -1
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionController.java`: +94 -5
- `services/api/src/main/java/vn/simtim/api/promotion/application/PromotionService.java`: +13 -4
- `services/api/src/test/java/vn/simtim/api/promotion/PromotionApiTest.java`: +63 -0
- `CONTRIBUTION_LOG.md`: +17 -0
TV2 — Nguyễn Văn Trung - 2026-10-05 15:22:49

Sửa INV-01: chuẩn hóa `findByClientOperationId` trả `List` thay vì `Optional` xuyên suốt domain port → JPA repository → infrastructure adapter → service → controller; đảm bảo filter `clientOperationId` được áp dụng TRƯỚC phân trang (`limit`/`offset` bị bỏ qua khi có filter), luôn trong phạm vi `organizationId`/`storeId` của session. Cập nhật truy vấn JPQL thêm `ORDER BY receivedAt DESC`. Bổ sung 2 provider integration test trên PostgreSQL 17 Testcontainers: `filterBeforePagination` (tạo 3 phiếu, limit=1, chứng minh phiếu khớp filter vẫn được trả dù bị cắt nếu pagination đến trước) và `exactMatchOnly` (hai phiếu cùng store, filter A không trả B). Root gate `pwsh -File scripts/verify.ps1` chưa chạy lại sau thay đổi này; cần chạy trước khi mở PR.

- `CHANGELOG.md`: +1 -0
- `services/api/src/main/java/vn/simtim/api/inventory/api/ReceiptController.java`: +4 -3
- `services/api/src/main/java/vn/simtim/api/inventory/application/GoodsReceiptService.java`: +2 -2
- `services/api/src/main/java/vn/simtim/api/inventory/domain/GoodsReceiptRepository.java`: +2 -1
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/GoodsReceiptJpaRepository.java`: +2 -2
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/GoodsReceiptRepositoryAdapter.java`: +5 -3
- `services/api/src/test/java/vn/simtim/api/inventory/InventoryReceiptApiTest.java`: +70 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-03 16:39:57

Thực hiện issue [FE-02] Nhận hàng và tồn kho trên PWA (#20). Tách feature nhận/tồn theo ADR 0003 với domain, validation, presentation và demo/API adapter riêng; API mode dùng Bearer session do auth adapter quản lý, không lưu token bền và không fallback sang dữ liệu demo. Hoàn thiện form catalog ACTIVE, kiểm tra số lượng/giá/lô/hạn, field error server, khóa thao tác đang gửi, đọc lại tồn sau xác nhận, phân trang tồn/lô/biến động, phiếu nhiều dòng, quyền 401/403 và ẩn giá nhập với SALES.

Bảo vệ xác nhận nhận hàng bằng `Idempotency-Key` và `clientOperationId` ổn định trong session tab: timeout, lỗi mạng hoặc 5xx giữ dấu thao tác để GET đối soát; kết quả rỗng không tự POST, retry chủ động dùng lại cùng key và không tạo offline queue. Root gate `pwsh -File scripts/verify.ps1` đạt repository policy/links, web lint/format/typecheck/build, demo E2E 6/6, API consumer/E2E 35/35 và backend integration 77/77 trên PostgreSQL 17 Testcontainers; Maven kết thúc `BUILD SUCCESS`. Reviewer được chỉ định: TV3 — Nguyễn Văn Thi.

Sau review PR #31, bổ sung filter provider `GET receipts?clientOperationId` bắt buộc organization/store từ session, bỏ vòng quét tối đa 10.000 phiếu ở web và thêm provider test chống rò cửa hàng. Live Playwright test chạy qua Spring API cùng PostgreSQL 17 disposable đã chứng minh trường hợp POST commit nhưng response bị mất: reload tìm lại đúng phiếu bằng GET filter và tổng số POST vẫn là một. Root gate sau thay đổi đạt demo E2E 6/6, API consumer/E2E 35/35, backend integration 79/79 và Maven `BUILD SUCCESS`; live integration đạt 1/1.

- `apps/web/src/modules/inventory/`: +1169 -0
- `apps/web/src/App.vue`: +40 -203
- `apps/web/src/adapter.ts`, `apps/web/src/api.ts`: +43 -3
- `apps/web/src/style.css`: +323 -0
- `apps/web/tests/api-inventory.spec.ts`: +501 -0
- `apps/web/tests/api-auth.spec.ts`, `apps/web/tests/workflows.spec.ts`: +52 -5
- `apps/web/playwright.api.config.ts`: +2 -1
- `apps/web/README.md`: +18 -5
- `CHANGELOG.md`: +4 -0
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
TV4 — Lê Văn Chiến - 2026-10-03 16:39:57

Thực hiện issue [FE-02] Nhận hàng và tồn kho trên PWA (#20). Tách feature nhận/tồn theo ADR 0003 với domain, validation, presentation và demo/API adapter riêng; API mode dùng Bearer session do auth adapter quản lý, không lưu token bền và không fallback sang dữ liệu demo. Hoàn thiện form catalog ACTIVE, kiểm tra số lượng/giá/lô/hạn, field error server, khóa thao tác đang gửi, đọc lại tồn sau xác nhận, phân trang tồn/lô/biến động, phiếu nhiều dòng, quyền 401/403 và ẩn giá nhập với SALES.

Bảo vệ xác nhận nhận hàng bằng `Idempotency-Key` và `clientOperationId` ổn định trong session tab: timeout, lỗi mạng hoặc 5xx giữ dấu thao tác để GET đối soát; kết quả rỗng không tự POST, retry chủ động dùng lại cùng key và không tạo offline queue. Root gate `pwsh -File scripts/verify.ps1` đạt repository policy/links, web lint/format/typecheck/build, demo E2E 6/6, API consumer/E2E 35/35 và backend integration 77/77 trên PostgreSQL 17 Testcontainers; Maven kết thúc `BUILD SUCCESS`. Reviewer được chỉ định: TV3 — Nguyễn Văn Thi.

Sau review PR #31, bổ sung filter provider `GET receipts?clientOperationId` bắt buộc organization/store từ session, bỏ vòng quét tối đa 10.000 phiếu ở web và thêm provider test chống rò cửa hàng. Live Playwright test chạy qua Spring API cùng PostgreSQL 17 disposable đã chứng minh trường hợp POST commit nhưng response bị mất: reload tìm lại đúng phiếu bằng GET filter và tổng số POST vẫn là một. Root gate sau thay đổi đạt demo E2E 6/6, API consumer/E2E 35/35, backend integration 79/79 và Maven `BUILD SUCCESS`; live integration đạt 1/1.

- `apps/web/src/modules/inventory/`: +1169 -0
- `apps/web/src/App.vue`: +40 -203
- `apps/web/src/adapter.ts`, `apps/web/src/api.ts`: +43 -3
- `apps/web/src/style.css`: +323 -0
- `apps/web/tests/api-inventory.spec.ts`: +501 -0
- `apps/web/tests/api-auth.spec.ts`, `apps/web/tests/workflows.spec.ts`: +52 -5
- `apps/web/playwright.api.config.ts`: +2 -1
- `apps/web/README.md`: +18 -5
- `CHANGELOG.md`: +4 -0
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
Lê Văn Chiến - 2026-10-02 15:11:50

Thực hiện issue [INV-02] Tra cứu tồn, lô, hạn và biến động (#19). Triển khai ba API chỉ đọc tồn hiện tại theo session scope với bộ lọc và phân trang ổn định; tính đúng tồn thực tế, tồn khả dụng và trạng thái hạn theo `Asia/Ho_Chi_Minh`; không trả giá vốn cho SALES; cho phép truy biến động về chứng từ nguồn. Công bố `InventorySalePort` để SAL-01 khóa tồn, lập kế hoạch FEFO, trừ lô và ghi biến động SALE trong cùng transaction, không trả JPA entity hoặc tự commit.

Bổ sung OpenAPI, migration index, provider tests cho hạn hôm qua/hôm nay/+7/+8, không hạn, lô BLOCKED và nhiều lô, cùng consumer compile test cho module sales. `pwsh -File scripts/verify.ps1` đạt toàn bộ root gate; API đạt 54/54 tests, web demo đạt 6/6 và API-mode E2E đạt 23/23. Chữ ký port vẫn chờ TV3 xác nhận và SAL-01 chuyển từ contract cũ trước khi hai module được tích hợp.

- `CHANGELOG.md`: +1 -0
- `contracts/`: +330 -0
- `services/api/README.md`: +3 -1
- `services/api/src/main/java/vn/simtim/api/inventory/`: +967 -2
- `services/api/src/main/resources/db/migration/V8__inventory_read_indexes.sql`: +11 -0
- `services/api/src/test/java/vn/simtim/api/inventory/`: +456 -0
- `services/api/src/test/java/vn/simtim/api/sale/InventorySalePortContractTest.java`: +55 -0
- `CONTRIBUTION_LOG.md`: +17 -0
---

---
Nguyễn Văn Trung - 2026-10-01 17:01:05

Thực hiện issue [INV-01] Transaction nhận hàng, lô và biến động (#18). Triển khai API nhận hàng POST /api/v1/inventory/receipts và GET danh sách/chi tiết; thực hiện transaction nguyên tử lưu phiếu, tạo lô hàng, tăng số dư tồn kho và ghi nhận biến động RECEIPT. Xử lý idempotency qua header Idempotency-Key và hash payload; áp dụng phân quyền RBAC (receipts.read, receipts.write cho STOCK, MANAGER, ADMIN; inventory.read cho tất cả các vai trò). Khởi tạo Flyway V5 (schema inventory + audit.audit_logs) và V6 (permissions). Hoàn thành 9 provider integration tests trên PostgreSQL 17 (Testcontainers) và vượt qua toàn bộ baseline gate kiểm thử verify.ps1 (40/40 tests).

- `CHANGELOG.md`: +1 -0
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +7 -0
- `services/api/src/main/java/vn/simtim/api/inventory/`: +735 -0
- `services/api/src/main/resources/db/demo/R__demo_seed.sql`: +5 -2
- `services/api/src/main/resources/db/migration/V5__inventory.sql`: +115 -0
- `services/api/src/main/resources/db/migration/V6__inventory_permissions.sql`: +19 -0
- `services/api/src/test/java/vn/simtim/api/inventory/`: +295 -0
- `CONTRIBUTION_LOG.md`: +15 -0
---

---
Nguyễn Văn Thi - 2026-10-01 13:48:00

Căn chỉnh SAL-01 khớp hoàn toàn tài liệu Sprint 2 và hợp đồng ranh giới: bổ sung route alias `POST /api/v1/sales/checkout`, hỗ trợ đơn hàng 0 VND và sửa CHECK constraint `sales.payments.amount >= 0`, chuẩn hóa tính toán tiền thanh toán theo `grandTotal` và tiền thối `change_amount` lưu vào `sales.invoices`, siết chặt kiểm tra phạm vi cửa hàng `storeId` theo session token, tách public model `BatchStock` và `InventoryConflictException` về module inventory để xóa phụ thuộc ngược.

Bổ sung 3 ca kiểm thử mới (alias route checkout, chặn sai storeId, đơn hàng 0đ), nâng tổng số ca kiểm thử bán hàng lên 19/19 tests (toàn bộ 50/50 backend tests pass trên PostgreSQL 17 Testcontainers). Cập nhật wire contract OpenAPI và tài liệu review.

- `contracts/sales-invoices.openapi.yaml`: +35 -0
- `contracts/SALES_INVOICE_REVIEW.md`: +18 -7
- `services/api/src/main/resources/db/migration/V6__sales_invoices.sql`: +2 -1
- `services/api/src/main/java/vn/simtim/api/inventory/application/BatchStock.java`: +1 -1
- `services/api/src/main/java/vn/simtim/api/inventory/application/InventoryConflictException.java`: +8 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/InventoryPort.java`: +1 -3
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/InventoryPortAdapter.java`: +4 -4
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleController.java`: +33 -14
- `services/api/src/main/java/vn/simtim/api/sale/api/CheckoutRequest.java`: +3 -3
- `services/api/src/main/java/vn/simtim/api/sale/api/QuoteRequest.java`: +2 -2
- `services/api/src/main/java/vn/simtim/api/sale/api/InvoiceResponse.java`: +4 -2
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleExceptionHandler.java`: +3 -2
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +21 -7
- `services/api/src/main/java/vn/simtim/api/sale/domain/Invoice.java`: +1 -0
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/InvoiceJpa.java`: +3 -1
- `services/api/src/test/java/vn/simtim/api/sale/SaleApiTest.java`: +86 -14
- `CHANGELOG.md`: +1 -1
- `CONTRIBUTION_LOG.md`: +29 -0
---

---
Nguyễn Văn Thi - 2026-10-01 08:00:04

Chuẩn hóa PRO-01B theo review Sprint 2: cấu hình wire API khuyến mãi sang `/api/v1/sales/promotions` (hỗ trợ alias `/api/v1/promotions` tương thích ngược), bổ sung Working Draft OpenAPI 3.1 wire contract và ma trận quyền review tại `contracts/`. Cập nhật V5 migration với `CREATE SCHEMA IF NOT EXISTS sales;`. Bổ sung integration test kiểm tra route prefix `/api/v1/sales/promotions`, toàn bộ 15/15 test khuyến mãi và 46/46 backend integration test pass trên PostgreSQL 17 (Testcontainers).

Root verification đạt repository policy/links, web lint/format/typecheck/build, 6 demo E2E, 23 API consumer E2E và 46 backend tests trên PostgreSQL 17.11 disposable sạch.

- `contracts/sales-promotions.openapi.yaml`: +336 -0
- `contracts/SALES_PROMOTIONS_REVIEW.md`: +48 -0
- `contracts/README.md`: +2 -0
- `CHANGELOG.md`: +2 -0
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +2 -1
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionController.java`: +1 -1
- `services/api/src/main/resources/db/migration/V5__sales_promotions.sql`: +1 -1
- `services/api/src/test/java/vn/simtim/api/promotion/PromotionApiTest.java`: +8 -0
---

---
Nguyễn Văn Thi - 2026-10-01 07:46:16

Chuẩn hóa SAL-01 theo review Sprint 2: cấu hình wire API bán hàng sang `/api/v1/sales/quote` và `/api/v1/sales/invoices`, ma trận quyền `sales.read` / `sales.write`. Tách ranh giới module với `InventoryPort` (`vn.simtim.api.inventory.application`) và `InventoryPortAdapter`, loại bỏ việc truy cập trực tiếp repo nội bộ của inventory từ sale. Bổ sung Working Draft OpenAPI 3.1 wire contract `contracts/sales-invoices.openapi.yaml` và `contracts/SALES_INVOICE_REVIEW.md`. Toàn bộ 16/16 test bán hàng và 47/47 backend integration test pass trên PostgreSQL 17 (Testcontainers).

Root verification đạt repository policy/links, web lint/format/typecheck/build, 6 demo E2E, 23 API consumer E2E và 47 backend tests trên PostgreSQL 17.11 disposable sạch.

- `contracts/sales-invoices.openapi.yaml`: +281 -0
- `contracts/SALES_INVOICE_REVIEW.md`: +52 -0
- `contracts/README.md`: +2 -0
- `services/api/src/main/java/vn/simtim/api/inventory/application/InventoryPort.java`: +37 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/InventoryPortAdapter.java`: +69 -0
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/InventoryBalanceJpa.java`: +3 -3
- `services/api/src/main/java/vn/simtim/api/inventory/infrastructure/InventoryBalanceJpaRepository.java`: +5 -5
- `services/api/src/main/java/vn/simtim/api/sale/api/SaleController.java`: +16 -16
- `services/api/src/main/java/vn/simtim/api/sale/application/SaleService.java`: +13 -14
- `services/api/src/main/java/vn/simtim/api/sale/domain/SaleRepository.java`: +1 -23
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/SaleRepositoryAdapter.java`: +11 -46
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +5 -3
- `services/api/src/main/resources/db/demo/R__demo_seed.sql`: +2 -2
- `services/api/src/main/resources/db/migration/V6__sales_invoices.sql`: +6 -6
- `services/api/src/test/java/vn/simtim/api/sale/SaleApiTest.java`: +21 -21
- `CHANGELOG.md`: +1 -1
---
---

---
TV1 — Nguyễn Đức Phát - 2026-09-30 23:09:32

Rà lại tài liệu `docs/project` theo `main` và GitHub ngày 30/09: thêm điểm vào phân biệt tài liệu hiện hành với bản lưu, ghi mốc Sprint 1 đã đóng và các PR Sprint 2 đang mở; sửa những câu còn nói REQ-01/BE-02 chờ review hoặc merge sau khi đã tích hợp. Giữ bản nháp lịch sử để truy vết, không xóa nội dung cũ. Bộ tài liệu vẫn ở nhánh local, chưa push hoặc mở PR.

Đối chiếu GitHub: milestone Sprint 1 đóng 7/7 Issue; Sprint 2 mở 7/7 Issue; PR #25–#27 đang mở, chưa tính Done. Validation: `pwsh -File scripts/verify.ps1 -SkipInstall` đạt policy, links, lint, format, build, 6 demo E2E, 23 API-mode E2E và 31 backend tests trên PostgreSQL 18.6 tạm; server thử nghiệm đã dừng.

- `CHANGELOG.md`: +1 -0
- `docs/README.md`: +5 -4
- `docs/project/README.md`: +30 -0
- `docs/project/REQ-01_DECISION_DRAFT.md`: +1 -1
- `docs/project/REQ-01_SCOPE_RECORD_2026-09-29.md`: +2 -2
- `docs/project/SPRINT_1_KICKOFF.md`: +2 -0
- `docs/project/SPRINT_1_WORD_BACKLOG_DRAFT.md`: +2 -2
- `docs/project/SPRINT_2_KICKOFF.md`: +1 -0
- `docs/project/governance/BACKLOG.md`: +4 -4
- `CONTRIBUTION_LOG.md`: +19 -0
---

---
Nguyễn Văn Thi - 2026-09-30 15:50:00

SAL-01: Implement quote, checkout CASH, hóa đơn và trừ tồn FEFO trên `services/api`.

Flyway V5 tạo `inventory` schema (goods_receipts, goods_receipt_lines, product_batches, inventory_balances, FEFO index). Flyway V6 tạo `sales` schema (invoices, invoice_lines, invoice_line_batches, payments) và `inventory.stock_movements` với cross-schema FK; thêm permissions `invoices.read`/`invoices.write`. Domain layer: Invoice, InvoiceLine, InvoiceLineBatch, Payment, BatchStock, ProductSnapshot, SaleRepository port, 3 custom exceptions. Application layer: SaleService với quote (read-only, không ghi DB) và checkout CASH (FEFO allocation, pessimistic lock, atomic: invoice + batch + balance deduction + movement + payment trong một transaction; validate cash trước khi lock stock). Infrastructure: 5 JPA entities + Spring Data repos + SaleRepositoryAdapter (JPA+JDBC, EntityManager.flush() trước JDBC insert để đảm bảo FK). API: POST /api/v1/invoices/quote, POST /api/v1/invoices, GET /api/v1/invoices, GET /api/v1/invoices/{id}; SaleExceptionHandler map 404/409/422. DemoAccounts mở rộng seedInventory() chạy sau khi users được tạo, thay thế stock_demo user đã xóa khỏi R__demo_seed.sql. 15 integration tests (SaleApiTest) trên PostgreSQL 17 Testcontainers — tổng 46/46 pass.

- `services/api/src/main/resources/db/migration/V5__inventory.sql`: +87 -0
- `services/api/src/main/resources/db/migration/V6__sales_invoices.sql`: +135 -0
- `services/api/src/main/resources/db/demo/R__demo_seed.sql`: +10 -100
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/DemoAccounts.java`: +89 -0
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +3 -0
- `services/api/src/main/java/vn/simtim/api/sale/domain/`: +162 -0
- `services/api/src/main/java/vn/simtim/api/sale/application/`: +242 -0
- `services/api/src/main/java/vn/simtim/api/sale/infrastructure/`: +511 -0
- `services/api/src/main/java/vn/simtim/api/sale/api/`: +206 -0
- `services/api/src/test/java/vn/simtim/api/sale/SaleApiTest.java`: +484 -0
- `CHANGELOG.md`: +2 -1
- `CONTRIBUTION_LOG.md`: +33 -0
---

---
Nguyễn Văn Trung - 2026-09-30 15:43:00

Triển khai Issue #22 (REP-01): Báo cáo doanh thu và tồn từ dữ liệu đã commit — nhánh `feature/REP-01-reports`.

- API Backend: `ReportsController` (`GET /api/v1/reports/revenue`, `GET /api/v1/reports/inventory`), `ReportsService`, `JdbcReportsRepository`; RBAC với `reports.read` chỉ cho MANAGER/ADMIN; doanh thu SUM header `grand_total` COMPLETED, không join lines/payments; tồn phân loại EXPIRED/NEAR_EXPIRY/VALID/NO_EXPIRY theo quy tắc contract; timezone `Asia/Ho_Chi_Minh`.
- DB: Migration `V9__reports_permissions.sql` — grant `reports.read` cho MANAGER/ADMIN.
- Web: Thêm `fetchApi` vào `AuthAdapter`; trang báo cáo chuyển sang dual-mode: API mode gọi 2 endpoint, có loading/empty/error states và filter ngày; demo mode giữ nguyên regression.

- `services/api/src/main/resources/db/migration/V9__reports_permissions.sql`: +8 -0
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +1 -0
- `services/api/src/main/java/vn/simtim/api/reports/api/ReportsController.java`: +43 -0
- `services/api/src/main/java/vn/simtim/api/reports/api/RevenueReportResponse.java`: +6 -0
- `services/api/src/main/java/vn/simtim/api/reports/api/InventoryReportResponse.java`: +10 -0
- `services/api/src/main/java/vn/simtim/api/reports/api/InventoryBalanceDto.java`: +14 -0
- `services/api/src/main/java/vn/simtim/api/reports/application/ReportsService.java`: +32 -0
- `services/api/src/main/java/vn/simtim/api/reports/domain/ReportsRepository.java`: +10 -0
- `services/api/src/main/java/vn/simtim/api/reports/infrastructure/JdbcReportsRepository.java`: +62 -0
- `services/api/src/test/java/vn/simtim/api/reports/api/ReportsControllerTest.java`: +57 -0
- `apps/web/tests/api-reports.spec.ts`: +61 -0
- `apps/web/src/adapter.ts`: +8 -2
- `apps/web/src/App.vue`: +75 -30
---

---
Nguyễn Văn Thi - 2026-09-30 10:31:47

Hoàn thành PRO-01B: API khuyến mãi cơ bản theo sản phẩm. Flyway V5 tạo schema `sales`, bảng `promotions` và `promotion_products`, quyền `promotions.read` (mọi vai trò) / `promotions.write` (MANAGER/ADMIN). Implement đầy đủ clean-architecture (domain → application → infrastructure → api) theo đúng pattern đã có: 9 REST endpoints (CRUD + phạm vi sản phẩm + tra cứu applicable tại điểm bán), validation business rule (time window, discount value, unique code), RFC 7807 exception handler scoped cho module. Cập nhật AuthSecurity và R__demo_seed. Viết 14 integration test trên PostgreSQL 17.11 (Testcontainers) bao phủ auth matrix, CRUD lifecycle, conflict, validation và applicable query; tổng 45/45 tests pass.

Root verification đạt repository policy/links, web lint/format/typecheck/build, 6 demo E2E, 23 API consumer E2E và 45 backend tests trên PostgreSQL 17.11 disposable sạch.

Defer có ghi chú: `promotion_batches` (chờ `inventory.product_batches` từ INV-01) và `applied_promotion_id` trong `invoice_lines` (thuộc SAL-01).

- `CHANGELOG.md`: +2 -0
- `services/api/src/main/java/vn/simtim/api/auth/infrastructure/AuthSecurity.java`: +5 -0
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionController.java`: +132 -0
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionExceptionHandler.java`: +45 -0
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionRequest.java`: +35 -0
- `services/api/src/main/java/vn/simtim/api/promotion/api/PromotionResponse.java`: +36 -0
- `services/api/src/main/java/vn/simtim/api/promotion/application/PromotionService.java`: +128 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/Promotion.java`: +30 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionConflictException.java`: +7 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionNotFoundException.java`: +7 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionProduct.java`: +9 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionRepository.java`: +26 -0
- `services/api/src/main/java/vn/simtim/api/promotion/domain/PromotionValidationException.java`: +7 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionJpa.java`: +62 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionJpaRepository.java`: +55 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionProductId.java`: +37 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionProductJpa.java`: +27 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionProductJpaRepository.java`: +30 -0
- `services/api/src/main/java/vn/simtim/api/promotion/infrastructure/PromotionRepositoryAdapter.java`: +94 -0
- `services/api/src/main/resources/db/demo/R__demo_seed.sql`: +29 -0
- `services/api/src/main/resources/db/migration/V5__sales_promotions.sql`: +45 -0
- `services/api/src/test/java/vn/simtim/api/promotion/PromotionApiTest.java`: +361 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-09-30 09:40:37

Chỉnh cách đọc Issue Sprint 2: tách Owner/Reviewer thành hai dòng và bỏ metadata ưu tiên/mã lặp với tiêu đề, nhãn. Điều chỉnh phân công theo trao đổi với Project Owner: TV4 nhận FE-02 cùng QA-02, TV2 nhận REP-01 sau INV-01/02, TV3 tập trung SAL-01 và PRO-01B P1; TV1 điều phối/review. Đồng bộ backlog, kickoff, contract ranh giới và ghi ngoại lệ Sprint 2 so với trách nhiệm module dài hạn. Issue là phân công kế hoạch, chưa ghi nhận code feature của thành viên khác. Giữ nhánh tài liệu ở máy, chưa push.

Validation: `pwsh -File scripts/verify.ps1 -SkipInstall` đạt policy, links, lint, format, build, 6 demo E2E, 23 API-mode E2E và 31 backend tests trên PostgreSQL 18.6 tạm; CSDL tạm đã xóa. Đã kiểm tra lại owner/reviewer/assignee của Issue #18–#24 trên GitHub.

- `GitHub Issues #18–#24`: N/A
- `CHANGELOG.md`: +1 -0
- `contracts/SPRINT_2_BOUNDARY_DRAFT.md`: +2 -2
- `docs/project/SPRINT_2_KICKOFF.md`: +11 -11
- `docs/project/governance/BACKLOG.md`: +3 -3
- `docs/project/governance/TEAM.md`: +2 -0
- `CONTRIBUTION_LOG.md`: +16 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-09-30 09:22:53

Thiết lập Sprint 2 để nhóm bắt đầu code: đóng milestone Sprint 1 đã có 7/7 Issue xong; tạo milestone Sprint 2 hạn 07/10/2026, nhãn sprint:2 và bảy Issue #18–#24 có assignee, reviewer, ưu tiên, phụ thuộc, tiêu chí chấp nhận và kế hoạch test. Tách FE-02 làm phần PWA nhận/tồn của INV-01/02. Đồng bộ backlog, kickoff, contract ranh giới và README theo nền Sprint 1 đã tích hợp. Chưa push nhánh tài liệu hoặc mở PR; Project board chưa cập nhật vì credential hiện thiếu quyền project.

Validation: `pwsh -File scripts/verify.ps1 -SkipInstall` với JDK 25/PostgreSQL 18.6 disposable đạt policy, links, lint, format, typecheck/build, 6 demo E2E, 23 API-mode E2E và 31 backend tests. CSDL tạm đã được xóa; GitHub xác nhận 7 Issue mở đúng milestone/assignee/nhãn và milestone Sprint 1 đã đóng.

- `CHANGELOG.md`: +1 -0
- `README.md`: +7 -7
- `apps/web/README.md`: +2 -2
- `contracts/README.md`: +1 -1
- `contracts/SPRINT_2_BOUNDARY_DRAFT.md`: +53 -6
- `docs/README.md`: +2 -2
- `docs/project/SPRINT_2_KICKOFF.md`: +57 -0
- `docs/project/SPRINT_2_REVIEW_PLAN.md`: +0 -47
- `docs/project/governance/BACKLOG.md`: +9 -8
- `services/api/README.md`: +3 -3
- `CONTRIBUTION_LOG.md`: +20 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-09-30 08:44:50

Rút gọn bộ review Sprint 2 trên nền main: giữ quyết định nghiệp vụ đã xác nhận, owner/thứ tự P0-P1, ranh giới HTTP/Java và các điểm kỹ thuật cần TV2/TV3/TV4 chốt. Tách quyết định game đào tạo và các bản DDL, OpenAPI, QA quá chi tiết khỏi nhánh review này; giữ bản nháp cũ trong nhánh local để truy vết. Không mở Issue, thay schema/API hay push.

Validation: `pwsh -File scripts/verify.ps1 -SkipInstall` với JDK 25 và PostgreSQL 18.6 disposable đạt policy, links, lint, format, typecheck/build, 6 demo E2E, 23 API-mode E2E và 31 backend tests. Database tạm đã được xóa.

- `CHANGELOG.md`: +1 -0
- `contracts/README.md`: +1 -0
- `contracts/SPRINT_2_BOUNDARY_DRAFT.md`: +49 -0
- `docs/README.md`: +2 -0
- `docs/project/SPRINT_2_REVIEW_PLAN.md`: +47 -0
- `docs/project/governance/BACKLOG.md`: +2 -0
- `CONTRIBUTION_LOG.md`: +16 -0

---
Lê Văn Chiến - 2026-09-29 21:00:00

Hoàn thành QA-01 sau Request Changes của PR #15: chạy lại độc lập source tích hợp BE-02/BE-03, cập nhật đúng wire auth và class/method, chuyển 29/29 traceability cases sang Pass dựa trên evidence thực tế. Bổ sung regression test barcode trùng, kiểm tra `409` và không tạo thêm record.

Root verification đạt repository policy/links, web lint/format/typecheck/build, 6 demo E2E, 23 API consumer E2E và 31 backend tests trên PostgreSQL 17.11 disposable sạch. Issue #8 giữ mở tới khi reviewer chấp nhận và CI của head mới đạt.

- `services/api/src/test/java/vn/simtim/api/catalog/CatalogApiTest.java`: +19 -0
- `docs/testing/QA-01-authorize.md`: +22 -24
- `docs/testing/QA-01-test-plan.md`: +18 -12
- `docs/testing/QA-01-test-report.md`: +82 -70
- `docs/testing/QA-01-traceability.md`: +68 -73
- `docs/testing/README.md`: +3 -1
- `CHANGELOG.md`: +2 -0
- `CONTRIBUTION_LOG.md`: +16 -0
---

---
TV1 — Nguyễn Đức Phát - 2026-09-29 20:01:32

Hỗ trợ tích hợp PR #12 FE-01 với BE-02/BE-03 trên nền main `6ebf601`: tách route fixture kiểm thử quyền, cho test catalog đăng nhập HTTP bằng tài khoản fixture STOCK/SALES và gửi Bearer token; thêm ba ca thiếu token, SALES ghi dữ liệu và giả mạo scope trên controller thật. Giữ nguyên mã sản phẩm, ma trận quyền, API và migration. Ghi nhận công việc hỗ trợ của TV1; owner FE-01 là TV3, owner catalog là TV2.

Đồng bộ contract auth/session sang Accepted theo review provider PR #14 và consumer PR #12 ngày 29/09/2026; giữ bằng chứng Draft cũ làm lịch sử. Tách lại entry FE-01/BE-03 sau conflict, giữ nguyên nội dung và timestamp.

Validation local: root `pwsh -File scripts/verify.ps1` đạt policy/links/lint/format/typecheck/build, 6 demo + 23 auth/consumer + 30 API tests; thêm 6 live PWA/backend tests đạt trên cả bốn vai trò, logout/revocation, reload và sai mật khẩu. Java 25 release 21/PostgreSQL 18.6, database disposable sạch. Kết quả hỗ trợ đã hoàn tất local; CI và approval độc lập sau push còn chờ, chưa ghi PR #12 hoặc issue #7 đã tích hợp.

Diff-stat của đợt hỗ trợ so với PR head `e0fd44a`:

- `CHANGELOG.md`: +3 -1
- `CONTRIBUTION_LOG.md`: +25 -0
- `contracts/AUTH_SESSION_REVIEW.md`: +15 -6
- `contracts/README.md`: +3 -3
- `contracts/auth-session.openapi.yaml`: +6 -4
- `docs/README.md`: +1 -1
- `services/api/README.md`: +2 -2
- `services/api/src/test/java/vn/simtim/api/ApiBootstrapTest.java`: +11 -9
- `services/api/src/test/java/vn/simtim/api/catalog/CatalogApiTest.java`: +108 -3
---

---
Lê Văn Chiến - 2026-09-28 17:26:58

Nhận trách nhiệm cho đợt chuẩn bị QA-01: hoàn thiện test plan, ma trận quyền, ma trận requirement–contract–test và checkpoint report; phân biệt bằng chứng BE-02 do owner báo cáo với kết quả QA trực tiếp xác nhận, đồng thời giữ BE-03 ở trạng thái blocked đến khi có API thật.

- `docs/testing/QA-01-authorize.md`: +73 -0
- `docs/testing/QA-01-test-plan.md`: +227 -0
- `docs/testing/QA-01-test-report.md`: +90 -0
- `docs/testing/QA-01-traceability.md`: +90 -0
- `docs/testing/README.md`: +14 -0
- `docs/README.md`: +4 -0
- `CHANGELOG.md`: +1 -0
- `CONTRIBUTION_LOG.md`: +15 -0
---

---
Nguyễn Văn Thi - 2026-09-28 16:18:06

FE-01: tách AuthAdapter demo/API, bổ sung E2E API mode và sửa lỗi review.
Bổ sung `ApiAuthAdapter` gọi `POST /api/auth/login` và `GET /api/me`; `DemoAuthAdapter` giữ nguyên hành vi offline. `onMounted` chỉ khôi phục phiên đúng mode, ngăn fallback tài khoản demo khi chạy API build. Form đăng nhập khóa submit và hiển thị trạng thái chờ khi `busy`. Thêm `playwright.api.config.ts` build với `VITE_USE_API=true` và 6 E2E test bao gồm reload, loading/guard submit trùng, HTTP 401/403, lỗi mạng và role không hợp lệ. Sửa strict-mode locator trong test guard submit. Verify gate PASS: policy/lint/format/typecheck/build/8 demo E2E/6 API E2E.

- `CHANGELOG.md`: +2 -0
- `apps/web/package.json`: +3 -1
- `apps/web/playwright.api.config.ts`: +18 -0
- `apps/web/playwright.config.ts`: +1 -0
- `apps/web/src/App.vue`: +21 -4
- `apps/web/src/adapter.ts`: +49 -2
- `apps/web/tests/adapter.spec.ts`: +1 -49
- `apps/web/tests/api-auth.spec.ts`: +113 -0
---

---
Nguyễn Văn Trung - 2026-09-28 14:38:45

Thực hiện issue [BE-03] API danh mục, sản phẩm và nhà cung cấp. Khởi tạo toàn bộ module `catalog` theo kiến trúc Modular Monolith (ADR 0003), bao gồm lớp Domain, Infrastructure, Application và API. Cấu hình Testcontainers và hoàn thành 17 tests tích hợp đảm bảo CRUD và business rules.

- `services/api/pom.xml`: +4 -0
- `services/api/src/main/java/vn/simtim/api/catalog/`: +900 -0
- `services/api/src/test/java/vn/simtim/api/catalog/`: +250 -0
- `CONTRIBUTION_LOG.md`: +12 -0
---

---
Nguyễn Đức Phát - 2026-09-24 00:29:26

Sửa CI governance sau lần chạy đầu trên Pull Request: checkout đủ parent commit để kiểm tra đúng diff, nâng GitHub Actions khỏi runtime Node.js đã ngừng hỗ trợ và loại bỏ lượt chạy trùng trên feature branch.

- `.github/workflows/ci.yml`: +7 -3
- `CHANGELOG.md`: +4 -0
- `CONTRIBUTION_LOG.md`: +10 -0
---

---
Nguyễn Đức Phát - 2026-09-23 23:42:37

Project Owner review và chấp nhận đợt hardening governance có AI hỗ trợ: chốt kiến trúc Sprint 1, chuẩn hóa GitHub Flow/Issue forms/CODEOWNERS, đưa cấu hình công cụ cùng Word draft ra khỏi tracking và thêm repository policy vào CI.

- `.agents/` và `.codex/`: +0 -59
- `.github/`: +136 -36
- Cấu hình và tài liệu root: +34 -7
- `docs/`: +156 -14; 1 DOCX bỏ tracking nhưng giữ local
- `scripts/`: +35 -0
- `CONTRIBUTION_LOG.md`: +13 -0
---

---
Nguyễn Đức Phát - 2026-09-21 21:28:26

Làm rõ mốc hiện tại là baseline Sprint 0 đã nghiệm thu và chỉ là điểm xuất phát để cả nhóm triển khai Sprint 1; không ghi nhận nhầm kế hoạch nền tảng thành implementation đã hoàn thành.

- `CHANGELOG.md`: +2 -0
- `README.md`: +1 -1
- `docs/architecture/adr/`: +4 -4
- `docs/project/`: +2 -2
- `CONTRIBUTION_LOG.md`: +12 -0
---

---
Nguyễn Đức Phát - 2026-09-21 20:52:23

Chốt baseline sạch để nhóm bắt đầu Sprint 1: giữ PWA Sprint 0 đã kiểm thử làm code active duy nhất, tổ chức lại repository và tài liệu, xác lập ownership, ADR, kế hoạch kickoff, lệnh root, lint/format và CI. Code thử nghiệm Sprint 1 không được nhập vào baseline này.

- `.github/`: +47 -4
- `apps/web/`: +3379 -2497; 1 binary di chuyển nguyên trạng
- `archive/`: +1 -1
- `contracts/`: +5 -0
- `docs/`: +188 -116; 7 binary di chuyển nguyên trạng
- `infra/`: +3 -0
- `AGENTS.md`, `README.md` và cấu hình root: +50 -30
- `scripts/`: +101 -0
- `services/`: +5 -0
- `CONTRIBUTION_LOG.md`: +17 -0
---

---
Codex (AI hỗ trợ; chờ Project Owner xác nhận người chịu trách nhiệm) - 2026-09-21 14:16:48

Chuẩn hóa Sprint 0 thành baseline có thể bàn giao: lưu trữ prototype cũ, thiết lập quy trình cộng tác/Codex, thống nhất cổng kiểm chứng với CI và cập nhật tài liệu trước khi tích hợp `main`. Mục này không tự quy đổi thành đóng góp của thành viên cho đến khi người chịu trách nhiệm review và xác nhận.

- `.agents/skills/verify-sim-tim/SKILL.md`: +19 -0
- `.agents/skills/verify-sim-tim/agents/openai.yaml`: +4 -0
- `.codex/config.toml`: +7 -0
- `.codex/rules/safety.rules`: +29 -0
- `.editorconfig`: +18 -0
- `.gitattributes`: +15 -0
- `.github/pull_request_template.md`: +7 -1
- `.github/workflows/ci.yml`: +1 -2
- `.gitignore`: +15 -0
- `.nvmrc`: +1 -0
- `AGENTS.md`: +57 -0
- `CHANGELOG.md` và `prototype-v2/CHANGELOG.md`: +38 -22
- `CONTRIBUTING.md`: +39 -0
- `README.md`: +33 -30
- `archive/prototype-v1/README.md`: +13 -0
- `archive/prototype-v1/index.html`, `app.js`, `styles.css`: +0 -0 (di chuyển nguyên trạng)
- `archive/prototype-v1/assets/mentor-mai.png`: N/A (di chuyển nguyên trạng)
- `docs/AGENTS.md`: +8 -0
- `docs/README.md`: +32 -0
- `docs/meetings/2026-09-14-tong-hop-du-an-hop-nhom.md`: +2 -2
- `docs/project-management/MERGE_01.md`: +7 -1
- `prototype-v2/AGENTS.md`: +13 -0
- `prototype-v2/README.md`: +6 -8
- `prototype-v2/package-lock.json`: +3 -0
- `prototype-v2/package.json`: +5 -1
- `CONTRIBUTION_LOG.md`: +33 -0
---
