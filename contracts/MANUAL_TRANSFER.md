# QR chuyển khoản và xác nhận thủ công

Trạng thái: phạm vi được Project Owner Nguyễn Đức Phát duyệt ngày 22/10/2026; triển khai cục bộ, chưa tích hợp hoặc triển khai lên môi trường dùng chung.

Quyết định hiện hành thay phần chuẩn bị payOS: không cần tài khoản merchant, API Key hay xác thực NFC để tạo QR chuyển khoản. Server không kết nối ngân hàng và không tự xác nhận tiền về. Phạm vi CASH của các sprint trước là lịch sử; bổ sung này không thay kết quả nghiệm thu cũ.

## Dữ liệu và vòng đời

- Một giao dịch gắn một giỏ của nhân viên/cửa hàng, một hóa đơn `DRAFT`, số tiền VND nguyên dương, mã nội dung riêng, thông tin người nhận tại lúc tạo và hạn 10 phút.
- Server khóa giỏ, xác nhận phiên bản/tổng tiền và chụp giá, khuyến mãi, phân bổ FEFO. `reserved_quantity` tăng; tồn vật lý, movement và doanh thu chưa thay đổi.
- `PENDING → COMPLETED`: nhân viên kiểm tra thực nhận, nhập số tiền bằng đúng tổng và mã giao dịch ngân hàng, đánh dấu đã kiểm tra. Server ghi người/thời gian xác nhận, tạo payment `TRANSFER`, hoàn tất hóa đơn, chuyển giữ hàng thành xuất bán và xóa giỏ trong cùng transaction.
- `PENDING → CANCELLED/EXPIRED`: giải phóng giữ hàng, hóa đơn `VOIDED`; giữ lại sản phẩm trong giỏ và lịch sử giao dịch. Không ghi payment hoàn tất, movement bán hay doanh thu.
- Trạng thái kết thúc không mở lại. Không cộng tiền, trừ hàng hoặc xóa một giỏ mới khi retry giao dịch cũ.
- Lô bị khóa/hết hạn hoặc sản phẩm ngừng bán sẽ chặn xác nhận và rollback toàn bộ. Nhân viên cần đối chiếu khoản tiền và xử lý nghiệp vụ trước, không tự đổi lô sau khi QR đã chốt giá.
- Kiểm kê vẫn đếm tồn vật lý. Không được duyệt tồn thấp hơn số đang giữ; mọi thay đổi giữ hàng tăng version để kiểm kê phát hiện xung đột.

V19 thêm bảng `sales.transfer_orders` và số lượng giữ trên balance; V18 ảnh sản phẩm trên `main` được giữ nguyên. Thông tin tài khoản, giá và lựa chọn lô là snapshot giao dịch; không suy ra cấu hình hiện hành từ lịch sử. Tác vụ hết hạn chạy mỗi 30 giây; đọc giỏ/chi tiết cũng kiểm tra hết hạn. Khi server dừng, lịch sử/giữ hàng vẫn tồn tại trong PostgreSQL; lúc hoạt động lại sẽ giải phóng giao dịch quá hạn.

## HTTP và quyền

Mọi đường dưới đây dùng session Bearer và phạm vi tổ chức/cửa hàng từ server. Không nhận actor hoặc tài khoản nhận tiền từ client.

| Đường | Nội dung |
|---|---|
| `GET /api/v1/sales/transfers/config` | Trả cấu hình khả dụng của cửa hàng, không trả tài khoản khi chưa cấu hình hợp lệ |
| `POST /api/v1/sales/transfers` | `{operationId: UUID, cartVersion, expectedTotal}`; lấy sản phẩm từ giỏ server |
| `GET /api/v1/sales/transfers/{id}` | Chi tiết, trạng thái và `qrPayload` chỉ khi còn chờ |
| `POST /api/v1/sales/transfers/{id}/confirm` | `{receivedAmount, bankReference, checkedBankReceipt:true}` |
| `POST /api/v1/sales/transfers/{id}/cancel` | `{reason}` |
| `GET /api/v1/sales/transfers?page=0&size=20` | Lịch sử phân trang và tổng số chờ/đã nhận/hủy/hết hạn, tổng VND đã nhận của toàn bộ phạm vi được xem |

Thu ngân chỉ đọc/xử lý giao dịch của mình. MANAGER/ADMIN được đọc/xử lý của nhân viên trong cửa hàng hiện tại. Các POST yêu cầu `sales.write`; STOCK không xác nhận được. Truy cập sai chủ/cửa hàng/tổ chức trả 404 hoặc bị auth filter từ chối. Xác nhận luôn lưu người thực hiện thực tế, không đổi người bán của hóa đơn.

Tạo QR dùng `operationId` cố định qua mất kết nối/reload; đổi version/tổng khi dùng lại mã trả 409. Xác nhận lặp cùng số tiền/mã ngân hàng trả giao dịch cũ; mã ngân hàng được chuẩn hóa chữ hoa/khoảng trắng, không dùng cho hai đơn cùng tổ chức/tài khoản nhận. Cơ sở dữ liệu và khóa transaction bảo vệ cả yêu cầu đồng thời. API trả `Cache-Control: no-store`.

Giỏ chung thêm `pendingTransferId`; version tăng khi tạo hoặc xử lý QR. Thiết bị khác đọc được khóa giỏ qua polling hiện có. Tải lại trang cần đăng nhập lại theo cơ chế session hiện hành; giao dịch vẫn khôi phục từ server. Đơn 0 VND dùng CASH, không tạo QR.

## QR và giới hạn

QR mã hóa tại server theo TLV NAPAS247 đến tài khoản (`A000000727`, `QRIBFTTA`, tiền tệ `704`, nội dung trường `62/08`, CRC-16/CCITT-FALSE). Web dùng bộ tạo SVG của thư viện quét mã đã có, không gửi ảnh hoặc thông tin giao dịch đến dịch vụ tạo QR bên ngoài. Vector công khai được đối chiếu với [vietnam-qr-pay](https://github.com/xuannghia/vietnam-qr-pay).

Ứng dụng lưu các giao dịch do nhân viên tạo/xác nhận, không đọc sao kê và không biết tổng số chuyển khoản thực tế vào tài khoản. Khách có thể đổi tiền/nội dung trong app ngân hàng; nhân viên phải kiểm tra. QR hết hạn/hủy không chặn ngân hàng chuyển tiền tới tài khoản, nên tiền đến muộn cần đối chiếu riêng. Không tự hoàn tiền, nhận thiếu/thừa, chia nhiều lần thanh toán hoặc xác minh chứng từ ngân hàng. Hai nội dung nhập sai khác nhau không thể được hệ thống tự phát hiện là cùng một khoản tiền.

Xem [cấu hình và kiểm thử](../docs/testing/MANUAL_TRANSFER_SETUP.md).
