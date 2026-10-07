# Bách Hóa Sim Tím

Hệ thống bán hàng và quản lý tồn kho với trải nghiệm đào tạo tách khỏi dữ liệu vận hành. `main` đã tích hợp API nhận hàng, tồn kho, bán hàng tiền mặt, khuyến mãi, báo cáo và PWA. Sprint 2 đã được [nghiệm thu có ngoại lệ](docs/project/SPRINT_2_ACCEPTANCE_2026-10-07.md): gate tự động đạt nhưng [QA-02](docs/testing/QA-02-test-report.md) vẫn còn 3 Fail và 3 Blocked P0.

## Bắt đầu nhanh: demo với Docker Compose

Cần Git và Docker Engine/Desktop có Docker Compose. Chạy từ thư mục gốc repository:

```powershell
Copy-Item infra/.env.example infra/.env
notepad infra/.env
docker compose --env-file infra/.env -f infra/compose.demo.yml up --build -d --wait
```

Trong `infra/.env`, đặt `POSTGRES_PASSWORD` và `SIMTIM_DEMO_PASSWORD` thành **hai mật khẩu local riêng**, không để trống. Mật khẩu tài khoản demo dài từ 12 ký tự đến 72 byte UTF-8. Chờ lệnh `up` kết thúc rồi mở **http://127.0.0.1:5174**; API health ở **http://127.0.0.1:8080/actuator/health**. Ba dịch vụ web, API và PostgreSQL khởi động trong cùng Compose; lần đầu cần thời gian tải image và build. Không commit `infra/.env`.

Đăng nhập web bằng một trong các tài khoản **server** `sales`, `stock`, `manager`, `admin`, cùng mật khẩu `SIMTIM_DEMO_PASSWORD` vừa đặt. Mã tổ chức/cửa hàng `SIMTIM`/`MAIN` được cấu hình sẵn trong bản demo. Tài khoản `NV001`/`demo123` chỉ thuộc demo mode chạy riêng trong trình duyệt, không dùng cho Compose.

Xem [hướng dẫn demo từng bước](infra/README.md) để thử nhận hàng, tồn, báo cáo và checkout API, xem log, đổi cổng hoặc dừng dịch vụ. Để dừng mà giữ dữ liệu:

```powershell
docker compose --env-file infra/.env -f infra/compose.demo.yml down
```

## Phát triển và kiểm chứng

Chạy PWA demo mode riêng trên máy cần PowerShell 7 và Node.js 22:

```powershell
pwsh -File scripts/setup.ps1
pwsh -File scripts/dev.ps1
```

Mode này dùng dữ liệu trình duyệt và tài khoản `NV001`, `KHO001`, `QL001` với mật khẩu `demo123`; không gửi giao dịch lên API. [Hướng dẫn web](apps/web/README.md) mô tả cách chạy API mode riêng khi phát triển. Gate repository cần Docker Engine cho PostgreSQL Testcontainers:

```powershell
pwsh -File scripts/verify.ps1
```

Gate kiểm tra policy, link, ESLint, Prettier, TypeScript, web build, Playwright và Maven integration tests. [Hướng dẫn API](services/api/README.md) có các lệnh kiểm thử hẹp hơn.

## Cấu trúc repository

| Thư mục | Nội dung |
| --- | --- |
| `apps/web/` | PWA Vue/TypeScript, demo mode và các adapter API đã tích hợp |
| `services/api/` | API Spring Boot, Flyway và kiểm thử PostgreSQL |
| `contracts/` | Contract liên module với trạng thái riêng trong từng tài liệu |
| `infra/` | Compose PostgreSQL cho dev và Compose ba dịch vụ cho demo |
| `docs/` | Yêu cầu, backlog, thiết kế, hồ sơ QA và lịch sử |
| `scripts/` | Lệnh setup, dev, verify và clean được hỗ trợ |
| `archive/` | Prototype lịch sử, chỉ dùng để truy vết |

- [Bản đồ tài liệu và tiến độ](docs/README.md)
- [Backlog Sprint 2](docs/project/governance/BACKLOG.md)
- [Hướng dẫn đóng góp](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Nhật ký đóng góp](CONTRIBUTION_LOG.md)

## Giới hạn hiện hành

Compose khởi động **API mode**: web nhận hàng, đọc tồn/lô/hạn, khuyến mãi và báo cáo từ server. Màn hình bán hàng/hóa đơn của web chưa nối checkout API; có thể thử endpoint checkout tiền mặt theo [kịch bản API trong hướng dẫn demo](infra/README.md). Dữ liệu giao dịch demo mode không đi vào PostgreSQL hoặc báo cáo server. Báo cáo QA-02 còn ghi các khoảng trống P0 về độ chính xác đơn vị EA, thay đổi giá giữa quote và checkout, quyền xem hóa đơn của SALES, checkout lặp lại và E2E web bán hàng. Không dùng bộ demo hoặc mật khẩu này cho môi trường thật.
