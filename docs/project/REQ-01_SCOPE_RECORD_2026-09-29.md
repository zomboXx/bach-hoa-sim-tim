# REQ-01 — Đối chiếu phạm vi ngày 29/09/2026

- Trạng thái: **Project Owner xác nhận phạm vi theo backlog ngày 29/09/2026 — chờ review của nhóm và tích hợp**.
- Issue: [REQ-01 #2](https://github.com/zomboXx/bach-hoa-sim-tim/issues/2).
- Người cung cấp thông tin: Nguyễn Đức Phát, TV1, Project Owner, `@zomboXx`.
- Căn cứ phạm vi: [backlog hiện hành](governance/BACKLOG.md); bản ghi này không bổ sung cam kết sprint.
- Thay thế phương án: [phương án ngày 26/09/2026](REQ-01_DECISION_DRAFT.md). Giữ bản cũ để truy vết.

## Nguồn và cách ghi nhận quyết định

Ngày 29/09/2026, Project Owner cho biết nhóm không có biên bản cho một cuộc họp chốt riêng: các thành viên đã trao đổi, tìm hiểu qua nhiều ngày rồi khởi động dự án. Nhóm có báo cáo hằng tuần cho giảng viên, gồm phân tích hiện trạng, xác định và mô hình hóa yêu cầu, giải pháp kỹ thuật, thiết kế giao diện, cài đặt, kiểm thử và phân công.

Đã đối chiếu bản nguồn `Nhom04_BaoCaoHoanChinh.docx`, phần **BÁO CÁO TUẦN 4**, sửa lần cuối 26/09/2026. SHA-256: `D650D50CE38AC76DE851728765CBFF695813237A143C14EA2F0808EA998BA6AA`. Bản Word đang soạn được giữ ngoài Git theo repository policy; tên, hash và các mục dưới đây nhận diện phiên bản đã đọc.

| Vị trí trong báo cáo | Nội dung dùng đối chiếu |
|---|---|
| Chương 1, mục 1.1 | Mô hình cửa hàng bán lẻ cho đồ án, dữ liệu bán hàng/tồn tập trung và môi trường đào tạo 2D cách ly. |
| Chương 1, mục 1.2 | Nghiên cứu tham khảo Sapo POS và KiotViet. |
| Chương 1, mục 1.3 | Quy trình bán/nhận hàng, sai lệch nhận hàng, hạn sử dụng/kiểm kê, báo cáo, tài khoản và đào tạo. |
| Chương 2 | Danh sách yêu cầu chức năng và phi chức năng; có cả chức năng rộng hơn backlog đã cam kết. |
| Chương 3, mục 3.2 và 3.4 | Tác nhân, chức năng và đặc tả Use Case. |

Đây là bằng chứng nghiên cứu tham khảo và thảo luận của nhóm theo lời Project Owner, không phải bằng chứng phỏng vấn một cửa hàng hay xác nhận của giảng viên. Không tạo biên bản có ngày lùi hoặc gán phê duyệt riêng cho TV2/TV3/TV4 khi chưa có bản ghi đó. Ghi quyết định hiện tại trên Issue/PR là đủ để truy vết; không yêu cầu nhóm tổ chức lại cuộc họp đã trao đổi trước đây.

## Ma trận phạm vi được Project Owner xác nhận

Các trạng thái trong [SRS 0.2](../product/analysis/02-danh-sach-yeu-cau.md) phản ánh bản đối chiếu dưới đây. Ngày 29/09/2026, Project Owner chọn: “Theo backlog hiện hành; phần mở rộng trong Word để sau”. Ma trận ghi quyết định phạm vi này; không ghi từng thành viên đã review riêng bản cập nhật. `Chấp nhận` là thuộc phạm vi hiện hành, không phải đã triển khai hoặc kiểm thử đạt; `Hoãn` là chưa cam kết; `Loại` là ngoài bản nộp hiện tại. Giữ ưu tiên SRS để thấy rõ các mục P1 chưa được lên lịch.

| Yêu cầu | Trạng thái và giới hạn | Use Case liên quan | Backlog / pain point |
|---|---|---|---|
| FR-AUTH-01..03 | Chấp nhận đăng nhập, đăng xuất, bốn vai trò server | UC-AUTH-01..02 | BE-02, FE-01 / PP-06 |
| FR-AUTH-04..05 | Hoãn CRUD tài khoản và đổi mật khẩu; ACC-01 chưa lên lịch | UC-ADM-01, UC-AUTH-03 | Ứng viên ACC-01 / PP-06 |
| FR-CAT-01..05, FR-REC-01 | Chấp nhận danh mục, tìm kiếm, chống trùng, nhà cung cấp; API giá cần review contract trước triển khai | UC-CAT-01..02, UC-SUP-01 | BE-03 / PP-02, PP-07 |
| FR-REC-02..05 | Chấp nhận nhận hàng nguyên tử; sai lệch chỉ ghi số giao/nhận/từ chối và lý do trên phiếu | UC-REC-01..02 | INV-01 / PP-02 |
| FR-REC-06 | Hoãn hủy/điều chỉnh phiếu đã xác nhận; vẫn cấm xóa vật lý chứng từ | UC-REC-01 | Chưa có backlog sửa sai / PP-02 |
| FR-INV-01..06 | Chấp nhận tồn/lô/hạn, biến động, chặn lô hết hạn, FEFO | UC-INV-01..02, UC-SAL-01 | INV-02, SAL-01 / PP-02, PP-03 |
| FR-INV-07..09, FR-REP-05 | Chấp nhận kiểm kê, chênh lệch và quản lý duyệt điều chỉnh | UC-INV-03..04 | MOB-01, SYN-01..02, INV-03 / PP-04, PP-05 |
| FR-INV-10 | Hoãn loại bỏ hàng vận hành; không suy từ bài đào tạo ra quyền sửa tồn thật | UC-INV-05 | Chưa có backlog / PP-03 |
| FR-INV-11 | Hoãn tồn kho/kệ riêng; MVP dùng một tồn chung | UC-INV-01 | Chưa có backlog vị trí / PP-04 |
| FR-SAL-01..08 | Chấp nhận bán hàng online, hóa đơn, thanh toán và giảm tồn nguyên tử | UC-SAL-01..02 | SAL-01 / PP-02, PP-07 |
| FR-SAL-09..10 | Hoãn hủy hóa đơn, trả hàng/hoàn tiền | UC-SAL-03; trả hàng chưa có UC riêng | Chưa có backlog / PP-02 |
| FR-PRO-01..03 | Chấp nhận khuyến mãi cơ bản, giữ giá đã bán | UC-PRO-01, UC-SAL-01 | PRO-01B / PP-03, PP-07 |
| FR-CUS-01..03 | Hoãn thành viên, tích/đổi điểm | UC-CUS-01..02 | Icebox / PP-07 |
| FR-REP-01..04, FR-AUD-01 | Chấp nhận báo cáo và ghi audit cho các nghiệp vụ được triển khai; hủy hóa đơn chưa thành cam kết | UC-REP-01..03 | REP-01, BE-02 và từng nghiệp vụ / PP-02, PP-06 |
| FR-AUD-02 | Hoãn giao diện tìm kiếm audit tổng quát; không hoãn lịch sử biến động FR-REP-04 | UC-REP-03 | Chưa có backlog riêng / PP-06 |
| FR-DEV-01..02 | Chấp nhận desktop/mobile và dữ liệu trung tâm | Các UC được cấp quyền | FE-01, MOB-01 / PP-01, PP-04 |
| FR-SYNC-01..04 | Chấp nhận chỉ cho kiểm kê offline, mã thao tác, retry và conflict; nhận hàng vẫn online | UC-SYNC-01..03, UC-INV-03 | SYN-01..02 / PP-05 |
| FR-SYNC-05 | Hoãn bán hàng offline nhiều thiết bị | UC-SAL-01 | Icebox / PP-05 |
| FR-TRN-01..08 | Chấp nhận một kịch bản hoàn chỉnh, dữ liệu/phiên/kết quả cách ly; không cam kết nhiều kịch bản | UC-TRN-01..05 | TRN-01..03 / PP-08, PP-09 |
| FR-TRN-09 | Loại nội dung kinh dị khỏi cam kết bản nộp hiện tại | Không thuộc UC bắt buộc | Icebox / ngoài pain point vận hành |

Các NFR về bảo mật, toàn vẹn, responsive, tin cậy, kiến trúc, backup/restore và cách ly đào tạo được chấp nhận trong phạm vi các nghiệp vụ ở trên; tiêu chí đo và bằng chứng đạt nằm ở QA của sprint tương ứng. NFR-PER-01 được hoãn việc chốt ngưỡng hai giây cho tới khi có môi trường/bộ dữ liệu đo; không cam kết thêm mục tiêu 100 người đồng thời từ Word. NFR-USA-01 chấp nhận thiết kế thao tác rõ và quét như bàn phím; chưa có bằng chứng kiểm thử người dùng thực tế. Ma trận pain point → requirement được giữ tại SRS mục 2.8.

## Những quyết định giữ nguyên và việc còn lại

- Bốn vai trò server là bán hàng, hàng hóa, quản lý, quản trị viên; người học là trạng thái nhân viên. Actor người học trên Use Case mô tả mục tiêu thực hành, không phải vai trò RBAC thứ năm.
- Giữ quyết định ngày 26–27/09/2026 về nhận hàng sai lệch, tiền/số lượng và cơ chế DB-01 review; bản ghi này không đổi kiểu dữ liệu, schema hoặc contract.
- Thiết kế 39 bảng đích không biến các chức năng tương lai trong Word thành cam kết sprint. Nếu nhóm muốn thêm thành viên, hủy hóa đơn, trả hàng hoặc xử lý hàng vận hành, cần quyết định phạm vi và cập nhật backlog trước.
- Owner/reviewer từng PR vẫn phải nghiệm thu code, contract và test; đóng REQ-01 không đóng QA-01, FE-01 hoặc các sprint.
- Trước khi đóng #2: ghi nguồn và xác nhận của Project Owner trên Issue, cả nhóm review bản cập nhật, tích hợp SRS và bản ghi qua PR, rồi liên kết bằng chứng kiểm tra tài liệu. Không ghi từng thành viên đã phê duyệt riêng khi chưa có chứng cứ.
