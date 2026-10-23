# Khôi phục phiên và giảm truy vấn xác thực — 23/10/2026

Owner: TV1 — Nguyễn Đức Phát. Project Owner yêu cầu sửa thao tác online chậm và phải đăng nhập lại khi F5. Bản ghi dưới đây chụp kết quả cục bộ trước push trên `codex/session-performance`; sau đó Project Owner cho phép push/merge/triển khai khi CI đạt. Trạng thái tích hợp/production theo PR, không suy từ thời điểm chạy cục bộ.

## Đối chiếu trước sửa

- Đo bằng `curl.exe` từ máy làm việc trên domain production: trang tĩnh 0,206 giây; ba lượt health 0,266–0,995 giây; hai lượt session không Bearer trả 401 trong 0,994–1,063 giây. Đây là số đo tại một thời điểm, không phải benchmark đăng nhập hoặc phân vị toàn hệ thống.
- Trong code, request có phiên hợp lệ thực hiện bốn lượt đọc nối tiếp: phiên, người dùng, vai trò và quyền. Đăng nhập thêm tìm tài khoản, BCrypt và ghi phiên; đăng xuất thêm cập nhật thu hồi. Chi phí đi lại tới CSDL có thể cộng dồn. Chưa có số đo tách riêng thời gian DB, cold start hoặc wake-up Neon trên production.
- Token chỉ ở bộ nhớ JS và marker API bị xóa sau login; tải lại trang vì thế không có cách khôi phục phiên theo baseline ngày 29/09.

## Thay đổi

- Gộp xác thực vào một câu SQL, kiểm tài khoản/tổ chức/cửa hàng, thời hạn/thu hồi, vai trò và quyền hiện hành. Không dùng cache quyền, không hạ BCrypt cost 12.
- Login PWA opt-in cookie HttpOnly/Secure/SameSite Strict, host-only, path `/api/v1/auth/restore`; `POST` bootstrap cần header riêng và kiểm Fetch Metadata. CORS vẫn chặn. API nghiệp vụ chỉ nhận Bearer; không lưu token vào localStorage/sessionStorage.
- Khôi phục grant cũ với thời hạn lưu trong DB, không gia hạn hoặc tạo thêm phiên. Đăng xuất thu hồi phiên và xóa cookie. Marker theo tab không phải bằng chứng xác thực; phản hồi cũ không được ghi đè login/logout mới.
- Yêu cầu auth có giới hạn chờ 15 giây và thông báo lỗi; lỗi mạng khôi phục không xóa hint để người dùng thử lại bằng F5.

## Kiểm chứng

- `mvnw.cmd -Dtest=ApiBootstrapTest test` sau phần giảm truy vấn: 11/11 đạt.
- `npm run lint`, `npm run build` và `npm run test:e2e:api`: đạt, consumer 70/70; bổ sung ca F5 giữ phiên, hết hạn, lỗi mạng và bootstrap trả về trễ.
- Kiểm tra mới trên PostgreSQL: thuộc tính cookie, expiry không kéo dài (sai số lưu timestamp dưới 1 ms), logout/expiry/role removal; cookie không tự xác thực API nghiệp vụ; thiếu header, cross-site/same-site khác origin, preflight và cookie trùng đều bị từ chối.
- `pwsh -File scripts/verify.ps1 -SkipInstall`: đạt policy/liên kết/lint/format/build, demo 51/51, consumer 70/70, backend PostgreSQL 213/213; trong đó auth 14/14. JDK 25 compile target Java 21.
- `pwsh -File scripts/test-transfer-live.ps1` với `SIMTIM_LIVE_TEST_PORT` là cổng trống riêng: 1/1 đạt. Cookie thật có HttpOnly/Secure/SameSite Strict; điện thoại F5 khôi phục đúng giao dịch chờ; máy tính logout xóa cookie và F5 không vào lại; điện thoại F5 tiếp tục được vì có phiên riêng.
- SQL oracle của bài live: một hóa đơn/payment/SALE movement, doanh thu +22.500 VND, không còn hàng giữ hoặc lệch ledger. Dùng tài khoản nhận tiền kiểm thử, không chuyển tiền thật.
- Lượt live đầu dừng trước test vì cổng 4176 đã được dùng. Thêm `SIMTIM_LIVE_TEST_PORT` và strictPort để chạy cổng riêng, không dừng tiến trình đang có. Bằng chứng browser nằm trong thư mục tạm do script in ra.

## Bàn giao

Chưa có so sánh latency production sau sửa, vì thay đổi chưa push/deploy. Sau review/CI và triển khai, đăng nhập lại một lần để kích hoạt cookie cho phiên đang dùng; thử F5, đăng xuất rồi F5, và đo lại request có đăng nhập. Không cam kết toàn bộ độ trễ sẽ biến mất chỉ từ việc giảm số truy vấn.
