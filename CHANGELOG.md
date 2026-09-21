# Changelog

Các thay đổi đáng chú ý của dự án được ghi tại đây. Tài liệu này mô tả sản phẩm và repository; việc ghi nhận trách nhiệm cá nhân nằm trong `CONTRIBUTION_LOG.md`.

## Unreleased

### Changed

- Tổ chức lại monorepo theo deployable unit: `apps/web`, `apps/training-godot`, `services/api`, `contracts`, `infra`, `scripts`, `tools` và tài liệu theo mục đích.
- Khôi phục và harden Sprint 1 gồm Vue PWA, Spring Boot/PostgreSQL/Flyway và Godot Chapter 0; lưu game Vue/SVG cũ trong `archive/`.
- Ghi nhận owner/reviewer bằng tài khoản GitHub, thêm CODEOWNERS, hướng dẫn module và ADR kiến trúc.
- Thêm OpenAPI, ma trận RBAC, error contract cùng các lệnh root cho setup, development, verification và cleanup.
- Tách CI thành các cổng repository, API, Godot, web build và kiểm thử tích hợp.
- Chuẩn hóa cấu trúc repository: chuyển Prototype 01 vào `archive/prototype-v1/` và đưa changelog lên root.
- Bổ sung bản đồ tài liệu, hướng dẫn đóng góp, quy tắc agent, EditorConfig, Git attributes và cấu hình Codex an toàn.
- Thêm skill `verify-sim-tim` và lệnh `npm run verify` dùng chung giữa máy phát triển và CI.

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
