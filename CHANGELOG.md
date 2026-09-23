# Changelog

Các thay đổi đáng chú ý của dự án được ghi tại đây. Tài liệu này mô tả sản phẩm và repository; việc ghi nhận trách nhiệm cá nhân nằm trong `CONTRIBUTION_LOG.md`.

## Unreleased

### Changed

- Chốt kiến trúc Sprint 1 theo module nghiệp vụ: frontend feature modules và backend modular monolith với ports/adapters.
- Chuẩn hóa GitHub Flow theo Issue ngắn hạn, Pull Request có reviewer và GitHub Project làm nguồn trạng thái vận hành.
- Giữ `AGENTS.md` làm hướng dẫn portable; ngừng track cấu hình `.agents`, `.codex` và bản Word đang soạn, đồng thời chặn chúng bằng repository policy trong CI.

### Added

- Bổ sung Issue forms và runbook cấu hình GitHub cho Project Owner.

## 0.2.1 — 2026-09-21

Đây là bản hardening repository của baseline Sprint 0 để bắt đầu Sprint 1; không bao gồm implementation Sprint 1.

### Changed

- Tổ chức baseline theo `apps`, `services`, `contracts`, `infra`, `scripts`, `docs` và `archive`; chỉ PWA Sprint 0 là code active đã nghiệm thu.
- Ghi nhận bốn thành viên và CODEOWNERS, bổ sung ADR cùng kế hoạch kickoff để chia PR Sprint 1 theo dependency rõ ràng.
- Thêm lệnh root `setup`, `dev`, `verify`, `clean`; CI tách repository policy và web baseline gate.
- Bổ sung ESLint, Prettier và kiểm tra link nội bộ; chuẩn hóa đường dẫn tài liệu theo product, architecture, project, deliverables và archive.
- Chuẩn hóa cấu trúc repository: chuyển Prototype 01 vào `archive/prototype-v1/` và đưa changelog lên root.
- Bổ sung bản đồ tài liệu, hướng dẫn đóng góp, quy tắc agent, EditorConfig, Git attributes và cấu hình Codex an toàn.
- Thêm skill `verify-sim-tim` và lệnh `npm run verify` dùng chung giữa máy phát triển và CI.

### Fixed

- Bổ sung key ổn định cho các danh sách Vue/SVG để tránh tái sử dụng DOM sai khi dữ liệu thay đổi.

## 0.2.0 — 2026-09-14

### Added

- Prototype 02 bằng Vue 3, TypeScript và Vite với 11 màn hình theo ba vai trò demo.
- Luồng nhận hàng, tồn, bán hàng, hóa đơn, báo cáo và khuyến mãi dùng chung dữ liệu IndexedDB.
- Kiểm kê theo lô, hàng đợi offline mô phỏng, phát hiện xung đột và bước quản lý duyệt.
- Một ca đào tạo 2D bằng Vue/SVG, có bàn phím, cảm ứng, tìm đường và phản hồi hành động sai.
- Playwright E2E và GitHub Actions cho build cùng kiểm thử browser.

### Fixed

- Giới hạn Service Worker cache ở static assets công khai để tránh lỗi response có `Vary: Origin`.

### Known limitations

- Chưa có REST backend, xác thực server, PostgreSQL hoặc runtime Godot trong baseline này.
- Quyền frontend, thanh toán và đồng bộ máy chủ chỉ phục vụ trình diễn.

## 0.1.0 — 2026-09-10

### Added

- Prototype HTML/CSS/JavaScript đầu tiên cho màn hình nhân viên bán hàng và khu đào tạo bảy chapter.

Prototype này hiện được lưu tại `archive/prototype-v1/`.
