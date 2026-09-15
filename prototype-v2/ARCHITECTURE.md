# Kiến trúc và hợp đồng tích hợp dự kiến

Ngày cập nhật: 2026-09-14. Theo định hướng người dùng vừa chốt; stack và phân công vẫn là đề xuất, chưa xác nhận năng lực của cả bốn thành viên.

## Kiến trúc mục tiêu

```text
PWA responsive ────────┐
                      ├── REST + JSON ── Backend nghiệp vụ ── CSDL quan hệ tập trung
Godot đào tạo 2D ──────┘                  ├─ xác thực / phân quyền
                                         ├─ dịch vụ bán hàng, kho, kiểm kê
                                         └─ phiên và dữ liệu đào tạo cách ly
```

Một doanh nghiệp mẫu, một cửa hàng. Không suy diễn thành nhiều tenant hoặc nhiều chi nhánh đồng thời. Gọi chung backend API có nghĩa là dùng chung quy tắc nghiệp vụ, không có nghĩa là đào tạo được ghi vào bảng tồn/hóa đơn vận hành. Trên server, phiên đào tạo phải có quyền và phạm vi dữ liệu riêng; không chỉ tin cờ `training` từ client.

## Hiện thực của prototype

PWA → adapter TypeScript → IndexedDB trên một thiết bị. Các thao tác cập nhật sao chép trạng thái, kiểm tra điều kiện, sau đó ghi trong một transaction IndexedDB; giao diện chỉ nhận trạng thái mới khi ghi thành công. Không có Java server, PostgreSQL, quản lý tenant hoặc Godot đã tích hợp trong repo này.

Adapter hiện là mô phỏng giao diện, không thể làm nguồn dữ liệu trung tâm hoặc lớp bảo mật. Trong triển khai thật, giá, tồn, quyền, FEFO, tổng tiền và giao dịch phải do backend quyết định. Giao diện không được gửi giá/tổng tiền để server tin dùng. Validation hiện nằm cả ở handler giao diện và adapter; cần di chuyển các quy tắc sang service backend khi thực hiện, giữ validation giao diện để phản hồi nhanh.

## API contract bản nháp (chưa có endpoint chạy)

Tiền tệ: số nguyên VND. Thời gian sự kiện: ISO 8601 có timezone. Hạn sử dụng: YYYY-MM-DD theo múi giờ cửa hàng. IDs do server cấp, trừ `clientOperationId` do client tạo để chống lặp. Lỗi trả `{ code, message, fieldErrors? }`.

| Phương thức / đường dẫn | Vai trò | Đầu vào / kết quả chính |
| --- | --- | --- |
| POST /api/auth/login | Chưa đăng nhập | username/password → phiên; cơ chế cookie/token chốt khi tích hợp |
| POST /api/auth/logout | Đã đăng nhập | Thu hồi phiên |
| GET /api/me | Đã đăng nhập | ID, tên, role, quyền |
| GET /api/products?query= | Bán hàng, hàng hóa, quản lý | Tên, mã, mã vạch, danh mục, đơn vị, giá công bố |
| POST /api/products | Quản lý | Dữ liệu sản phẩm; UNIQUE barcode/code |
| GET /api/categories | Nhân viên | Danh mục đang hoạt động |
| POST /api/categories | Quản lý | Tên danh mục |
| GET /api/suppliers | Hàng hóa, quản lý | Đối tác và thông tin liên hệ |
| POST /api/suppliers | Quản lý | Tạo nhà cung cấp |
| POST /api/goods-receipts | Hàng hóa, quản lý | Supplier, dòng hàng, mã lô, hạn, giao/nhận/từ chối, lý do; transaction tăng tồn |
| GET /api/inventory/batches | Nhân viên | Tồn lô, hạn, trạng thái, version |
| GET /api/inventory/movements | Hàng hóa, quản lý | Biến động theo sản phẩm/lô/chứng từ |
| POST /api/invoices | Bán hàng, quản lý | clientOperationId, productId/quantity, paymentMethod, amountTendered; server tính giá/giảm, chọn lô, transaction hóa đơn + thanh toán + trừ tồn |
| GET /api/invoices/{id} | Bán hàng, quản lý | Chi tiết hóa đơn, giá lưu tại giao dịch |
| POST /api/promotions | Quản lý | Phạm vi sản phẩm, thời gian, tỷ lệ; chưa chốt giảm theo từng lô |
| GET /api/reports/sales?from=&to= | Quản lý | Doanh thu và hóa đơn theo thời gian |
| GET /api/reports/inventory | Quản lý | Tồn sản phẩm/lô, cận hạn/hết hạn |
| POST /api/stock-counts | Hàng hóa, quản lý | clientOperationId, batchId, baseVersion, actualQuantity, countedAt, note |
| POST /api/stock-counts/{id}/approve | Quản lý | Kiểm tra version → transaction điều chỉnh và biến động; 409 nếu xung đột |
| POST /api/training/sessions | Người học | scenarioId → ID phiên cách ly + quyền truy cập ngắn hạn |
| GET /api/training/sessions/{id} | Chủ phiên / quản lý được cấp quyền | Mục tiêu, bước hiện tại, dữ liệu luyện tập |
| POST /api/training/sessions/{id}/actions | Chủ phiên | actionId, sequence, actionType, payload; server kiểm tra nghiệp vụ trong sandbox |
| POST /api/training/sessions/{id}/complete | Chủ phiên | Server đánh giá sự kiện hợp lệ, trả kết quả và phản hồi |

