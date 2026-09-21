# Kết quả kiểm thử cục bộ — 16/09/2026

## Bổ sung POS — 18/09/2026

Vue TypeScript/Vite build: PASS. Playwright toàn suite: **12/12 PASS** (27.9 giây), bao gồm 2 test mới và 10 kiểm tra hồi quy. Không thay đổi API/schema trong lần cập nhật POS này.

Màn chính Sales đổi thành hóa đơn trống; tra tên/barcode để thêm, chỉnh số lượng và xóa dòng. Hai test mới (desktop và 390×844) kiểm tra: hóa đơn trống; thêm đúng 1; quét lại gộp dòng/tăng 1; nút ±; chặn chữ/âm/thập phân/exponent/paste sai/số quá lớn; chặn vượt tồn; ô trống tạm khôi phục khi blur; cập nhật thành tiền; nhập 0/giảm về 0/xóa nút; camera chỉ thông báo. Ảnh `invoice-empty-{desktop,mobile}.png` và `invoice-filled-{desktop,mobile}.png` trong thư mục test-results (ignored). Các kết quả 16/09 phía dưới giữ để đối chiếu lịch sử.

Nhánh: `codex/sprint-1-central-api-chapter-0`. Đây là bằng chứng kỹ thuật cục bộ, chưa phải biên bản nghiệm thu hoặc kết quả GitHub Actions.

| Hạng mục | Kết quả |
|---|---|
| Maven verify, JDK 25 (target Java 21) | PASS, đóng gói Spring Boot JAR |
| Spring Boot + PostgreSQL 17.11 + Flyway V1/V2/V3 | PASS; nâng cấp DB hiện có lên V3 và validate lại |
| JUnit/MockMvc | **12/12 PASS** trên DB riêng `simtim_test` |
| Vue TypeScript + Vite production build | PASS |
| Playwright trên bản production preview, Chrome | **10/10 PASS**, 23.8 giây ở lần chạy cuối |
| Godot 4.6.3 import + Web single-thread export | PASS |
| Godot physics tour + compact quiz | PASS: nhân vật qua collision tới 6 điểm, thử đáp án sai, 3 đáp án đúng mới mở completion; 3 câu và phản hồi sai không tràn khung 640×560 |
| API runtime health | UP, PostgreSQL; localhost:8080 |
| Git diff whitespace check | PASS |

## Các kiểm tra API

1. Health hoạt động; người chưa đăng nhập không đọc state.
2. BCrypt và login đúng/sai; cookie session đọc được `/me`, không lộ password.
3. CSRF và kiểm tra phân quyền server.
4. Chống trùng barcode, tìm barcode, validation tên danh mục.
5. Giá/khuyến mãi server, lô còn hạn FEFO, giữ nguyên lô hết hạn, retry idempotent không trừ tồn hai lần.
6. Thiếu tiền không thay đổi tồn.
7. Không nhận số lượng phân số; không complete training khi thiếu điểm khám phá.
8. Kiểm kê version cũ trả CONFLICT, không ghi đè tồn.
9. Chapter chưa mở trả đúng thông báo yêu cầu.
10. Phiên đào tạo chỉ chủ sở hữu truy cập; đủ 6 event vẫn chưa complete nếu chưa trả lời orientation đúng; tồn/hóa đơn không thay đổi.
11. Ma trận quyền ghi của 5 tài khoản trên hóa đơn, nhận hàng, kiểm kê, danh mục và duyệt.
12. Read model và endpoint đọc không lộ không gian nghiệp vụ không được phép (Kế toán/Kho/Sales).

## Các kiểm tra trình duyệt

1. Sales login → barcode + Enter vào giỏ → thanh toán ghi nhận → hóa đơn → reload còn hóa đơn; không có nút nhận hàng.
2. Màn 7 chương, 6 popup đúng nội dung, launch canvas Godot thật, E trò chuyện với mentor và ghi điểm khám phá.
3. Viewport 390×844: POS/lobby không tràn ngang; game và hội thoại đã kiểm tra ảnh. Desktop kiểm tra ở 1440×1050. Đã sửa min-width của grid POS khi có nhiều danh mục.
4. Manager tạo danh mục/sản phẩm/nhà cung cấp, sửa giá → stock nhận hàng → xem lô/tồn từ API.
5. Manager ngắt mạng thật trong browser context, ghi kiểm kê → reload → nối mạng → phiếu lên server một lần → duyệt riêng. Warehouse hiện không được lập kiểm kê theo giới hạn role mới.
6–10. Lần lượt đăng nhập Sales, Warehouse, Kế toán, Manager và Admin: menu theo quyền, vào mentor/quay lại, reload không trắng màn hình; kiểm tra không tràn ngang trên mobile.

Ảnh tự động: `apps/web/test-results/academy-desktop.png`, `academy-mobile.png`, `godot-mentor.png`, `godot-mobile.png`. Đây là output tạm bị ignore, sẽ được CI upload làm artifact. Sprite alpha thật đã kiểm tra trong game Web. Godot tour chạy riêng ở `apps/training-godot/tests/tour_test.gd`; completion/orientation API kiểm thử độc lập bằng MockMvc, chưa có test browser tự chơi toàn bộ 6 điểm và 3 câu từ đầu đến cuối.

## Chưa xác nhận

- Chưa chạy CI từ GitHub, chưa push/merge hoặc review bởi thành viên khác.
- Chưa nghiệm thu trên điện thoại thật/Safari/máy yếu nhất của nhóm.
- Chưa kiểm thử tải đồng thời, backup/restore, mất kết nối DB giữa transaction và môi trường internet production.
- Không tuyên bố full offline, payment gateway, SaaS đa tenant hay chapter nghiệp vụ sau Chapter 0 đã hoàn thành.

Môi trường dữ liệu mẫu có một số chứng từ, phiếu và danh mục được tạo khi chạy E2E để minh chứng lưu dữ liệu thật. Không xóa hoặc reset dữ liệu này tự động sau kiểm thử.
