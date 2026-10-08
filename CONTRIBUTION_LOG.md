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
