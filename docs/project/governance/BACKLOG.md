# Product backlog hiện tại

Tài liệu này chốt phạm vi, ưu tiên và phụ thuộc ở mức sprint. Sau khi GitHub Project được tạo, trạng thái thực thi từng ngày chỉ cập nhật trên Issue/Project; không sửa đồng thời một cột trạng thái trùng lặp tại đây.

Ưu tiên: `P0` bắt buộc cho luồng quản lý; `P1` cần cho bản cuối kỳ; `P2` chỉ làm khi P0/P1 ổn định. Sprint 0 là baseline ngày 21/09/2026; Sprint 1 đã đóng 7/7 Issue ngày 30/09/2026; Sprint 2 là kế hoạch đang triển khai. [Điểm vào tài liệu và mốc tiến độ](../README.md) giúp phân biệt phần đã tích hợp với PR đang review. TV1–TV4 được ánh xạ trong [TEAM.md](TEAM.md).

## Sprint 0 — Baseline có thể merge

Mục tiêu: thống nhất cấu trúc làm việc và giữ một prototype có thể build/test làm mốc so sánh.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Trạng thái | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| GOV-01 | Chốt quy trình Scrum, phân công và DoD | P0 | TV1 | Cả nhóm | Done | Tài liệu trong `docs/project/governance` có owner, reviewer và quy tắc thống nhất |
| GOV-02 | Thiết lập Issue/PR template và CI | P0 | TV1 | TV2 | Done | PR tự động chạy repository policy và web baseline gate |
| PRO-01 | Baseline PWA responsive | P0 | TV3 | TV4 | Done | Đăng nhập demo, màn hình desktop/mobile không vỡ luồng chính |
| PRO-02 | Baseline nhận → tồn → bán → hóa đơn → báo cáo | P0 | TV2, TV3 | TV1 | Done | Dữ liệu demo nối xuyên suốt, chặn lô hết hạn và xuất FEFO |
| PRO-03 | Baseline kiểm kê offline | P1 | TV4 | TV2 | Done | Reload giữ phiếu; đồng bộ phát hiện tồn thay đổi và yêu cầu kiểm lại |
| PRO-04 | Baseline đào tạo tách dữ liệu | P1 | TV4 | TV1 | Done | Hoàn thành một ca; tồn/hóa đơn vận hành không bị thay đổi |

Điều kiện đóng Sprint 0 nằm tại [MERGE_01.md](MERGE_01.md).

## Sprint 1 — Backend và dữ liệu trung tâm

Mục tiêu: PWA đăng nhập và đọc danh mục từ API/CSDL thật.

