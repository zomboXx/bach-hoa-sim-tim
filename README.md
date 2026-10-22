# Bách Hóa Sim Tím

Ứng dụng web bán hàng, quản lý tồn kho và luyện tập nghiệp vụ cho cửa hàng bán lẻ. Phần đào tạo được tách khỏi dữ liệu bán hàng và tồn kho thật của hệ thống.

## Dùng thử trực tuyến

Mở **[bản demo trên Vercel](https://bach-hoa-sim-tim-demo.vercel.app/)**. Đăng nhập bằng một trong bốn mã nhân viên `sales`, `stock`, `manager`, `admin` và mật khẩu demo do nhóm cung cấp riêng. Bản demo đã chọn sẵn tổ chức `SIMTIM` và cửa hàng `MAIN`.

Bạn có thể [mở bộ mã vạch mẫu](https://bach-hoa-sim-tim-demo.vercel.app/demo-barcodes/index.html) để thử nhập mã hoặc quét bằng camera. Trên điện thoại, hãy cho phép trình duyệt dùng camera. Hai thiết bị đăng nhập cùng nhân viên và cửa hàng có thể cùng thao tác trên giỏ bán hàng khi có mạng; [cách hoạt động và kiểm thử giỏ chung](contracts/POS_SHARED_CART.md) được ghi riêng.

**Lưu ý:** Bản trực tuyến dùng chung một cơ sở dữ liệu Neon cho người thử nghiệm. Giao dịch có thể làm thay đổi dữ liệu demo và không đồng bộ với dữ liệu chạy trên máy bạn. Không nhập thông tin khách hàng thật.

PWA hỗ trợ giữ phiên khi F5 trong cùng tab, tối đa theo thời hạn đăng nhập (mặc định 8 giờ); đăng xuất hoặc phiên hết hạn cần đăng nhập lại. Khi bản cập nhật được triển khai, hãy đăng nhập lại một lần để kích hoạt khôi phục phiên cho tab đang dùng.

## Chạy trên máy bằng Docker

Cần Docker Desktop hoặc Docker Engine có Compose. Tại thư mục gốc của dự án, chạy:

```powershell
Copy-Item infra/.env.example infra/.env
notepad infra/.env
docker compose --env-file infra/.env -f infra/compose.demo.yml up --build -d --wait
```

Trong `infra/.env`, đặt hai mật khẩu **khác nhau** cho `POSTGRES_PASSWORD` và `SIMTIM_DEMO_PASSWORD`; mật khẩu demo dài ít nhất 12 ký tự và không quá 72 byte UTF-8. Đừng commit file này. Sau khi Compose khởi động xong, mở [web local](http://127.0.0.1:5174) và đăng nhập bằng một trong bốn mã nhân viên trên với `SIMTIM_DEMO_PASSWORD` vừa đặt.

Compose chạy web, API và PostgreSQL cùng nhau. Xem [hướng dẫn Docker chi tiết](infra/README.md) để thử các luồng nghiệp vụ, xem log, đổi cổng và dừng dịch vụ. Lần chạy đầu cần thêm thời gian tải image và build.

QR chuyển khoản cần [cấu hình tài khoản nhận trên backend](docs/testing/MANUAL_TRANSFER_SETUP.md). Trong **Bán hàng**, chọn **Chuyển khoản QR → Tạo QR và giữ hàng**; nhân viên kiểm tra tiền về rồi xác nhận thủ công. Giao dịch được lưu cùng hóa đơn; lịch sử chuyển khoản cho biết số giao dịch và tổng tiền đã xác nhận. Tính năng được bật theo cấu hình riêng của từng môi trường.

## Phát triển và kiểm thử

Để chạy riêng giao diện với dữ liệu mẫu trong trình duyệt, cần PowerShell 7 và Node.js 22:

```powershell
pwsh -File scripts/setup.ps1
pwsh -File scripts/dev.ps1
```

Chế độ này dùng tài khoản `NV001`/`demo123`; dữ liệu chỉ nằm trong trình duyệt, không gửi lên API hoặc chia sẻ giữa các thiết bị. Tài khoản này **không dùng được** ở bản Vercel hoặc Compose. Xem [hướng dẫn web](apps/web/README.md) để cấu hình API mode khi phát triển.

Để chạy bộ kiểm tra của repository Git, cần thêm JDK 21 và Docker đang hoạt động:

```powershell
pwsh -File scripts/verify.ps1
```

Lệnh này kiểm tra quy tắc repository, liên kết tài liệu, lint/build web, kiểm thử trình duyệt và kiểm thử API với PostgreSQL qua Testcontainers. [Hướng dẫn API](services/api/README.md) có các lệnh chạy riêng cho backend.

## Mã nguồn và tài liệu

| Vị trí | Nội dung |
| --- | --- |
| `apps/web/` | Giao diện PWA Vue/TypeScript |
| `services/api/` | API Spring Boot và migration Flyway |
| `infra/` | Docker Compose và hướng dẫn chạy local |
| `contracts/` | Giao diện giữa các module và trạng thái từng contract |
| `docs/` | Yêu cầu, backlog, thiết kế và kết quả kiểm thử |
| `scripts/` | Lệnh thiết lập, phát triển và kiểm tra |

Bắt đầu từ [bản đồ tài liệu](docs/README.md) để phân biệt tài liệu hiện hành với bản nháp và prototype lịch sử. [Backlog](docs/project/governance/BACKLOG.md) ghi phạm vi công việc; [changelog](CHANGELOG.md) ghi thay đổi đã tích hợp. Hồ sơ phát hành cuối vẫn đang được theo dõi trong [REL-01](https://github.com/zomboXx/bach-hoa-sim-tim/issues/59).
