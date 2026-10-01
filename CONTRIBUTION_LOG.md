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
Nguyễn Văn Trung - 2026-09-30 15:43:00

Triển khai Issue #22 (REP-01): Báo cáo doanh thu và tồn từ dữ liệu đã commit — nhánh `feature/REP-01-reports`.

- API Backend: `ReportsController` (`GET /api/v1/reports/revenue`, `GET /api/v1/reports/inventory`), `ReportsService`, `JdbcReportsRepository`; RBAC với `reports.read` chỉ cho MANAGER/ADMIN; doanh thu SUM header `grand_total` COMPLETED, không join lines/payments; tồn phân loại EXPIRED/NEAR_EXPIRY/VALID/NO_EXPIRY theo quy tắc contract; timezone `Asia/Ho_Chi_Minh`.
- DB: Migration `V5__reports_permissions.sql` — grant `reports.read` cho MANAGER/ADMIN.
- Web: Thêm `fetchApi` vào `AuthAdapter`; trang báo cáo chuyển sang dual-mode: API mode gọi 2 endpoint, có loading/empty/error states và filter ngày; demo mode giữ nguyên regression.

- `services/api/src/main/resources/db/migration/V5__reports_permissions.sql`: +8 -0
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