Sprint 1 đã đóng 7/7 Issue; [kickoff Sprint 1](../SPRINT_1_KICKOFF.md) lưu thứ tự tích hợp đã thực hiện. Code thử nghiệm ngoài `main` không làm thay đổi trạng thái backlog. [Bản đối chiếu REQ-01 ngày 29/09/2026](../REQ-01_SCOPE_RECORD_2026-09-29.md) đã tích hợp qua [PR #17](https://github.com/zomboXx/bach-hoa-sim-tim/pull/17), [Issue #2](https://github.com/zomboXx/bach-hoa-sim-tim/issues/2) đã đóng; phạm vi theo backlog hiện hành, phần mở rộng trong Word để sau.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Phụ thuộc | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| REQ-01 | Xác nhận phạm vi P1 và các mục `Cần khảo sát` | P0 | TV1 | Cả nhóm | GOV-01 | Đối chiếu pain point, Use Case và từng yêu cầu với phạm vi MVP; bản ghi quyết định có ngày trên Issue/PR ghi mục chấp nhận/hoãn/loại, nguồn nghiên cứu/khảo sát và người xác nhận; cập nhật SRS theo quyết định |
| DB-01 | Chốt ERD MVP và data dictionary | P0 | TV2 | TV1, TV3 | REQ-01 | Mô tả khóa, quan hệ, trạng thái, phạm vi tổ chức/cửa hàng và ràng buộc cho tài khoản/quyền, danh mục/giá, nhà cung cấp, nhận hàng, tồn, bán và kiểm kê; tách bảng MVP khỏi thiết kế tương lai; review kiểu tiền, số lượng và quy tắc làm tròn trước migration nghiệp vụ |
| BE-01 | Khởi tạo backend, migration và CSDL demo | P0 | TV2 | TV1 | DB-01 | Từ môi trường sạch chạy được Java/Spring Boot, PostgreSQL và Flyway; migration của schema đã chốt tạo dữ liệu demo tối thiểu; health check và integration test qua; Maven Wrapper, Compose và gate CI được commit cùng module |
| BE-02 | Đăng nhập và RBAC phía server | P0 | TV1 | TV2 | BE-01 | Mật khẩu được hash; đăng nhập sai và truy cập thiếu quyền bị từ chối; bốn vai trò bán hàng/hàng hóa/quản lý/quản trị viên chỉ dùng API được cấp; người học là trạng thái nhân viên, không phải vai trò server thứ năm |
| BE-03 | API danh mục, sản phẩm và nhà cung cấp | P0 | TV2 | TV3 | BE-01 | CRUD theo quyền và trạng thái; tìm sản phẩm theo mã, mã vạch hoặc tên; từ chối mã trùng và dữ liệu không hợp lệ bằng lỗi có cấu trúc; contract giá được review trước API thay đổi giá |
| FE-01 | Tách adapter demo/API và kết nối đăng nhập | P0 | TV3 | TV1 | BE-02 | Đổi demo/API bằng cấu hình; PWA đăng nhập qua API, hiển thị trạng thái chờ/lỗi mạng và điều hướng theo quyền server; demo adapter vẫn qua regression |
| QA-01 | Test API nền tảng và ma trận truy vết | P1 | TV4 | TV1 | BE-02, BE-03 | Test đăng nhập sai, truy cập sai quyền, mã trùng và validation; ma trận nối requirement → contract → test; chỉ ghi kết quả đạt khi API thật được chạy kiểm thử |

Quyết định của Project Owner Nguyễn Đức Phát ngày 26/09/2026: bốn vai trò server là bán hàng, hàng hóa, quản lý và quản trị viên; “người học” là trạng thái của nhân viên. Dòng BE-02 trước đây ghi năm vai trò; [phương án cũ](../REQ-01_DECISION_DRAFT.md) và [bản ghi phạm vi đã tích hợp](../REQ-01_SCOPE_RECORD_2026-09-29.md) lưu thay đổi.

[Schema vật lý DB-01](../../architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md) đã được Project Owner duyệt ngày 27/09/2026: 38 bảng của Word và một bảng phiên đăng nhập, migration theo sprint. Cùng ngày, Project Owner thông báo nhóm đã review và hoàn toàn đồng ý với phương án; chưa có liên kết biên bản review thiết kế để đối chiếu độc lập. Ba ERD logic đã được đồng bộ với các thay đổi thuộc MVP. [PR #11](https://github.com/zomboXx/bach-hoa-sim-tim/pull/11) chứa BE-01 và V2/V3 cho 14 bảng Sprint 1, CI PostgreSQL 17 cùng hai job khác đã đạt; PR đã merge vào `main` với commit `cced1a1`, được kiểm tra ngày 28/09/2026. Các bảng sau MVP trong schema đích chưa trở thành cam kết sprint.

BE-02 đã tích hợp qua [PR #14](https://github.com/zomboXx/bach-hoa-sim-tim/pull/14); [contract session/RBAC Accepted](../../../contracts/AUTH_SESSION_REVIEW.md) là nguồn giao tiếp hiện hành. BE-03 và FE-01 đã tích hợp; [PR #12](https://github.com/zomboXx/bach-hoa-sim-tim/pull/12) đưa web adapter demo/API vào `main`. [PR #15](https://github.com/zomboXx/bach-hoa-sim-tim/pull/15) đã tích hợp QA-01. Các kết quả này là nền Sprint 1, không phải bằng chứng các nghiệp vụ Sprint 2 đã xong.

### Chi tiết từ Word cần refinement, chưa cam kết vào Sprint 1

| Ứng viên | Nội dung cần ghi nhận | Phụ thuộc và quyết định còn thiếu |
|---|---|---|
| ACC-01 | Quản trị viên tạo, khóa/mở khóa tài khoản và gán vai trò; có đường cấp và thu hồi tài khoản cá nhân. | Sau BE-02; cần nhóm chốt sprint, owner/reviewer và quyền quản trị cụ thể. |
| PRC-01 | Giá bán có thời điểm hiệu lực và lịch sử; hóa đơn giữ giá tại lúc bán. | DB-01 và BE-03 phải chốt một giá hiện hành hay nhiều phiên bản giá trong MVP trước khi lên lịch API. |

Hai mã trên là nhãn theo dõi đề xuất, chưa là Issue đã nhận làm hoặc tiêu chí đóng Sprint 1. Các mục Word thuộc Sprint 2 trở đi được đối chiếu theo từng sprint trong [bản nháp phạm vi Word](../SPRINT_1_WORD_BACKLOG_DRAFT.md); chưa mở rộng cam kết của sprint này.

## Sprint 2 — Luồng nghiệp vụ cốt lõi

Mục tiêu: hoàn thành luồng tạo sản phẩm → nhận lô → bán → báo cáo trên dữ liệu trung tâm.

| ID | Backlog item | Ưu tiên | Owner | Reviewer | Phụ thuộc | Tiêu chí chấp nhận |
|---|---|---:|---|---|---|---|
| [INV-01](https://github.com/zomboXx/bach-hoa-sim-tim/issues/18) | Transaction nhận hàng | P0 | TV2 | TV1 | BE-03 | Ghi số giao/nhận/từ chối và lý do trên phiếu; tạo lô, tăng tồn và biến động nguyên tử |
| [INV-02](https://github.com/zomboXx/bach-hoa-sim-tim/issues/19) | Tra cứu tồn, lô, hạn và biến động | P0 | TV2 | TV4 | INV-01 | Lọc còn hạn/cận hạn/hết hạn; truy được chứng từ nguồn |
| [FE-02](https://github.com/zomboXx/bach-hoa-sim-tim/issues/20) | Giao diện nhận hàng và tồn qua API | P0 | TV4 | TV3 | INV-01, INV-02 | PWA API mode xác nhận phiếu, tra tồn/lô/biến động theo quyền; demo mode giữ regression |
| [SAL-01](https://github.com/zomboXx/bach-hoa-sim-tim/issues/21) | Transaction bán hàng và hóa đơn | P0 | TV3 | TV2 | INV-02 | Server tính giá, chọn lô còn hạn, lưu hóa đơn và giảm tồn nguyên tử |
| [PRO-01B](https://github.com/zomboXx/bach-hoa-sim-tim/issues/23) | Khuyến mãi cơ bản | P1 | TV3 | TV1 | SAL-01 | Chỉ áp dụng đúng thời gian/phạm vi; hóa đơn giữ giá đã bán |
| [REP-01](https://github.com/zomboXx/bach-hoa-sim-tim/issues/22) | Báo cáo doanh thu và tồn | P0 | TV2 | TV3 | SAL-01 | Báo cáo phản ánh đúng giao dịch vừa thực hiện |
| [QA-02](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24) | E2E luồng nhận–bán | P0 | TV4 | TV1 | REP-01 | Chạy tự động trên dữ liệu sạch và kiểm tra rollback lỗi |

[Milestone Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2), [kickoff](../SPRINT_2_KICKOFF.md) và [contract ranh giới](../../../contracts/SPRINT_2_BOUNDARY_DRAFT.md) quy định thứ tự triển khai và điểm giao. Theo điều chỉnh phân công ngày 30/09/2026, TV4 nhận FE-02 để có phần code PWA nhận/tồn cùng QA-02; TV2 nhận REP-01 sau INV-01/02, TV3 tập trung SAL-01 và review giao diện; TV1 điều phối và review, không nhận thêm feature. FE-02 vẫn giữ nguyên tiêu chí nghiệp vụ INV-01/02. PRO-01B là P1 sau các P0; cần kiểm lại năng lực TV2 vì có ba Issue backend. Trạng thái thực thi lấy từ GitHub Issues; Project board cần quyền truy cập riêng nếu nhóm dùng.

Quyết định của Project Owner Nguyễn Đức Phát ngày 26/09/2026: với hàng giao thiếu, thừa hoặc hư hỏng, MVP ghi số lượng và lý do trên phiếu nhận; chưa có quy trình điều chỉnh riêng. Tiêu chí INV-01 trước đây chỉ ghi tạo phiếu/lô, tăng tồn và biến động nguyên tử; xem [bản đối chiếu REQ-01](../REQ-01_DECISION_DRAFT.md).

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
