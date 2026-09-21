# Bách Hóa Sim Tím

Ứng dụng quản lý bán hàng và tồn kho cho một cửa hàng bán lẻ, kết hợp mô phỏng đào tạo nghiệp vụ. Repository hiện lấy **Sprint 0 — Prototype 02** làm baseline: Vue 3, TypeScript, Vite, IndexedDB và Playwright; backend và Godot thuộc các sprint tiếp theo, không được xem là đã có trong baseline này.

## Bắt đầu nhanh

Yêu cầu Node.js 22 (tối thiểu 20.19).

```powershell
cd prototype-v2
npm ci
npm run dev
```

Mở `http://127.0.0.1:5174`. Các tài khoản demo dùng chung mật khẩu `demo123`:

| Tài khoản | Vai trò |
| --- | --- |
| `NV001` | Nhân viên bán hàng |
| `KHO001` | Nhân viên hàng hóa |
| `QL001` | Quản lý cửa hàng |

## Kiểm chứng baseline

```powershell
cd prototype-v2
npm run verify
```

Lệnh này chạy kiểm tra kiểu dữ liệu, production build và toàn bộ Playwright E2E. Xem tiêu chí merge tại [MERGE_01.md](docs/project-management/MERGE_01.md).

## Cấu trúc repository

```text
prototype-v2/       PWA đang được phát triển và kiểm thử
docs/               Yêu cầu, thiết kế, quản lý dự án và biên bản
.github/             CI, Issue template và Pull Request template
.agents/skills/      Workflow dùng lại cho Codex trong repository
.codex/              Cấu hình và hàng rào an toàn cho Codex
archive/             Prototype cũ chỉ giữ để truy vết
```

- [Bản đồ tài liệu](docs/README.md)
- [Hướng dẫn đóng góp](CONTRIBUTING.md)
- [Kiến trúc và giới hạn Prototype 02](prototype-v2/ARCHITECTURE.md)
- [Changelog](CHANGELOG.md)
- [Nhật ký đóng góp](CONTRIBUTION_LOG.md)

## Trạng thái và giới hạn

Baseline chạy hoàn toàn trong trình duyệt. Đăng nhập, phân quyền, API, thanh toán và đồng bộ máy chủ đang được mô phỏng; dữ liệu nằm trong browser profile hiện tại. Không sử dụng tài khoản demo hoặc dữ liệu prototype cho môi trường thật.

Prototype đời đầu đã được chuyển vào [archive/prototype-v1](archive/prototype-v1/README.md). Nó không thuộc build, test hoặc đường phát triển hiện tại.
