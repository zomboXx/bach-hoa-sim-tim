# Năm vai trò và Chapter 0 — cập nhật 16/09/2026

Đây là phạm vi đã hiện thực trong bản Sprint 1, không phải tuyên bố hoàn thành tất cả nghiệp vụ của năm vai trò.

| Tài khoản mẫu | Vai trò | Màn hình vào | Chức năng hiện có |
|---|---|---|---|
| ADMIN001 | Admin / Chủ cửa hàng | Tổng quan | Toàn quyền trên API đã triển khai; danh mục, bán, nhận, tồn, kiểm kê/duyệt, khuyến mãi, báo cáo |
| KT001 | Kế toán | Không gian kế toán | Xem doanh số hóa đơn và tra cứu hóa đơn; không sửa danh mục, bán/nhận hoặc duyệt kiểm kê |
| QL001 | Quản lý cửa hàng | Trung tâm phê duyệt | Danh mục, nhà cung cấp, tồn, kiểm kê/duyệt chênh lệch, khuyến mãi, báo cáo |
| NV001 | Sales / Cashier | POS | Tìm tên, nhập/quét barcode kiểu bàn phím + Enter, giỏ, tính tiền, hóa đơn của mình |
| KHO001 | Warehouse | Nhận hàng | Tạo phiếu nhận/nhập, xem lô/tồn và biến động để xử lý hàng hóa |

Mật khẩu mẫu: `demo123`. Tất cả có mentor. Role nội bộ `stock` tương ứng Warehouse; không đổi khóa này để giữ lịch sử. Vai trò cấp cao Admin kế thừa các thao tác đã triển khai; Manager không mặc nhiên kiêm Sales hoặc Warehouse.

## POS hóa đơn trước — cập nhật 18/09/2026

Sales đăng nhập vào màn hình **Hóa đơn đang lập**, ban đầu không có dòng hàng. Tra tên rồi chọn kết quả, hoặc nhập barcode và Enter/nút Thêm. Một lần thêm tăng đúng 1; cùng sản phẩm gộp vào dòng hiện có. Không bày toàn bộ danh mục sản phẩm ở màn hình chính.

Mỗi dòng có đơn giá, số lượng nhập trực tiếp/nút ±, thành tiền và nút xóa. Ô số lượng chỉ nhận chữ số 0–9; ký tự khác, số âm, thập phân, số vượt giới hạn an toàn không thay đổi giá trị. Để trống tạm khi sửa không đổi hóa đơn; rời ô sẽ khôi phục số lượng hiện tại. Nhập 0 hoặc giảm từ 1 về 0 sẽ xóa dòng. Không tăng vượt tồn còn hạn; tổng tiền cập nhật theo số lượng và khuyến mãi hiện có. Máy chủ vẫn xác nhận lại khi thanh toán.

Nút **Quét mã bằng camera · Sắp có** chỉ mở thông báo, không xin quyền camera. Ý tưởng điện thoại cùng tài khoản quét vào hóa đơn máy tính đã được ghi nhận nhưng chưa triển khai camera, ghép phiên hóa đơn hay đồng bộ thời gian thực. Đăng nhập cùng tài khoản trên hai thiết bị hiện chưa chia sẻ hóa đơn nháp; nháp chỉ ở bộ nhớ trang và không lưu qua reload.

## Chốt quyền

Spring Security chặn API không được phép trước validation; controller kiểm tra quyền lần nữa. `/state` lọc dữ liệu theo vai trò: Sales chỉ thấy hóa đơn của mình, Warehouse không thấy hóa đơn, Kế toán không nhận danh mục/lô/phiếu kho. Không dựa vào ẩn menu để bảo vệ nghiệp vụ.

Theo giới hạn Warehouse chỉ lập nhập/xuất/điều chuyển, bản cập nhật này bỏ quyền lập kiểm kê của Warehouse. Kiểm kê offline hiện dành cho Manager/Admin. Queue IndexedDB cũ vẫn giữ theo tài khoản, không tự xóa hoặc chuyển chủ sở hữu; tài khoản đã mất quyền không được tự đồng bộ. Nhóm cần xử lý bàn giao riêng nếu tồn tại queue kho từ prototype trước. Chưa có quy tắc hai người độc lập: Manager hiện có thể duyệt phiếu do mình lập; UI không gọi đây là cơ chế four-eyes.

## Đã có màn hình nhưng chưa có nghiệp vụ ghi

- Kế toán: thu chi, công nợ, đối soát két; doanh số hiện có không phải lợi nhuận hoặc số dư két.
- Manager: duyệt hủy hóa đơn, trả/hoàn tiền, mở/đóng ca. Chưa có API thực thi; không cho nhân viên tự thực hiện các thao tác này.
- Warehouse: xuất hàng và điều chuyển; hiển thị chưa khả dụng, chưa thay đổi tồn.
- Admin: cấu hình, tài khoản, quản lý chuỗi; mới một cửa hàng mẫu, chưa multi-tenant/SaaS hoàn chỉnh.

## Chapter 0 hoàn chỉnh ở mức làm quen

Mentor hình tròn `?` → lobby 7 chương → Chapter 0 mở game Godot Web thật. Chapter 1–6 luôn hiện: **Nội dung này đang được cập nhật, vui lòng thử lại sau**.

Người chơi điều khiển nhân viên nam trên map đã cung cấp bằng WASD/mũi tên/nút cảm ứng, gặp mentor nữ, tới gần điểm sáng và nhấn E/nút tương tác. Sáu điểm: mentor, quầy thu ngân, kệ hàng, hàng lạnh, nhận hàng, giỏ hàng/lối vào. Có va chạm chân quầy/kệ và camera theo nhân vật.

Sau sáu điểm, mentor hỏi ba câu về hướng dẫn khách tính tiền, chuyển yêu cầu nhạy cảm cho quản lý, vị trí kiểm nhận hàng. Sai có giải thích và được thử lại; E/Escape không bỏ qua phần này. Chỉ hoàn thành sau sáu điểm và ba đáp án đúng; API kiểm tra chủ phiên và điều kiện hoàn thành. Kết quả được lưu riêng, không thay đổi hóa đơn/tồn thật. Đây là định hướng cửa hàng, chưa phải đánh giá nghiệp vụ sâu hoặc hệ thống chống gian lận game.

Khi mất mạng, UI báo chưa lưu và cho thử lại khi còn ở chương. Chưa hỗ trợ tiếp tục phiên chơi sau khi đóng tab/reload. Các chương nghiệp vụ tương lai bao gồm giao tiếp, bán hàng, chăm sóc kệ, nhận hàng, hạn dùng và xử lý phản hồi — không thu hẹp training thành nhập liệu kho.

Nguồn sprite, cách tạo alpha và prompt xem [ASSETS.md](../../apps/training-godot/ASSETS.md). Bản gốc giữ nguyên; file Web build sinh từ source, không phải game canvas tự viết trong PWA.
