# Hồ sơ kiểm thử

- [QR chuyển khoản xác nhận thủ công](MANUAL_TRANSFER_SETUP.md): cấu hình tài khoản riêng, thử hai thiết bị và lệnh PostgreSQL/API/web disposable. Không chuyển tiền trong bài kiểm thử tự động.

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
8. [Bằng chứng retry HTTP 502 ngày 18/10](QA-02-sales-502-evidence-2026-10-18.md): giữ operation khi kết quả checkout chưa chắc chắn, browser/live replay cùng key và DB oracle không ghi trùng.
9. [Kết quả chạy lại trên mã tích hợp ngày 20/10](QA-02-closeout-2026-10-20.md): đối chiếu từng GAP, live browser và oracle PostgreSQL; kết quả hiện hành 23/23 Pass.

Checkpoint ngày 07/10/2026 (lịch sử): suite QA xuyên module đạt 5/5 trên PostgreSQL 17.11 disposable và đã bắt lỗi query tồn REP-01 mà mock test bỏ sót. Bản sửa `840a82a` buộc dùng Testcontainers và tăng oracle revenue thành hóa đơn hai dòng; PR #33 đã merge vào `main`. Sáu ngoại lệ khi đó gồm 3 Fail và 3 Blocked; kết quả sau khi sửa nằm ở bản chạy lại ngày 20/10 phía trên.

## QA-04 — Regression, bảo mật quyền và backup/restore

Đọc theo thứ tự:

1. [Test plan](QA-04-test-plan.md): mục tiêu, phạm vi kiểm tra các sprint trước, ma trận quyền và kiểm chứng sao lưu/khôi phục trên môi trường sạch.
2. [Ma trận quyền](QA-04-authorize.md): 4 vai trò SALES, STOCK, MANAGER, ADMIN, nguyên tắc deny-by-default, che giấu giá vốn và cô lập phạm vi cửa hàng.
3. [Ma trận truy vết](QA-04-traceability.md): requirement/NFR → contract → automated test → oracle/actual → Pass/Fail/Blocked; ảnh chụp 15/10 có 22 Pass, 3 Fail, 3 Blocked trong 28 dòng, sáu ngoại lệ được cập nhật trong bản QA-02 ngày 20/10.
4. [Test report](QA-04-test-report.md): lệnh, runtime, 9/9 tests Pass của `Qa04RegressionSecurityTest` trên mã tích hợp, kết quả backup/restore sạch và đối soát trung thực ngoại lệ #34.

Checkpoint ngày 15/10/2026: TV2 đã chạy suite QA-04 và đối soát CSDL độc lập trên PostgreSQL 17 Testcontainers; khôi phục CSDL demo sạch thành công; đối soát đầy đủ 6 GAP của Issue #34.

Lượt bổ sung 19/10/2026 ở đầu [test report](QA-04-test-report.md) ghi Compose ba dịch vụ, ca đào tạo server, đối soát vận hành và backup/restore bằng script trên project riêng. PR #70 đã tích hợp, root gate và CI trên `main` đạt; chưa có bằng chứng TV4 đã review lượt tích hợp. Sáu ngoại lệ #34 được chạy lại riêng ngày 20/10.

## DOC-01 — Truy vết bản cuối kỳ

[Ma trận truy vết bản làm việc](DOC-01-traceability-working.md) nối yêu cầu vận hành và đào tạo với thiết kế, code, test và phần bằng chứng còn thiếu tại `main` ngày 14/10/2026. Đây là đầu vào cho DOC-01 và QA-04, chưa phải kết quả Pass hay báo cáo cuối.

## TRN-02 — Kịch bản đào tạo

[Kịch bản kiểm thử thủ công TRN-02](TRN-02-manual-test-script.md) hướng dẫn TV4 kiểm luồng đúng, sai, đảo thứ tự, bỏ dở, chơi lại và cách ly hóa đơn/tồn; đồng thời ghi checklist để TV1 review luật chấm và TV2 review contract. Bảng kết quả khởi tạo là `Not run`/`Blocked`, không phải bằng chứng nghiệm thu.

[Test report TRN-02](TRN-02-test-report.md) ghi kết quả local ngày 17/10/2026: M01–M07 đạt trên kết quả merge evidence `e855e74` với implementation `c56437c`, demo `25/25` và API consumer `39/39`; M08 còn `Blocked` chờ TRN-03. Đây chưa phải bằng chứng Done vì CI và review TV1/TV2 còn chờ.

## QA-03 — Desktop/mobile/offline kiểm kê

Đọc theo thứ tự:

1. [Test plan](QA-03-test-plan.md): môi trường, viewport, database sạch và exit criteria.
2. [Ma trận quyền](QA-03-authorize.md): quyền đọc, gửi và duyệt theo role/store.
3. [Ma trận truy vết](QA-03-traceability.md): backlog → contract → test → evidence.
4. [Test report](QA-03-test-report.md): SHA, lệnh chạy thật, ảnh viewport và ngoại lệ còn chờ review.

QA-03 không cập nhật kết luận QA-02 hoặc sáu GAP tại Issue #34.
