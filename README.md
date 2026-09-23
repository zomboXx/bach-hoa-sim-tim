# Bách Hóa Sim Tím

Đây là baseline đã hoàn thành và nghiệm thu của Sprint 0, đồng thời là điểm xuất phát để cả nhóm triển khai Sprint 1. Code active đã được nghiệm thu chỉ gồm PWA dùng Vue 3, TypeScript, Vite, IndexedDB và Playwright. API, PostgreSQL và Godot chưa thuộc baseline; chúng chỉ được thêm trong Sprint 1 qua backlog item và Pull Request có owner, reviewer và kiểm thử.

## Bắt đầu nhanh

Yêu cầu Git, PowerShell 7 và Node.js 22.

```powershell
pwsh -File scripts/setup.ps1
pwsh -File scripts/dev.ps1
```

Mở `http://127.0.0.1:5174`. Các tài khoản demo dùng chung mật khẩu `demo123`:

| Tài khoản | Vai trò |
| --- | --- |
| `NV001` | Nhân viên bán hàng |
| `KHO001` | Nhân viên hàng hóa |
| `QL001` | Quản lý cửa hàng |

## Kiểm chứng baseline

```powershell
pwsh -File scripts/verify.ps1
```

Lệnh này kiểm tra whitespace/link, ESLint, Prettier, TypeScript, production build và toàn bộ Playwright E2E. Xem [cổng baseline](docs/project/governance/MERGE_01.md) và [kế hoạch khởi động Sprint 1](docs/project/SPRINT_1_KICKOFF.md).

## Cấu trúc repository

```text
apps/web/           PWA Sprint 0 và điểm bắt đầu frontend Sprint 1
services/           Nơi đặt dịch vụ khi backlog item khởi tạo được duyệt
contracts/          Hợp đồng liên module, luôn ghi trạng thái draft/accepted
infra/              Hạ tầng local đi cùng dịch vụ sở hữu
docs/               Product, kiến trúc, quản trị dự án, kiểm thử và lịch sử
scripts/            Lệnh setup/dev/verify/clean được hỗ trợ
.github/             CI, Issue template và Pull Request template
archive/             Prototype cũ chỉ giữ để truy vết
```

`AGENTS.md` chứa hướng dẫn portable dùng chung. Cấu hình riêng của công cụ như `.agents/` và `.codex/`, cùng bản Word đang soạn, chỉ giữ local và không thuộc baseline.

- [Bản đồ tài liệu](docs/README.md)
- [Hướng dẫn đóng góp](CONTRIBUTING.md)
- [Kiến trúc và giới hạn PWA](apps/web/ARCHITECTURE.md)
- [Changelog](CHANGELOG.md)
- [Nhật ký đóng góp](CONTRIBUTION_LOG.md)

## Trạng thái và giới hạn

Baseline chạy hoàn toàn trong trình duyệt. Đăng nhập, phân quyền, API, thanh toán và đồng bộ máy chủ đang được mô phỏng; dữ liệu nằm trong browser profile hiện tại. Không sử dụng tài khoản demo hoặc dữ liệu prototype cho môi trường thật.

Prototype đời đầu nằm trong [archive/prototype-v1](archive/prototype-v1/README.md). Code Sprint 1 thử nghiệm trước baseline được giữ ngoài `main` để tham khảo, không được xem là nguồn sự thật hay tự động ghi nhận là đóng góp đã nghiệm thu.
