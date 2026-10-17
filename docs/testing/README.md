# Hồ sơ kiểm thử

Thư mục này lưu test plan, ma trận truy vết, ma trận quyền và test report đã được review. Không commit token, secret, database dump, Playwright report, ảnh test hàng loạt hoặc output build.

## QA-01 — API nền tảng

Đọc theo thứ tự:

1. [Test plan](QA-01-test-plan.md): mục tiêu, phạm vi, phương pháp và điều kiện hoàn thành.
2. [Ma trận quyền](QA-01-authorize.md): actor/role nào được phép thực hiện operation nào.
3. [Ma trận truy vết](QA-01-traceability.md): requirement → contract → test → result → evidence.
4. [Test report](QA-01-test-report.md): trạng thái thực thi và kết luận tại một commit cụ thể.

Một test chỉ được ghi `Pass` khi người lập báo cáo trực tiếp chạy trên implementation xác định và có bằng chứng. Test được owner hoặc CI của module báo đạt nhưng chưa được QA đối chiếu ghi `Reported`; dependency chưa sẵn sàng ghi `Blocked`; assertion khác expected ghi `Fail`; lỗi khởi tạo/deserialization ghi `Error` và nêu rõ test có đi tới assertion hay không.

Checkpoint hiện tại ngày 29/09/2026: TV4 đã chạy source `5f0b14c` có cả BE-02/BE-03 trên PostgreSQL 17.11 disposable; root gate, 31/31 backend tests và 29/29 traceability cases đạt. Hồ sơ đang chờ reviewer/CI của head mới trước khi đóng Issue #8.

## QA-02 — E2E nhận–bán–báo cáo

Đọc theo thứ tự:

1. [Test plan](QA-02-test-plan.md): fixture sạch, oracle DB và exit criteria.
2. [Ma trận quyền](QA-02-authorize.md): bốn role, store scope và invoice privacy.
3. [Ma trận truy vết](QA-02-traceability.md): backlog → contract → automated test → evidence → Pass/Fail/Blocked.
4. [Test report](QA-02-test-report.md): lệnh, SHA, runtime, kết quả và khoảng trống chưa được phép ghi Pass.
5. [Checkpoint sáu GAP ngày 11/10](QA-02-gap-checkpoint-2026-10-11.md): quyết định phạm vi Issue #34, test tái hiện expected-red, oracle và điều kiện chạy lại.
6. [Bằng chứng GAP-06 ngày 13/10](QA-02-gap06-evidence-2026-10-13.md): live nhận–bán–báo cáo, DB oracle trước/sau, root gate và điều kiện cập nhật #34 sau merge.
7. [Bằng chứng sửa review sales ngày 17/10](QA-02-sales-retry-evidence-2026-10-17.md): quote hiển thị trước checkout, retry cùng idempotency key và oracle invoice/payment/SALE movement độc lập.

Checkpoint ngày 07/10/2026: suite QA xuyên module đạt 5/5 trên PostgreSQL 17.11 disposable và đã bắt lỗi query tồn REP-01 mà mock test bỏ sót. Bản sửa `840a82a` buộc dùng Testcontainers và tăng oracle revenue thành hóa đơn hai dòng; PR #33 đã merge vào `main`. Toàn QA-02 vẫn chưa có bằng chứng nghiệm thu P0 đầy đủ: ma trận còn 3 Fail và 3 Blocked về checkout price/replay, unit precision, invoice privacy và live PWA sales E2E.

## DOC-01 — Truy vết bản cuối kỳ

[Ma trận truy vết bản làm việc](DOC-01-traceability-working.md) nối yêu cầu vận hành và đào tạo với thiết kế, code, test và phần bằng chứng còn thiếu tại `main` ngày 14/10/2026. Đây là đầu vào cho DOC-01 và QA-04, chưa phải kết quả Pass hay báo cáo cuối.

## QA-03 — Desktop/mobile/offline kiểm kê

Đọc theo thứ tự:

1. [Test plan](QA-03-test-plan.md): môi trường, viewport, database sạch và exit criteria.
2. [Ma trận quyền](QA-03-authorize.md): quyền đọc, gửi và duyệt theo role/store.
3. [Ma trận truy vết](QA-03-traceability.md): backlog → contract → test → evidence.
4. [Test report](QA-03-test-report.md): SHA, lệnh chạy thật, ảnh viewport và ngoại lệ còn chờ review.

QA-03 không cập nhật kết luận QA-02 hoặc sáu GAP tại Issue #34.
