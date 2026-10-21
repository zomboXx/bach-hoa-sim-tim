# Bách Hóa Sim Tím

Hệ thống bán hàng và quản lý tồn kho với trải nghiệm đào tạo tách khỏi dữ liệu vận hành. `main` đã tích hợp API nhận hàng, tồn kho, bán hàng tiền mặt, khuyến mãi, báo cáo và PWA. Sprint 2 đã được [nghiệm thu có ngoại lệ](docs/project/SPRINT_2_ACCEPTANCE_2026-10-07.md): gate tự động đạt nhưng [QA-02](docs/testing/QA-02-test-report.md) vẫn còn 3 Fail và 3 Blocked P0. [Sprint 4](docs/project/SPRINT_4_KICKOFF.md) đã mở việc theo [ADR 0004](docs/architecture/adr/0004-training-2d-web.md); các Issue mới chưa phải tính năng đã hoàn tất.

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
- [Backlog hiện hành](docs/project/governance/BACKLOG.md)
- [Milestone Sprint 4](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/4)
- [Hướng dẫn đóng góp](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Nhật ký đóng góp](CONTRIBUTION_LOG.md)

## Quét mã và dùng điện thoại cùng máy tính

Trong **API mode**, đăng nhập cùng nhân viên và cửa hàng trên hai thiết bị, cùng địa chỉ ứng dụng. Vào **Bán hàng** trên cả hai: chọn hàng, nhập mã rồi Enter hoặc bấm **Quét mã bằng camera** đều cập nhật giỏ chung. Giỏ trên thiết bị còn lại cập nhật gần thời gian thực khi có mạng; trạng thái kết nối hiển thị ngay trong giỏ. Sau mỗi mã được nhận, bấm **Quét tiếp** để tránh tự cộng lặp khi giữ camera đứng yên. Chỉ checkout mới ghi hóa đơn và trừ tồn.

Điện thoại cần **HTTPS** và quyền camera; địa chỉ HTTP qua IP nội bộ thường không được trình duyệt cấp camera. Demo mode dùng tài khoản NV001 lưu dữ liệu riêng trên từng trình duyệt, nên không đồng bộ sang thiết bị khác. [Bộ mã vạch mẫu](apps/web/public/demo-barcodes/index.html) có ảnh PNG/SVG kèm tên và ghi rõ bộ API hoặc demo; khi ứng dụng đang chạy, mở `/demo-barcodes/index.html` để tải ảnh. Xem [contract và hướng dẫn kiểm thử](contracts/POS_SHARED_CART.md).

Bản chạy với profile `demo` và `SIMTIM_DEMO_PASSWORD` có **10 sản phẩm có giá, mã vạch và tồn kho** tại cửa hàng MAIN. Demo trong trình duyệt vẫn dùng bộ bảy sản phẩm riêng. Dữ liệu tạo thêm ở local không tự chuyển sang database deploy; muốn đối chiếu, dùng cùng API mode và cùng database/cửa hàng.

| Sản phẩm API | Mã quét | Giá bán ban đầu (VND) | Tồn ban đầu |
|---|---|---:|---:|
| Gạo gói 1 kg | `8930000000001` | 25.000 | 100 cái |
| Táo cân ký | `ST-APPLE-01` | 40.000 | 50 kg |
| Sữa tươi ít đường 180 ml | `8934673120010` | 8.000 | 120 cái |
| Bánh quy bơ 200 g | `8934673120027` | 32.000 | 60 cái |
| Mì ăn liền 75 g | `ST-NOODLE-01` | 5.000 | 150 cái |
| Dầu ăn 1 lít | `ST-OIL-01` | 48.000 | 40 cái |
| Đường trắng 1 kg | `ST-SUGAR-01` | 23.000 | 80 cái |
| Muối iốt 500 g | `001000000008` | 6.000 | 60 cái |
| Nước suối 500 ml | `ST-WATER-01` | 5.000 | 200 cái |
| Cà phê rang xay 250 g | `ST-COFFEE-01` | 60.000 | 45 cái |

Đây là lượng và giá seed, chưa tính khuyến mãi. Khi nâng cấp database demo cũ, tám mặt hàng bổ sung có phiếu nhận/lô/movement riêng; không sửa phiếu gạo/táo đã có. Khởi động lại không bù lại hàng đã bán, không đặt lại giá đã chỉnh và không tạo phiếu/movement trùng. Hạn của lô mới tính theo ngày Việt Nam khi tạo lần đầu, rồi giữ nguyên qua các lần khởi động. Ảnh quét có đủ mười mặt hàng API; mã muối giữ hai số 0 ở đầu. Bộ seed này phục vụ demo, không dùng cho cửa hàng vận hành thật.

## Giới hạn hiện hành

Compose khởi động **API mode**: nhận hàng, tồn/lô/hạn, khuyến mãi, bán tiền mặt, hóa đơn và báo cáo dùng server. Dữ liệu giao dịch demo mode không đi vào PostgreSQL hoặc báo cáo server. [Hồ sơ QA-02](docs/testing/QA-02-test-report.md) giữ kết quả ban đầu và bằng chứng chạy lại trên mã đã tích hợp. Không dùng bộ demo hoặc mật khẩu này cho môi trường thật.