## Offline giới hạn: kiểm kê

```text
Đếm lô → lưu IndexedDB (PENDING)
       → có mạng → gửi cùng clientOperationId
       → server khử trùng lặp và kiểm tra baseVersion
       → REVIEW → quản lý duyệt → APPROVED + biến động tồn
       └─ CONFLICT → kiểm lại; không tự ghi đè tồn mới
```

Prototype dùng expected quantity để phát hiện thay đổi, không có version máy chủ. Khi triển khai phải dùng version vì tồn có thể thay đổi rồi trở lại cùng một số lượng. Nút đồng bộ chỉ chuyển phiếu vào vùng dữ liệu demo cùng thiết bị, không gửi tới máy chủ. Phiếu PENDING có ID ổn định và chỉ chuyển trạng thái một lần; server thật phải bổ sung UNIQUE cho clientOperationId, xử lý retry và transaction.

Service Worker chỉ lưu giao diện/tài nguyên công khai; không biến các API bán hàng thành cache offline. Cần lần tải thành công khi có mạng; môi trường triển khai PWA thật cần HTTPS. Tài khoản mới không đăng nhập offline; phiên demo còn lưu trên cùng tab có thể mở lại giao diện để kiểm kê.

## Godot và một kịch bản bắt buộc

Đã điều chỉnh theo phản hồi người dùng: đào tạo phải là nhập vai trong cửa hàng 2D, không phải biểu mẫu nghiệp vụ đặt cạnh hình minh họa. Prototype hiện có ca “Ca đầu tiên ở Sim Tím” với ba nhóm kỹ năng:

1. Di chuyển đến khách, chào hỏi, lắng nghe và xác nhận nhu cầu.
2. Tới kệ chọn đúng hàng, mang đến quầy, quét từng món và trả đúng tiền thừa.
3. Phát hiện hàng hết hạn tại tủ mát, mang tới khu xử lý, gắn nhãn và bàn giao cho mentor.

`TrainingGame.vue` quản lý trạng thái phiên riêng gồm vị trí, bước nhiệm vụ, hội thoại, đồ vật đang cầm và kết quả; `world.ts` phụ trách va chạm và tìm đường trên lưới. Chỉ nhân vật ở ô kề mới tương tác được; nhấp mục tiêu khiến nhân vật đi qua các ô trống trước khi mở hội thoại. Dùng WASD/phím mũi tên + E, hoặc thao tác chạm. Module không import adapter nghiệp vụ hay ghi IndexedDB của cửa hàng, vì vậy thao tác luyện tập không làm thay đổi dữ liệu vận hành.

Đây là game 2D chơi được trong trình duyệt để kiểm chứng trải nghiệm, triển khai bằng Vue/SVG. Khi tích hợp Godot, chuyển các scene/nhân vật/điều khiển sang engine; backend đánh giá hành động theo service nghiệp vụ trong phạm vi phiên riêng. Chưa có Godot runtime hoặc lưu kết quả qua backend trong bản này; không coi đó là đã hoàn thành phần Godot cuối kỳ.

Không tự thêm chapter thứ hai hoặc tuyến cốt truyện. Camera, lưu kết quả và offline production còn thuộc phần mở rộng cần ưu tiên sau.

## Stack và phân công

- Frontend đang thử: Vue 3, Vite, TypeScript, CSS thuần, Service Worker, IndexedDB.
- Backend ứng viên: Java Spring Boot + Security + Data JPA; PostgreSQL + Flyway. Có thể đổi NestJS khi nhóm xác nhận phù hợp kỹ năng JavaScript hơn Java.
- Trưởng nhóm: yêu cầu, API contract, tích hợp/review, Godot–backend.
- Thành viên 2: dữ liệu và backend sản phẩm/lô/nhập/tồn.
- Thành viên 3: giao diện desktop, bán hàng/hóa đơn/khuyến mãi/báo cáo.
- Thành viên 4: giao diện kiểm hàng responsive, hỗ trợ kịch bản Godot; offline và bài bổ sung không dồn vào phần bắt buộc.

Mỗi phần backend/CSDL cần có ít nhất một người phụ trách và một người review để tránh chỉ một người hiểu hệ thống. Chưa gán tên thành viên, chưa khóa phiên bản triển khai và chưa xác nhận năng lực/thời lượng.
