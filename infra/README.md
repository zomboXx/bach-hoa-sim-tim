# Chạy thử trên máy bằng Docker Compose

Chạy các lệnh dưới đây tại thư mục gốc repository. Cần Docker Engine/Desktop đang chạy và Compose plugin. Cấu hình này chỉ dành cho máy local: web/API chỉ mở trên `127.0.0.1`, PostgreSQL không mở cổng ra host và dữ liệu nằm trong named volume riêng `simtim-demo_demo_postgres_data`.

Nếu chỉ muốn dùng thử trực tuyến, mở [bản PWA trên Vercel](https://bach-hoa-sim-tim-demo.vercel.app/) theo [README gốc](../README.md#bản-demo-trực-tuyến). Bản đó dùng PostgreSQL Neon riêng; dữ liệu và mật khẩu demo không đồng bộ với Compose local.

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

1. Vào **Tồn kho & lô hàng**: kiểm tra mười sản phẩm mẫu đã seed, lô, tồn khả dụng, hạn dùng và biến động nguồn.
2. Với `stock` hoặc `manager`, vào **Nhận hàng**, chọn sản phẩm và nhà cung cấp còn hoạt động, nhập số lượng/giá/hạn hợp lệ, xác nhận phiếu. Đọc lại phiếu và tồn để kiểm tra lượng nhận được ghi một lần.
3. Với `manager` hoặc `admin`, mở **Báo cáo**. Báo cáo này đọc dữ liệu server; doanh thu ban đầu có thể bằng 0 cho tới khi checkout qua API.

Màn hình **Bán hàng** ở API mode đã nối quote → checkout CASH → hóa đơn trên server; có thể dùng để thử cùng một database Compose. [QA-02 closeout](../docs/testing/QA-02-closeout-2026-10-20.md) ghi bằng chứng xử lý sáu GAP; [báo cáo ngày 07/10](../docs/testing/QA-02-test-report.md) được giữ làm lịch sử.

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

Nếu muốn tiếp tục, nhập số tiền khách trả bằng VND nguyên, ít nhất bằng `grandTotal` vừa thấy. Gửi `expectedTotal` để API từ chối nếu giá đổi sau quote. Tạo một `Idempotency-Key` cho thao tác checkout; nếu mất phản hồi, tra hóa đơn hoặc gửi lại **cùng key và payload**, không tạo key mới cho cùng lượt bán.

```powershell
$cash = [long]::Parse((Read-Host 'Tiền mặt khách trả (VND)'))
$checkoutKey = [guid]::NewGuid().ToString()
$checkoutBody = @{ items = $items; expectedTotal = [long]$quote.grandTotal; cashAmount = $cash } | ConvertTo-Json -Depth 5
$checkoutHeaders = $headers.Clone()
$checkoutHeaders['Idempotency-Key'] = $checkoutKey
$invoice = Invoke-RestMethod "$base/api/v1/sales/checkout" -Method Post -Headers $checkoutHeaders -ContentType 'application/json' -Body $checkoutBody
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

### 5.1. Sao lưu đúng project đang chạy

Chạy tại thư mục gốc repository. Truyền cùng tên project và env file đã dùng khi `up` để không chạm vào bản demo khác:

```powershell
$sourceProject = 'simtim-demo' # thay bằng giá trị sau -p lúc khởi động
$envFile = 'infra/.env'
$dump = 'infra/backups/demo_checkpoint.sql'
pwsh -File scripts/backup-db.ps1 -ProjectName $sourceProject -EnvFile $envFile -OutFile $dump
```

### 5.2. Thử khôi phục trên project mới

Tạo tên project riêng; chỉ khởi động PostgreSQL, không dùng lại volume của bản nguồn. Script restore dùng `ON_ERROR_STOP=1` và một transaction nên SQL lỗi trả mã khác 0 và rollback:

```powershell
$restoreProject = 'simtim-restore-' + (Get-Date -Format yyyyMMddHHmmss)
docker compose -p $restoreProject --env-file $envFile -f infra/compose.demo.yml up -d --wait postgres
pwsh -File scripts/restore-db.ps1 -ProjectName $restoreProject -EnvFile $envFile -BackupFile $dump
docker compose -p $restoreProject --env-file $envFile -f infra/compose.demo.yml exec -T postgres psql -U simtim -d simtim -Atc 'select count(*) from sales.invoices'
```

Đối chiếu thêm số dư, `SUM(quantity_delta)`, hóa đơn/thanh toán và kết quả học trước/sau bằng SQL trên hai project. Khi đã lưu bằng chứng, dọn **chỉ project khôi phục vừa tạo** bằng `docker compose -p $restoreProject --env-file $envFile -f infra/compose.demo.yml down -v`. Không chạy `down -v` trên project nguồn.
