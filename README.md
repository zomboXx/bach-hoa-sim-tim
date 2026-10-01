# Bách Hóa Sim Tím

Repository đã tích hợp nền Sprint 1: PWA Vue/TypeScript, API Spring Boot, PostgreSQL/Flyway, đăng nhập server, phân quyền và catalog. [Milestone Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2) gồm các Issue nhận hàng, tồn, bán, báo cáo và QA; [kickoff](docs/project/SPRINT_2_KICKOFF.md) ghi thứ tự làm việc và [contract](contracts/SPRINT_2_BOUNDARY_DRAFT.md) ghi quy tắc chung. API nghiệp vụ Sprint 2 chưa được triển khai.

## Bắt đầu nhanh

Yêu cầu Git, PowerShell 7 và Node.js 22. `scripts/dev.ps1` chạy PWA ở demo mode; API mode cần Java 21, PostgreSQL và lệnh riêng trong [hướng dẫn API](services/api/README.md).

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

Lệnh này kiểm tra whitespace/link, ESLint, Prettier, TypeScript, production build, Playwright E2E và API integration test với PostgreSQL qua Docker. Xem [cổng baseline](docs/project/governance/MERGE_01.md), [hướng dẫn API](services/api/README.md) và [kickoff Sprint 2](docs/project/SPRINT_2_KICKOFF.md).

## Cấu trúc repository

```text
apps/web/           PWA demo và API mode; Sprint 2 nối nghiệp vụ thật theo Issue
services/api/       API auth/catalog đã tích hợp; Sprint 2 thêm inventory/sales/reports
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

Demo mode vẫn dùng dữ liệu trên thiết bị. API mode đã kết nối đăng nhập/session BE-02; nhận hàng, bán hàng và báo cáo thật sẽ được nối lần lượt trong Sprint 2. Các giao dịch demo không được gửi lên API hoặc tính vào báo cáo server. Không sử dụng tài khoản demo hoặc dữ liệu prototype cho môi trường thật.

Prototype đời đầu nằm trong [archive/prototype-v1](archive/prototype-v1/README.md). Trạng thái công việc hiện hành nằm ở GitHub Issues/PR, không suy ra từ prototype lịch sử.
