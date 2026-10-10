# MOB-01 stocktake consumer review

**Status:** Proposed — chưa phải wire contract Accepted
**Consumer owner:** TV4 — Lê Văn Chiến
**UI reviewer:** TV3 — Nguyễn Văn Thi
**Provider handshake:** cần TV2/TV4 chốt cùng SYN-02 trước khi nối thao tác ghi API

Tài liệu này ghi boundary mà giao diện MOB-01 cần để review; không tự chọn route, DTO hay permission cho provider. Nguồn phạm vi vẫn là backlog Sprint 3 và Issue #43.

## Phần đã có thể dùng từ INV-02

API mode được phép dùng ba API đọc tồn đã tích hợp để dựng giao diện theo store/lô:

- session cung cấp organizationId và storeId;
- danh sách tồn sản phẩm cung cấp tên, SKU và on-hand/available;
- danh sách lô cung cấp batchId, productId, số lô, lô nhà cung cấp, hạn và số lượng;
- HTTP 401 kết thúc phiên; HTTP 403 giữ phiên và hiển thị lỗi quyền.

MOB-01 hiện chỉ đọc các nguồn này. Nút gửi bị khóa và client không gọi endpoint kiểm kê chưa được duyệt.

## Khoảng trống phải chốt trước API write

| Chủ đề | Consumer cần | Trạng thái |
| --- | --- | --- |
| Đơn vị/precision | unitCode hoặc quy tắc scale theo sản phẩm; KG tối đa 3 chữ số thập phân | INV-02 response hiện chưa có |
| Snapshot xung đột | số hệ thống và baseVersion của đúng balance/lô tại lúc bắt đầu đếm | Chờ SYN-02 |
| Quyền | quyền đọc phiên và quyền gửi số đếm cho STOCK/MANAGER; server là nguồn quyết định | Chưa chốt tên permission |
| Lifecycle | cách mở/đọc phiên, gửi một dòng theo lô và đọc trạng thái lưu/chờ duyệt/conflict | Chưa chốt route/DTO |
| Idempotency | clientOperationId, quy tắc retry cùng payload và từ chối payload khác | Chờ SYN-01/SYN-02 |
| Lỗi | lỗi field có cấu trúc; phân biệt 401, 403, validation, conflict/version và lỗi tạm thời | Chờ provider review |
| Scope | mọi đọc/ghi lấy organization/store từ Bearer session, không tin scope trong body | Bắt buộc |

Consumer tối thiểu cần nhận diện được: lô, sản phẩm, đơn vị/scale, on-hand tại snapshot, version gốc, số thực tế, trạng thái và lỗi có thể khôi phục. Danh sách này là nhu cầu UI, không phải tên field wire đã Accepted.

## Điểm review và bằng chứng

- TV3 review bố cục mobile/desktop, validation đơn vị, loading/error/permission và copy trạng thái.
- TV2/TV4 chốt provider contract cùng consumer/provider tests trước khi tạo API stocktake adapter.
- Consumer mocks nằm ở apps/web/tests/api-stocktake.spec.ts; chúng chứng minh UI boundary, không chứng minh provider đã triển khai.
- Sau khi provider có sẵn, bổ sung live test API/PostgreSQL và ảnh API mode trước khi ghi bằng chứng hoàn tất tích hợp.
