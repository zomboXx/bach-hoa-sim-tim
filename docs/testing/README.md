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

Checkpoint ngày 07/10/2026: suite QA xuyên module đạt 5/5 trên PostgreSQL 17.11 disposable và đã bắt lỗi query tồn REP-01 mà mock test bỏ sót. TV1 đã review độc lập head `d18b2b3`; bản sửa inline `840a82a` buộc dùng Testcontainers và tăng oracle revenue thành hóa đơn hai dòng, đang chờ CI/reviewer xác nhận lại. Toàn QA-02 chưa đạt nghiệm thu vì còn các khoảng trống checkout price/replay, unit precision, invoice privacy và live PWA sales E2E.
