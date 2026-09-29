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
