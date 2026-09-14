# Sim Tím Workspace · Prototype 02

Bản giao diện tương tác theo hướng **hệ thống client–server triển khai tập trung, có khả năng phát triển theo mô hình SaaS**. Thử frontend Vue 3 + TypeScript + Vite; không khóa stack, phân công hoặc cam kết xây dựng SaaS đa doanh nghiệp.

## Chạy

Node.js 22.12 trở lên (hoặc Node 20.19+).

```powershell
cd prototype-v2
npm ci
npm run build
npm run preview
```

Mở **http://127.0.0.1:4174**. Preview dùng bản build, có Service Worker để thử offline. `npm run dev` dùng cổng 5174 cho phát triển giao diện, không đăng ký Service Worker mới. Nếu đổi mã nguồn khi đang xem preview, cần build lại và tải lại trang.

## Tài khoản demo

Mật khẩu chung: `demo123`.

| Tài khoản | Vai trò | Trải nghiệm |
| --- | --- | --- |
| NV001 | Bán hàng | Bán hàng, hóa đơn, tra cứu sản phẩm/tồn, đào tạo |
| KHO001 | Hàng hóa | Nhận hàng, tra cứu nhà cung cấp, tồn kho, kiểm kê, đào tạo |
| QL001 | Quản lý | Các màn hình trên, thêm danh mục/sản phẩm/nhà cung cấp, thiết lập giảm giá, duyệt kiểm kê, báo cáo |

Danh mục được nhập khi thêm sản phẩm. Chưa có màn hình sửa/xóa danh mục hoặc quản trị tài khoản.

## Kịch bản trình diễn

1. Đăng nhập `QL001`. Dashboard bắt đầu với doanh thu 0 để không trộn số liệu giả vào giao dịch vừa tạo.
2. Nhận hàng: chọn nhà cung cấp, sản phẩm, mã lô mới, hạn còn hiệu lực; giao 20, chấp nhận 18 và ghi lý do từ chối 2 hộp. Lô và biến động tồn được lưu cùng lần ghi IndexedDB.
3. Bán hàng: tìm sản phẩm bằng tên/mã vạch, thêm giỏ, đổi số lượng, nhập tiền khách đưa hoặc chọn chuyển khoản mô phỏng. Thanh toán tạo hóa đơn, giữ giá sau khuyến mãi, trừ lô còn hạn theo FEFO và ghi biến động.
4. Xem hóa đơn chi tiết, tồn kho và báo cáo; lô hết hạn vẫn được theo dõi nhưng không được bán.
5. Kiểm kê: dùng nút “Thử mất mạng” hoặc DevTools Network Offline sau lần tải online đầu tiên. Lưu phiếu theo lô, tải lại trang để thấy phiếu được giữ trong IndexedDB. Khi kết nối lại, phiếu chuyển từ PENDING sang REVIEW (hoặc CONFLICT nếu tồn thay đổi). Chỉ quản lý duyệt REVIEW mới điều chỉnh tồn; phiếu duyệt rồi không được duyệt lại.
6. Đào tạo: mở nút mentor, kiểm tra 20 hộp sữa, nhập sai 20 để xem phản hồi, sửa thành 18 để hoàn thành. Mọi số liệu của bài thực hành được giữ riêng trong bộ nhớ; không ghi vào tồn hoặc hóa đơn.

## Đã hoạt động / đang mô phỏng

- **Hoạt động trong trình duyệt:** responsive, form và validation, dữ liệu demo nối xuyên suốt, IndexedDB, Service Worker cache giao diện, lưu kiểm kê offline và xử lý trạng thái phiếu; dialog có quản lý focus native.
- **Đang mô phỏng:** đăng nhập, phân quyền, lớp API, xác nhận thanh toán, đồng bộ và lưu dữ liệu vận hành. Không có REST server đang chạy. Dữ liệu chỉ ở thiết bị/browser profile hiện tại, chưa hỗ trợ nhiều tab cùng chỉnh sửa hoặc nhiều thiết bị dùng chung dữ liệu.
- **Đào tạo:** một bản thử tương tác bằng Vue/CSS, chưa phải runtime Godot. Chưa lưu kết quả đào tạo. Không làm tiếp danh sách 7 chapter của bản cũ.
- **Chưa triển khai:** Spring Boot/Spring Security, PostgreSQL/Flyway, API thật, Godot thực tế, quét camera, xuất/in hóa đơn, quản lý sửa/xóa đầy đủ. Đó là các phần của kế hoạch cuối kỳ, không được tính là đã hoàn thành bởi prototype này.

Quyền ở frontend chỉ giúp trình diễn; backend thật phải xác thực và kiểm tra quyền độc lập. Các tài khoản demo là công khai, không dùng dữ liệu cửa hàng thật ở đây. Giới hạn offline dành cho kiểm kê; các thao tác ghi khác bị chặn khi mất kết nối được phát hiện.

Để thử dữ liệu sạch, dùng một browser profile riêng hoặc xóa riêng IndexedDB `simtim-v2` của origin thử nghiệm. Việc xóa này làm mất hóa đơn/phiếu demo đã tạo trên thiết bị.

## Kiểm thử

```powershell
npm run build
npm run test:e2e
```

Kiểm thử dùng Google Chrome đã cài qua Playwright (`channel: chrome`), khởi chạy preview nếu cổng 4174 chưa được dùng. Có kiểm tra nhận hàng/bán hàng/FEFO, giá khuyến mãi, hóa đơn, reload offline, đồng bộ, duyệt tồn, cách ly đào tạo, đăng nhập sai và quyền giao diện.

## Tài liệu và cấu trúc

- `src/App.vue`: các màn hình và trạng thái tương tác prototype.
- `src/api.ts`: kiểu dữ liệu, adapter demo, đăng nhập mẫu, giao dịch bán hàng và lưu trữ.
- `public/sw.js`: chỉ cache tài nguyên giao diện công khai cùng origin; không cache API nghiệp vụ.
- `ARCHITECTURE.md`: ranh giới client/backend/Godot và API contract dự kiến.
- `CHANGELOG.md`: bàn giao và giới hạn của bản này.

Hình mentor tái sử dụng từ `../assets/mentor-mai.png`; không phát sinh asset ImageGen mới trong bản 02.
