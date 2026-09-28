# Hồ sơ kiểm thử

Thư mục này lưu test plan, ma trận truy vết, ma trận quyền và test report đã được review. Không commit token, secret, database dump, Playwright report, ảnh test hàng loạt hoặc output build.

## QA-01 — API nền tảng

Đọc theo thứ tự:

1. [Test plan](QA-01-test-plan.md): mục tiêu, phạm vi, phương pháp và điều kiện hoàn thành.
2. [Ma trận quyền](QA-01-authorize.md): actor/role nào được phép thực hiện operation nào.
3. [Ma trận truy vết](QA-01-traceability.md): requirement → contract → test → result → evidence.
4. [Test report](QA-01-test-report.md): trạng thái thực thi và kết luận tại một commit cụ thể.

Một test chỉ được ghi `Pass` khi người lập báo cáo trực tiếp chạy trên implementation xác định và có bằng chứng. Test được owner hoặc CI của module báo đạt nhưng chưa được QA đối chiếu ghi `Reported`; dependency chưa sẵn sàng ghi `Blocked`.
