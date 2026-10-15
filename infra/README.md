# Chạy thử trên máy bằng Docker Compose

Chạy các lệnh dưới đây tại thư mục gốc repository. Cần Docker Engine/Desktop đang chạy và Compose plugin. Cấu hình này chỉ dành cho máy local: web/API chỉ mở trên `127.0.0.1`, PostgreSQL không mở cổng ra host và dữ liệu nằm trong named volume riêng `simtim-demo_demo_postgres_data`.

## 1. Chuẩn bị và khởi động

```powershell
Copy-Item infra/.env.example infra/.env
notepad infra/.env
docker compose --env-file infra/.env -f infra/compose.demo.yml up --build -d --wait
```

Đặt `POSTGRES_PASSWORD` và `SIMTIM_DEMO_PASSWORD` thành hai mật khẩu local riêng trong `infra/.env`. `SIMTIM_DEMO_PASSWORD` cần ít nhất 12 ký tự và không quá 72 byte UTF-8. Không thêm dấu nháy vào giá trị trong file và không commit file này. `POSTGRES_PORT` chỉ dùng cho Compose PostgreSQL dành cho phát triển, không dùng trong bản demo ba dịch vụ.

Kiểm tra trạng thái và truy cập:

```powershell
docker compose --env-file infra/.env -f infra/compose.demo.yml ps
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

Mở **http://127.0.0.1:5174**. Trang web được build ở API mode và Nginx chuyển `/api/` tới service API trong Compose. Health API trả `status: UP`. Nếu cổng 5174 hoặc 8080 đã có chương trình khác, sửa `WEB_PORT` hoặc `API_PORT` trong `infra/.env`, chạy lại `up`, rồi dùng cổng mới trong các URL.

## 2. Đăng nhập và thử web

Đăng nhập với `manager` hoặc `stock` và mật khẩu `SIMTIM_DEMO_PASSWORD`; `sales` và `admin` là hai tài khoản server khác. Mã tổ chức `SIMTIM`, cửa hàng `MAIN` đã nằm trong bản build web. Tài khoản `NV001`/`demo123` thuộc demo mode không dùng ở đây.

1. Vào **Tồn kho & lô hàng**: kiểm tra dữ liệu seed gồm gạo và táo, lô táo hết hạn ngày 31/12/2026. Xem tồn khả dụng, hạn dùng và biến động nguồn.
2. Với `stock` hoặc `manager`, vào **Nhận hàng**, chọn sản phẩm và nhà cung cấp còn hoạt động, nhập số lượng/giá/hạn hợp lệ, xác nhận phiếu. Đọc lại phiếu và tồn để kiểm tra lượng nhận được ghi một lần.
3. Với `manager` hoặc `admin`, mở **Báo cáo**. Báo cáo này đọc dữ liệu server; doanh thu ban đầu có thể bằng 0 cho tới khi checkout qua API.

Không dùng màn hình **Bán hàng** của web API mode làm bằng chứng checkout server: adapter bán hàng/hóa đơn chưa được nối. Nếu cần tự thử giao dịch thật, dùng mục tiếp theo. Kết quả không tự sửa các mục Fail/Blocked trong [QA-02](../docs/testing/QA-02-test-report.md).

## 3. Thử quote và checkout tiền mặt qua API

Trong PowerShell, dùng tài khoản `manager` đã seed. Lệnh quote không ghi dữ liệu; checkout **tạo hóa đơn và trừ tồn thật trong database demo**, nên chỉ chạy một lần cho một lượt bán. Nhập mật khẩu bằng prompt, không chép mật khẩu vào lệnh:

```powershell
$base = 'http://127.0.0.1:8080'
$secret = Read-Host 'SIMTIM_DEMO_PASSWORD' -AsSecureString
$login = @{
  organizationCode = 'SIMTIM'
  storeCode = 'MAIN'
  username = 'manager'
  password = [System.Net.NetworkCredential]::new('', $secret).Password
} | ConvertTo-Json
$session = Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login
$headers = @{
  Authorization = "Bearer $($session.accessToken)"
  'X-Organization-Id' = '10000000-0000-0000-0000-000000000001'
}
$items = @(@{ productId = '10000000-0000-0000-0000-000000000041'; quantity = 1 })
$quoteBody = @{ items = $items } | ConvertTo-Json -Depth 5
$quote = Invoke-RestMethod "$base/api/v1/sales/quote" -Method Post -Headers $headers -ContentType 'application/json' -Body $quoteBody
$quote | Format-List subtotal, discountTotal, grandTotal
```

Nếu muốn tiếp tục, nhập số tiền khách trả bằng VND nguyên, ít nhất bằng `grandTotal` vừa thấy. API hiện chưa có `expectedTotal` để từ chối thay đổi giá giữa quote và checkout; kiểm tra lại số tiền ngay trước khi gửi. Không gửi lại checkout nếu response bị mất: API chưa có khóa idempotency cho giao dịch bán.

```powershell
$cash = [long]::Parse((Read-Host 'Tiền mặt khách trả (VND)'))
$checkoutBody = @{ items = $items; cashAmount = $cash } | ConvertTo-Json -Depth 5
$invoice = Invoke-RestMethod "$base/api/v1/sales/checkout" -Method Post -Headers $headers -ContentType 'application/json' -Body $checkoutBody
$invoice | Format-List id, grandTotal, changeAmount
```

Sau đó xem lại hóa đơn qua `Invoke-RestMethod "$base/api/v1/sales/invoices/$($invoice.id)" -Headers $headers` và tải lại **Tồn kho & lô hàng** / **Báo cáo** trên web bằng tài khoản `manager`. Phiên web và phiên PowerShell là hai phiên đăng nhập độc lập.

## 4. Log, dừng và chạy lại

```powershell
docker compose --env-file infra/.env -f infra/compose.demo.yml logs --tail=100 api
docker compose --env-file infra/.env -f infra/compose.demo.yml down
```

Nếu `up --wait` thất bại, kiểm tra `docker compose ... ps` và `logs --tail=100 api` hoặc `logs --tail=100 web`. Nếu Docker báo không kết nối được daemon, hãy khởi động Docker Engine/Desktop trước rồi chạy lại lệnh `up`.

## 5. Sao lưu và khôi phục CSDL trên môi trường sạch (NFR-BAC-01)

Theo [NFR-BAC-01](../docs/product/analysis/02-danh-sach-yeu-cau.md) và tiêu chí chấp nhận của [QA-04](../docs/testing/QA-04-test-report.md), quy trình sao lưu và khôi phục được kiểm chứng độc lập trên PostgreSQL sạch.

### 5.1. Dùng script tiện ích

Chạy tại thư mục gốc repository:

```powershell
# Sao lưu dữ liệu hiện tại ra file SQL timestamped
pwsh -File scripts/backup-db.ps1

