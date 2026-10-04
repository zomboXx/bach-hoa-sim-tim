# Sim Tím Workspace · Prototype 02 (Sprint 0 baseline)

PWA tương tác đã được chốt làm baseline Sprint 0. Ứng dụng dùng Vue 3, TypeScript, Vite và IndexedDB để kiểm chứng luồng nghiệp vụ trước khi Sprint 1 kết nối API tập trung. Baseline không cam kết SaaS đa doanh nghiệp và không được xem là lớp bảo mật production.

## Chạy

Node.js 22.12 trở lên (hoặc Node 20.19+).

```powershell
cd apps/web
npm ci
npm run dev
```

Mở **http://127.0.0.1:5174**. Lệnh `npm run dev` không đăng ký Service Worker mới. Để kiểm tra production build và offline cache, chạy `npm run build`, sau đó `npm run preview` và mở **http://127.0.0.1:4174**. Nếu đổi mã nguồn khi đang xem preview, cần build lại và tải lại trang.

## Tài khoản demo

Mật khẩu chung: `demo123`.

| Tài khoản | Vai trò  | Trải nghiệm                                                                                        |
| --------- | -------- | -------------------------------------------------------------------------------------------------- |
| NV001     | Bán hàng | Bán hàng, hóa đơn, tra cứu sản phẩm/tồn, đào tạo                                                   |
| KHO001    | Hàng hóa | Nhận hàng, tra cứu nhà cung cấp, tồn kho, kiểm kê, đào tạo                                         |
| QL001     | Quản lý  | Các màn hình trên, thêm danh mục/sản phẩm/nhà cung cấp, thiết lập giảm giá, duyệt kiểm kê, báo cáo |

Danh mục được nhập khi thêm sản phẩm. Chưa có màn hình sửa/xóa danh mục hoặc quản trị tài khoản.

## Auth adapter FE-01 / BE-02 (đã tích hợp)

Mặc định `VITE_USE_API` không bật, giữ nguyên demo mode. FE-01 đã tích hợp qua PR #12 và dùng [contract session Accepted](../../contracts/AUTH_SESSION_REVIEW.md) của BE-02. Các luồng nhận, tồn, bán và báo cáo API mode thuộc [Sprint 2](../../docs/project/SPRINT_2_KICKOFF.md).

```powershell
$env:VITE_USE_API = 'true'
$env:VITE_ORGANIZATION_CODE = 'SIMTIM'
$env:VITE_STORE_CODE = 'MAIN'
$env:API_PROXY_TARGET = 'http://127.0.0.1:8080'
npm run dev
```

`VITE_*` được nhúng khi build, không đặt secret ở đó. Scope mặc định là `SIMTIM`/`MAIN`; `API_PROXY_TARGET` chỉ cấu hình proxy dev/preview, không nhúng vào client. Production cần reverse proxy cùng origin cho `/api`; Vite preview chỉ phục vụ kiểm thử local. Có thể chạy `npm run build` rồi `npm run preview` với cấu hình này để thử production bundle.

API gửi `POST /api/v1/auth/login` với `{ organizationCode, storeCode, username, password }`, nhận `{ accessToken, tokenType, expiresAt, session }`. Dùng tài khoản backend đã tạo, không dùng tài khoản demo `NV001`/`demo123`. Token opaque chỉ giữ trong bộ nhớ, không ghi storage, cookie, URL hoặc log. Reload quay lại form đăng nhập; demo mode vẫn khôi phục tài khoản trên cùng tab. `restoreSession()` chỉ gọi `GET /api/v1/auth/session` khi adapter đang giữ token; `POST /api/v1/auth/logout` dùng Bearer và thu hồi phiên server. Nếu thu hồi thất bại, giao diện vẫn đăng xuất và báo lỗi; phiên server còn tồn tại tới khi hết hạn hoặc được thu hồi.

API mode hiển thị danh mục theo `catalog.read`, nhận hàng theo `receipts.read`/`receipts.write`, tồn kho theo `inventory.read`, báo cáo theo `reports.read` và đào tạo theo `trainingEnabled`; role chỉ dùng để hiển thị, không tự cấp quyền thao tác. Backend luôn kiểm tra quyền độc lập.

## FE-02 · Nhận hàng và tồn kho qua API

FE-02 tách trong `src/modules/inventory/` theo ADR 0003: domain/validation thuần, API và demo adapter riêng, presentation không đọc trực tiếp IndexedDB hay tự gắn Bearer token. API mode khởi tạo trạng thái vận hành rỗng và chỉ hiển thị dữ liệu nhận/tồn do server trả về; lỗi API không fallback sang demo. Demo mode tiếp tục dùng IndexedDB và cùng giao diện mới để regression.

