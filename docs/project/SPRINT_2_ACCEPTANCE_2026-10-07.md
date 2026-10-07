# Sprint 2 — Nghiệm thu có ngoại lệ ngày 07/10/2026

**Quyết định của Project Owner — TV1 Nguyễn Đức Phát, 07/10/2026:** nghiệm thu kết quả Sprint 2 **có ngoại lệ** và tiếp tục theo dõi các lỗi còn lại. Quyết định này chấp nhận mốc bàn giao hiện tại để chuyển sang bước chuẩn bị tiếp theo; không chuyển các ca QA chưa đạt thành Pass và không khẳng định toàn bộ P0 đã hoàn tất.

## Bằng chứng và phạm vi nhận

- [Milestone Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2) có 7/7 Issue đóng tại thời điểm kiểm tra; các PR triển khai và QA đã merge vào `main` tới `cc65009`. Milestone vẫn ở trạng thái mở.
- [QA-02 test report](../testing/QA-02-test-report.md) ghi suite xuyên module 5/5 Pass, root gate web và backend đạt trên PostgreSQL Testcontainers. [Ma trận QA-02](../testing/QA-02-traceability.md) còn **3 Fail, 3 Blocked**.
- Phần đã tích hợp: API nhận hàng, tồn/lô/biến động, checkout tiền mặt, khuyến mãi và báo cáo; PWA API mode có nhận/tồn, khuyến mãi và báo cáo. [Demo Compose](../../infra/README.md) giúp kiểm tra trên máy, nhưng không thay thế bằng chứng QA còn thiếu.

## Ngoại lệ phải theo dõi

| Mã theo dõi | Kết quả QA | Phần cần bổ sung | Điều kiện đóng |
| --- | --- | --- | --- |
| S2-GAP-01 | Fail | Ràng buộc quantity theo đơn vị EA (nguyên) và KG (tối đa 3 số thập phân) ở biên ghi nghiệp vụ. | Provider test từ chối EA lẻ, chấp nhận KG hợp lệ; không ghi dữ liệu khi sai. |
| S2-GAP-02 | Fail | Quote/checkout có cơ chế `expectedTotal` và trả `PRICE_CHANGED` khi giá đổi. | Integration test chứng minh checkout không ghi hóa đơn/tồn nếu tổng giá thay đổi. |
| S2-GAP-03 | Fail | SALES không xem hóa đơn do nhân viên khác tạo trong cùng cửa hàng. | Test cùng store, khác seller trả từ chối ở list/detail theo contract quyền đã chốt. |
| S2-GAP-04 | Blocked | Checkout có khóa idempotency và cách đối soát khi mất phản hồi. | Replay cùng key không tạo hóa đơn/trừ tồn lần hai; payload khác bị từ chối. |
| S2-GAP-05 | Blocked | PWA API mode nối bán hàng, checkout và hóa đơn thật với backend. | Consumer và live E2E chứng minh quote, checkout tiền mặt, hóa đơn, tồn và lỗi API trên cùng luồng. |
| S2-GAP-06 | Blocked | Chạy live web E2E nhận–bán–báo cáo trên API/PostgreSQL thật. | Test tự động trên database sạch, bằng chứng PR/CI và báo cáo QA cập nhật. |

Sáu mã trên được theo dõi trong [Issue #34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34), **chưa được xếp Sprint 3**. Khi lập Sprint 3, nhóm cần tách Issue/owner/reviewer theo phụ thuộc: xử lý contract checkout và quyền trước khi chốt live E2E, rồi liên kết Issue con vào bảng này. Mỗi dòng chỉ được đóng khi test tương ứng đạt trên code đã tích hợp và QA cập nhật ma trận; lịch sử QA-02 ngày 07/10 giữ nguyên để truy vết.
