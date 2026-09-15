# Product backlog hiện tại

Ưu tiên: `P0` bắt buộc cho luồng quản lý; `P1` cần cho bản cuối kỳ; `P2` chỉ làm khi P0/P1 ổn định. Trạng thái phản ánh repository tại ngày 16/09/2026.

## Sprint 0 — Baseline có thể merge

Mục tiêu: thống nhất cấu trúc làm việc và giữ một prototype có thể build/test làm mốc so sánh.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Trạng thái | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| GOV-01 | Chốt quy trình Scrum, phân công và DoD | P0 | TV1 | Cả nhóm | Review | Tài liệu trong `docs/project-management` được nhóm điền tên và đồng ý |
| GOV-02 | Thiết lập Issue/PR template và CI | P0 | TV1 | TV2 | Testing | PR tự động build và chạy 4 E2E test |
| PRO-01 | Baseline PWA responsive | P0 | TV3 | TV4 | Testing | Đăng nhập demo, màn hình desktop/mobile không vỡ luồng chính |
| PRO-02 | Baseline nhận → tồn → bán → hóa đơn → báo cáo | P0 | TV2, TV3 | TV1 | Testing | Dữ liệu demo nối xuyên suốt, chặn lô hết hạn và xuất FEFO |
| PRO-03 | Baseline kiểm kê offline | P1 | TV4 | TV2 | Testing | Reload giữ phiếu; đồng bộ phát hiện tồn thay đổi và yêu cầu kiểm lại |
| PRO-04 | Baseline đào tạo tách dữ liệu | P1 | TV4 | TV1 | Testing | Hoàn thành một ca; tồn/hóa đơn vận hành không bị thay đổi |

Điều kiện đóng Sprint 0 nằm tại [MERGE_01.md](MERGE_01.md).

## Sprint 1 — Backend và dữ liệu trung tâm

Mục tiêu: PWA đăng nhập và đọc danh mục từ API/CSDL thật.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Phụ thuộc | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| REQ-01 | Xác nhận phạm vi P1 và các mục `Cần khảo sát` | P0 | TV1 | Cả nhóm | GOV-01 | Có biên bản quyết định và cập nhật SRS |
| DB-01 | Chốt ERD MVP và data dictionary | P0 | TV2 | TV1, TV3 | REQ-01 | Khóa, quan hệ, trạng thái và ràng buộc được mô tả |
| BE-01 | Khởi tạo backend, migration và CSDL demo | P0 | TV2 | TV1 | DB-01 | Môi trường sạch chạy migration và health check |
| BE-02 | Đăng nhập và RBAC phía server | P0 | TV1 | TV2 | BE-01 | Ba vai trò chỉ gọi được API đúng quyền; mật khẩu được hash |
| BE-03 | API danh mục, sản phẩm và nhà cung cấp | P0 | TV2 | TV3 | BE-01 | CRUD cần thiết, tìm mã vạch và chống trùng |
| FE-01 | Tách adapter demo/API và kết nối đăng nhập | P0 | TV3 | TV1 | BE-02 | Có thể đổi adapter bằng cấu hình; lỗi mạng được hiển thị |
| QA-01 | Test API nền tảng và ma trận truy vết | P1 | TV4 | TV1 | BE-02, BE-03 | Test quyền và validation; requirement liên kết test |

## Sprint 2 — Luồng nghiệp vụ cốt lõi