- Danh mục nhận hàng gọi `GET /api/v1/suppliers` và `GET /api/v1/products`, chỉ cho chọn record `ACTIVE`.
- Xác nhận gọi `POST /api/v1/inventory/receipts` với `Idempotency-Key` UUID ổn định, `clientOperationId`, số lượng dạng chuỗi tối đa ba chữ số thập phân và giá nguyên VND. Sau `201`, client đọc lại phiếu và ba API tồn sản phẩm/lô/biến động.
- Dấu thao tác đang gửi chỉ lưu trong `sessionStorage` của tab, không chứa Bearer token. Timeout, lỗi mạng hoặc `5xx` giữ nguyên payload, `Idempotency-Key` và `clientOperationId`; reload/đăng nhập lại chỉ GET để đối soát. Kết quả rỗng không tự tạo key hoặc tự POST. Người dùng phải chủ động thử lại cùng mã hoặc bỏ dấu thao tác. Nhận hàng không có offline queue.
- `401` xóa phiên/token trong bộ nhớ và về đăng nhập; `403` giữ màn hình và báo thiếu quyền. SALES chỉ có màn hình tồn nếu server cấp `inventory.read`; response tồn/lô/biến động không có giá nhập.
- Trạng thái hạn dùng `businessDate`/`expiryStatus` do server tính theo `Asia/Ho_Chi_Minh`. Màn hình đọc hết các trang dữ liệu, hiển thị riêng on-hand, available, trạng thái lô, mọi dòng của phiếu nhận và chứng từ nguồn của movement.

Recovery gọi đúng một `GET /api/v1/inventory/receipts?clientOperationId=...`. Backend lọc theo organization/store của Bearer session và trả danh sách rỗng hoặc đúng một phiếu; client không quét danh sách phân trang và không thể thấy phiếu của cửa hàng khác.

## Kịch bản trình diễn

1. Đăng nhập `QL001`. Dashboard bắt đầu với doanh thu 0 để không trộn số liệu giả vào giao dịch vừa tạo.
2. Nhận hàng: chọn nhà cung cấp, sản phẩm, mã lô mới, hạn còn hiệu lực; giao 20, chấp nhận 18 và ghi lý do từ chối 2 hộp. Lô và biến động tồn được lưu cùng lần ghi IndexedDB.
3. Bán hàng: tìm sản phẩm bằng tên/mã vạch, thêm giỏ, đổi số lượng, nhập tiền khách đưa hoặc chọn chuyển khoản mô phỏng. Thanh toán tạo hóa đơn, giữ giá sau khuyến mãi, trừ lô còn hạn theo FEFO và ghi biến động.
4. Xem hóa đơn chi tiết, tồn kho và báo cáo; lô hết hạn vẫn được theo dõi nhưng không được bán.
5. Kiểm kê: dùng nút “Thử mất mạng” hoặc DevTools Network Offline sau lần tải online đầu tiên. Lưu phiếu theo lô, tải lại trang để thấy phiếu được giữ trong IndexedDB. Khi kết nối lại, phiếu chuyển từ PENDING sang REVIEW (hoặc CONFLICT nếu tồn thay đổi). Chỉ quản lý duyệt REVIEW mới điều chỉnh tồn; phiếu duyệt rồi không được duyệt lại.
6. Đào tạo: nhấn avatar mentor để chuyển hẳn sang cửa hàng 2D. Bắt đầu ca, điều khiển nhân vật bằng WASD/phím mũi tên; đứng cạnh khách hoặc đồ vật rồi nhấn E. Có thể nhấp/chạm vào mục tiêu để nhân vật tự đi tới qua lối trống, hoặc dùng phím điều hướng cảm ứng trên điện thoại.
7. Trong ca chơi: chào hỏi khách Linh, xác nhận nhu cầu 2 hộp sữa ít đường; tới kệ chọn đúng hàng; tới quầy quét từng hộp, nhận 20.000đ và trả 3.000đ; kiểm tra tủ mát, mang sữa chua hết hạn sang khu xử lý và báo lại với Mai. Chọn sai sẽ nhận phản hồi tại chỗ, không tự bỏ qua bước. Sau ba kỹ năng có màn hình tổng kết và chơi lại.

## Đã hoạt động / đang mô phỏng