# Hoặc chỉ định đường dẫn file đầu ra cụ thể
pwsh -File scripts/backup-db.ps1 -OutFile "infra/backups/demo_checkpoint.sql"

# Khôi phục vào CSDL từ file bản sao
pwsh -File scripts/restore-db.ps1 -BackupFile "infra/backups/demo_checkpoint.sql"
```

### 5.2. Lệnh trực tiếp qua Docker Compose

1. **Sao lưu dữ liệu từ service PostgreSQL:**

```powershell
docker compose --env-file infra/.env -f infra/compose.demo.yml exec -T postgres pg_dump -U simtim -d simtim --clean --if-exists > infra/backups/demo_backup.sql
```

2. **Dựng môi trường PostgreSQL sạch (xóa toàn bộ volume cũ):**

```powershell
docker compose --env-file infra/.env -f infra/compose.demo.yml down -v
docker compose --env-file infra/.env -f infra/compose.demo.yml up -d --wait
```

3. **Khôi phục dữ liệu vào môi trường sạch:**

```powershell
Get-Content infra/backups/demo_backup.sql -Raw | docker compose --env-file infra/.env -f infra/compose.demo.yml exec -T postgres psql -U simtim -d simtim
```

4. **Kiểm tra tính toàn vẹn sau phục hồi:**

- Xác nhận HTTP health: `Invoke-RestMethod http://127.0.0.1:8080/actuator/health` trả `status: UP`.
- Đăng nhập web **http://127.0.0.1:5174** bằng tài khoản `manager` / `SIMTIM_DEMO_PASSWORD`.
- Vào **Báo cáo** đối soát doanh thu và số hóa đơn đã checkout trước khi sao lưu.
- Vào **Tồn kho & lô hàng** đối soát số lượng tồn khả dụng và lịch sử biến động khớp với số liệu trước phục hồi (`on_hand_quantity = SUM(quantity_delta)`).