Mục tiêu: hoàn thành luồng tạo sản phẩm → nhận lô → bán → báo cáo trên dữ liệu trung tâm.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Phụ thuộc | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| INV-01 | Transaction nhận hàng | P0 | TV2 | TV1 | BE-03 | Tạo phiếu/lô, tăng tồn và biến động nguyên tử |
| INV-02 | Tra cứu tồn, lô, hạn và biến động | P0 | TV2 | TV4 | INV-01 | Lọc còn hạn/cận hạn/hết hạn; truy được chứng từ nguồn |
| SAL-01 | Transaction bán hàng và hóa đơn | P0 | TV3 | TV2 | INV-02 | Server tính giá, chọn lô còn hạn, lưu hóa đơn và giảm tồn nguyên tử |
| PRO-01B | Khuyến mãi cơ bản | P1 | TV3 | TV1 | SAL-01 | Chỉ áp dụng đúng thời gian/phạm vi; hóa đơn giữ giá đã bán |
| REP-01 | Báo cáo doanh thu và tồn | P0 | TV3 | TV2 | SAL-01 | Báo cáo phản ánh đúng giao dịch vừa thực hiện |
| QA-02 | E2E luồng nhận–bán | P0 | TV4 | TV1 | REP-01 | Chạy tự động trên dữ liệu sạch và kiểm tra rollback lỗi |

## Sprint 3 — Hiện trường và kiểm kê

Mục tiêu: nhân viên kiểm kê trên điện thoại; quản lý duyệt trên máy tính.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Phụ thuộc | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| MOB-01 | Giao diện kiểm kê responsive | P1 | TV4 | TV3 | INV-02 | Không cuộn ngang ở viewport đã công bố; nhập theo lô |
| SYN-01 | Hàng đợi kiểm kê offline | P1 | TV4 | TV2 | MOB-01 | Giữ thao tác qua reload; có `clientOperationId` |
| SYN-02 | API đồng bộ và xử lý xung đột | P1 | TV2 | TV4 | SYN-01 | Retry không tạo bản ghi trùng; version lệch trả conflict |
| INV-03 | Duyệt điều chỉnh tồn | P1 | TV2 | TV1 | SYN-02 | Chỉ quản lý duyệt; tạo biến động; không duyệt hai lần |
| QA-03 | Test desktop/mobile/offline | P1 | TV4 | TV3 | INV-03 | Có kết quả test viewport, reload và reconnect |

## Sprint 4 — Đào tạo, ổn định và báo cáo

Mục tiêu: một kịch bản đào tạo hoàn chỉnh, bản release candidate và hồ sơ kiểm thử.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Phụ thuộc | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| TRN-01 | Khởi tạo Godot và scene cửa hàng | P1 | TV4 | TV1 | Sprint 0 | Chạy trên máy yếu nhất; có di chuyển, va chạm, tương tác |
| TRN-02 | Một kịch bản đào tạo | P1 | TV4, TV1 | TV3 | TRN-01 | Có mục tiêu, hướng dẫn, phản hồi sai và kết quả |
| TRN-03 | Lưu phiên/kết quả cách ly | P1 | TV1 | TV2 | BE-02, TRN-02 | Kết quả được lưu; không thay đổi dữ liệu vận hành |
| QA-04 | Regression, bảo mật quyền và backup/restore | P0 | TV4 | Cả nhóm | Các sprint trước | Test report đạt; khôi phục CSDL demo trên môi trường sạch |
| DOC-01 | Hoàn thiện báo cáo và traceability | P0 | TV1 điều phối | Cả nhóm | QA-04 | Yêu cầu–thiết kế–code–test có liên kết và minh chứng |
| REL-01 | Release candidate và kịch bản vấn đáp | P0 | TV1 | Cả nhóm | DOC-01 | Bản chạy sạch, dữ liệu demo, hướng dẫn và video dự phòng |

## Icebox — Không cam kết cuối kỳ

- Bán hàng offline hoàn chỉnh trên nhiều thiết bị.
- Native Android/iOS/Windows riêng và thiết bị POS chuyên dụng.
- SaaS đa doanh nghiệp, subscription và billing.
- Nhiều cửa hàng/kho trung tâm vận hành đồng thời.
- Khách hàng thành viên và đổi điểm nếu khảo sát không chứng minh cần cho MVP.
- Chapter kinh dị, AI khách hàng, combat và toàn bộ cốt truyện.

