# Camera và giỏ POS dùng chung

**Trạng thái: Draft — phạm vi do Project Owner yêu cầu ngày 20/10/2026; triển khai và kiểm thử cục bộ, chưa tích hợp `main`.** Đây là phần mở rộng sau Sprint 4, không thay tên/mã các backlog item đã chốt.

## Hành vi

Điện thoại và máy tính đăng nhập **cùng nhân viên, tổ chức, cửa hàng và cùng API** dùng một giỏ đang bán. Nhân viên khác hoặc cửa hàng khác có giỏ riêng. Chọn sản phẩm, nhập mã rồi Enter và camera đều đi qua cùng thao tác thêm hàng. Giỏ là bản nháp; chỉ checkout mới ghi hóa đơn, thanh toán và tồn.

Camera dùng `@zxing/browser`, được Project Owner đồng ý bổ sung ngày 20/10. Chỉ mở khi người dùng bấm nút và cấp quyền; ưu tiên camera sau, cần HTTPS hoặc localhost. Ảnh giải mã trên thiết bị, không upload. Đọc được một mã thì dừng nhận mã cho đến khi bấm **Quét tiếp**; đóng/rời màn hình/tắt tab thì dừng camera. Chuỗi mã được giữ nguyên, không ép thành số. Bộ ảnh mẫu dùng **Code 128** để mã số và mã chữ đều được biểu diễn chính xác.

Dữ liệu mã đã có từ V3: `catalog.product_barcodes.barcode varchar(100)`, unique theo tổ chức; một sản phẩm có thể có nhiều mã. API tra cứu hiện hành `GET /api/v1/products/search?barcode=...` dùng khớp chính xác. Bổ sung mã demo `ST-APPLE-01` cho táo; giữ mã gạo `8930000000001`. Chuỗi gạo không đổi sang EAN-13.

## Ranh giới API

Mọi route dùng Bearer và scope lấy từ session. `sales.write` là quyền cần cho đọc/sửa giỏ đang bán; STOCK chỉ có quyền đọc hóa đơn không được dùng giỏ.

| Thao tác | Route / field | Kết quả |
|---|---|---|
| Đọc giỏ | `GET /api/v1/sales/cart` | `{id, version, generation, items:[{productId, productName, quantity}]}` |
| Đọc có điều kiện | `If-None-Match` với ETag lần trước | `304` khi không đổi; `Cache-Control: no-store` |
| Thêm/bớt lượng | `POST /api/v1/sales/cart/operations` | Body `{operationId: UUID, generation, productId, delta: decimal-string}`; trả snapshot mới |
| Checkout | Route hiện hành `POST /api/v1/sales/checkout` | Thêm `cartVersion` tùy chọn; giỏ chung bắt buộc key, tổng, items khớp snapshot |
| Lỗi thay giỏ | `409 CART_CHANGED` | Tải giỏ/báo giá mới, yêu cầu nhân viên xác nhận lại |
| Lỗi dùng lại key | `409 IDEMPOTENCY_KEY_REUSED` | Cùng key nhưng khác payload bị từ chối |

`cartVersion` vắng mặt giữ đường checkout cũ để tương thích client cũ. Client mới luôn gửi field này khi thanh toán giỏ chung. Không sử dụng đường cũ để thanh toán lại một giỏ chung trên thiết bị khác.

## Đồng bộ và transaction

Mỗi thao tác gửi lên ngay; thiết bị còn lại đọc snapshot/ETag cách 500 ms khi màn bán hàng mở. Lỗi mạng chờ 2,5 giây trước lần đọc tiếp. Đây là đồng bộ gần thời gian thực, độ trễ phụ thuộc mạng và thời gian xử lý, không cam kết cập nhật tức thì tuyệt đối. Không dựa vào event trong RAM, localStorage hay một instance API cụ thể.

V17 tạo `pos_carts`, `pos_cart_lines`, `pos_cart_operations`. Một cart duy nhất theo organization/store/owner. Khóa hàng PostgreSQL tuần tự hóa mutation/checkout. Delta giữ được hai lượt thêm từ hai thiết bị; không ghi đè toàn bộ giỏ bằng bản cũ.

Mỗi thay đổi có UUID và hash payload; retry trả snapshot hiện hành mà không cộng lại. Thao tác chưa rõ kết quả được giữ ở sessionStorage theo scope và khóa thao tác mới đến khi retry; token không được lưu ở đây. `generation` tăng khi checkout để từ chối lượt quét muộn thuộc đơn đã thanh toán.

Checkout kiểm phiên bản và items, tính giá/tồn/FEFO ở server, tạo hóa đơn, xóa các dòng giỏ và lưu receipt thao tác trong một transaction. Hai key khác nhau cùng phiên bản chỉ một request thành công. Retry key thành công cũ trả lại hóa đơn, không xóa giỏ mới. Thất bại về giá/tồn rollback cả nghiệp vụ và giỏ.

## Kiểm chứng

- `Qa02E2eTest`: hai session một nhân viên, actor/store isolation, ETag, precision, concurrent delta, checkout stale, concurrent checkout, replay sau đơn mới và rollback.
- `barcode-camera.spec.ts`: giải mã pixel của cả bốn PNG bằng decoder thật; từ chối quyền camera vẫn nhập tay được; đóng camera dừng stream.
- `api-sale.spec.ts`: hồi quy quote/checkout/invoice và retry.
- `pwsh -File scripts/test-pos-live.ps1`: PostgreSQL 17 disposable, hai browser context desktop/mobile cùng nhân viên, camera stream giả lập chứa ảnh mã vạch thật, nhập tay, thao tác đồng thời, mất phản hồi sau commit/retry, checkout và SQL oracle.
- CI mở rộng live matrix để chạy cùng bài POS trên PostgreSQL sạch.

Camera vật lý trên Android/iPhone và trải nghiệm qua mạng điện thoại vẫn cần người dùng thử trên HTTPS sau khi bản cập nhật được triển khai.

## Bằng chứng local ngày 20/10/2026

Windows, JDK 25 (biên dịch target Java 21), Docker Desktop, PostgreSQL 17.11 disposable:

- Gate gốc `pwsh -File scripts/verify.ps1 -SkipInstall` đạt: policy/link/lint/format/build, demo 39/39, API consumer 58/58 và backend 189/189. `Qa02E2eTest` đạt 18/18, gồm bảy ca giỏ chung mới.
- Camera giải mã đúng bốn PNG bằng decoder thật và ca từ chối quyền vẫn cho nhập tay: 5/5.
- Live POS cuối đạt 1/1: hai session desktop/mobile, camera → giỏ máy khác quan sát sau 1.290 ms (gồm thời gian mở camera), nhập tay, concurrent delta, lost-response/retry và checkout. SQL oracle: receipt +0, invoice +1, payment +1, SALE movement +1, revenue +112.500 VND, balance mismatch 0.
- Live GAP-05 chạy lại sau thay đổi checkout đạt 2/2, gồm HTTP 502 sau commit và retry cùng key/payload. Oracle: invoice/payment/SALE movement mỗi loại +2 cho hai lượt bán, revenue +80.000 VND, balance mismatch 0.
- CI được cấu hình thêm suite `pos`; chưa có bằng chứng GitHub Actions cho nhánh chưa push.

Chạy lại bộ ảnh bằng `python scripts/generate-demo-barcodes.py` (Pillow và ReportLab là công cụ sinh ảnh, không thuộc dependency production). Các PNG/SVG đã nằm trong `apps/web/public/demo-barcodes/`; ảnh được kiểm chứng đọc đúng chuỗi, không đổi checksum hay ép mã sang số.