- **Hoạt động trong trình duyệt:** responsive, form và validation, dữ liệu demo nối xuyên suốt, IndexedDB, Service Worker cache giao diện, lưu kiểm kê offline và xử lý trạng thái phiếu; API mode có đăng nhập/session, nhận hàng, tồn/lô/hạn, biến động và báo cáo theo quyền server.
- **Đang mô phỏng:** demo mode vẫn mô phỏng đăng nhập, phân quyền, thanh toán, đồng bộ và lưu dữ liệu vận hành trên thiết bị. API mode không dùng các dữ liệu mô phỏng này làm fallback; các feature chưa có adapter server sẽ hiển thị trạng thái rỗng hoặc bị ẩn theo quyền.
- **Đào tạo:** trò chơi nhập vai 2D trên trình duyệt, dùng Vue/SVG để thử trải nghiệm trước khi tích hợp Godot. Có di chuyển, va chạm, tìm đường, hội thoại với NPC, giỏ cầm tay và chuỗi nhiệm vụ; không còn biểu mẫu nhập số lượng nhận hàng. Một ca liền mạch bao gồm giao tiếp, bán hàng và xử lý hàng hóa. Kết quả chỉ tồn tại trong phiên chơi; chưa lưu qua backend, chưa phải runtime Godot.
- **Chưa triển khai:** Godot thực tế, quét camera, xuất/in hóa đơn và quản lý sửa/xóa đầy đủ. Các phần này không được tính là hoàn thành bởi prototype/PWA hiện tại.

Quyền ở frontend chỉ giúp trình diễn; backend thật phải xác thực và kiểm tra quyền độc lập. Các tài khoản demo là công khai, không dùng dữ liệu cửa hàng thật ở đây. Giới hạn offline dành cho kiểm kê; các thao tác ghi khác bị chặn khi mất kết nối được phát hiện.

Để thử dữ liệu sạch, dùng một browser profile riêng hoặc xóa riêng IndexedDB `simtim-v2` của origin thử nghiệm. Việc xóa này làm mất hóa đơn/phiếu demo đã tạo trên thiết bị.

## Kiểm thử

```powershell
npm run verify
```

Kiểm thử dùng Google Chrome đã cài qua Playwright (`channel: chrome`), khởi chạy preview nếu cổng 4174 chưa được dùng. Có kiểm tra nhận hàng/bán hàng/FEFO, giá khuyến mãi, hóa đơn, reload offline, đồng bộ, duyệt tồn, cách ly đào tạo, đăng nhập sai và quyền giao diện.

`npm run test:e2e:api` tự build API mode và chạy cổng 4175 với HTTP mock theo DTO BE-02, INV-01 và INV-02. Consumer/E2E bao phủ form hợp lệ/sai, server field error, pending/double-click, 401/403, timeout/reload, giữ idempotency key, đọc lại tồn, SALES không thấy giá nhập và viewport mobile. Bộ này không thay thế kiểm thử kết nối với backend/PostgreSQL thật trước khi merge.

Từ root repository, `pwsh -File scripts/test-fe02-live.ps1` tạo PostgreSQL 17 disposable, chạy Spring API với mật khẩu demo sinh ngẫu nhiên và thực thi Playwright qua proxy thật. Test cho POST commit vào database nhưng response bị cắt ở trình duyệt, rồi reload/đăng nhập lại và xác nhận UI chỉ GET theo `clientOperationId`, tìm được phiếu và không POST lần hai. Script luôn dừng API, xóa container cùng credential tạm sau khi chạy.

## Tài liệu và cấu trúc

- `src/App.vue`: các màn hình quản lý và cổng chuyển sang khu đào tạo.
- `src/modules/inventory/`: domain, validation, demo/API adapter, dấu thao tác theo tab và giao diện nhận/tồn FE-02.
- `src/training/TrainingGame.vue`: phiên nhập vai riêng, NPC, hội thoại, hành động và nhiệm vụ; không import dữ liệu vận hành.
- `src/training/world.ts`: bố cục cửa hàng, kiểm tra va chạm, tìm đường tới mục tiêu.
- `src/training/PixelPerson.vue`, `src/training/training.css`: nhân vật pixel, chuyển động và giao diện game desktop/mobile.
- `src/api.ts`: kiểu dữ liệu, adapter demo, đăng nhập mẫu, giao dịch bán hàng và lưu trữ.
- `public/sw.js`: chỉ cache tài nguyên giao diện công khai cùng origin; không cache API nghiệp vụ.
- `ARCHITECTURE.md`: ranh giới client/backend/Godot và API contract dự kiến.
- `../../CHANGELOG.md`: lịch sử thay đổi của toàn repository.

Hình mentor trong `public/mentor.png` được kế thừa từ prototype đầu; nguồn lịch sử nằm tại `../../archive/prototype-v1/assets/mentor-mai.png`.
