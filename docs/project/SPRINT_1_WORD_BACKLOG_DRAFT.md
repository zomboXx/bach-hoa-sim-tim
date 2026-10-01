# Đối chiếu backlog Sprint 1 với báo cáo Word

- Trạng thái hiện tại: **Bản làm việc lịch sử ngày 26/09/2026**. Phần đã chốt nằm trong [REQ-01 ngày 29/09](REQ-01_SCOPE_RECORD_2026-09-29.md) và [backlog hiện hành](governance/BACKLOG.md); các ứng viên chưa có Issue/sprint vẫn chỉ là đầu vào refinement, không là cam kết triển khai.
- Ngày lập: 26/09/2026.
- Nguồn: bản làm việc `Nhom04_BaoCaoHoanChinh.docx` do Project Owner cung cấp; [backlog hiện hành](governance/BACKLOG.md), [Sprint 1 kickoff](SPRINT_1_KICKOFF.md) và [quyết định REQ-01](REQ-01_DECISION_DRAFT.md).

Báo cáo Word phân tích nhiều nghiệp vụ hơn các Issue đã lên lịch. Việc đưa một yêu cầu vào backlog giúp nhóm theo dõi và ưu tiên; chỉ khi có owner, reviewer, dependency, tiêu chí chấp nhận và sprint được nhóm chốt thì yêu cầu đó mới là cam kết triển khai. Ngày 27/09/2026, chi tiết của các Issue Sprint 1 và hai ứng viên ACC-01/PRC-01 đã được đưa vào [backlog](governance/BACKLOG.md) để review. Sprint 1 sau đó đã đóng 7/7 Issue; bảng dưới đây giữ nguyên đối chiếu tại thời điểm lập và không thay trạng thái Issue hiện hành.

## Các Issue Sprint 1 đã có

| Issue | Chi tiết từ Word cần giữ khi hoàn thiện Issue/contract | Ranh giới của Sprint 1 |
|---|---|---|
| REQ-01 | Đối chiếu yêu cầu với pain point và Use Case; ghi rõ chấp nhận, hoãn hoặc loại từng mục cần khảo sát. Phân biệt bốn vai trò server với tác nhân người học. | Cần biên bản nhóm và nguồn khảo sát; bản Word là tài liệu phân tích, chưa là bằng chứng nhóm đã duyệt. |
| DB-01 | Chốt khóa, quan hệ, trạng thái, ràng buộc cho tài khoản/quyền, danh mục/giá, nhà cung cấp, nhận hàng, tồn, bán hàng và kiểm kê. Bản Word Chương 4 là đầu vào data dictionary. | Đánh dấu riêng bảng/thuộc tính của tính năng chưa cam kết; không tự đưa tất cả bảng trong Word vào migration MVP. Tiền nguyên VND và số lượng tối đa ba chữ số thập phân là phương án hiện có, có thể điều chỉnh trong review DB-01; ghi kiểu cột và quy tắc làm tròn được chọn trước migration nghiệp vụ. |
| BE-01 | Môi trường sạch khởi động được Java/Spring Boot, PostgreSQL và Flyway; migration có dữ liệu demo tối thiểu và health check. | Chỉ tạo schema đã được DB-01 chốt; không lấy toàn bộ Chương 4 làm migration đầu tiên. |
| BE-02 | Xác thực server, hash mật khẩu, bốn vai trò bán hàng/hàng hóa/quản lý/quản trị viên, từ chối truy cập thiếu quyền và kiểm thử đăng nhập sai. | Người học là trạng thái/quyền truy cập đào tạo của nhân viên, không phải vai trò server thứ năm. Quản trị vòng đời tài khoản được tách thành quyết định ưu tiên ở bảng dưới. |
| BE-03 | Danh mục, sản phẩm, mã/mã vạch, nhà cung cấp và trạng thái; tìm theo mã, mã vạch hoặc tên; từ chối trùng và trả lỗi validation có cấu trúc. | Quy tắc giá phiên bản hóa trong Word cần contract/data dictionary trước khi thêm API thay đổi giá. |
| FE-01 | Đăng nhập qua API, trạng thái chờ/lỗi mạng và điều hướng theo quyền; giữ adapter demo để kiểm thử hồi quy. | Server là nguồn quyền có thẩm quyền; chưa chuyển luồng bán/nhận hàng sang API trong Issue này. |
| QA-01 | Test đăng nhập sai, truy cập sai quyền, dữ liệu trùng/không hợp lệ và ma trận yêu cầu–test. | Chỉ đánh dấu test API đạt khi API liên quan chạy được; kết quả kiểm thử PWA demo không thay thế test server. |

## Chi tiết Word chưa có vị trí cam kết rõ trong Sprint 1

| Nội dung trong Word | Phương án ghi vào backlog | Quyết định còn cần |
|---|---|---|
| Quản trị viên tạo, khóa/mở khóa tài khoản và gán vai trò | Ghi thành backlog item riêng sau BE-02; ứng viên MVP vì yêu cầu tài khoản cá nhân cần cách cấp và thu hồi. | Chốt sprint, owner/reviewer và phạm vi API quản trị; không gộp ngầm vào tiêu chí đăng nhập. |
| Giá bán có hiệu lực theo thời gian | Ghi điều kiện thiết kế ở DB-01 và backlog item catalog/giá sau BE-03. | Chốt cần nhiều phiên bản giá trong MVP hay một giá hiện hành và snapshot trên hóa đơn. |
| Thành viên và tích điểm | Ghi thành backlog item riêng, ứng viên sau MVP; chưa chốt sprint. | Chỉ nâng vào MVP sau quyết định REQ-01; không để hóa đơn Sprint 2 phụ thuộc ngầm vào bảng điểm. |
| Tích hợp dịch vụ thanh toán điện tử và đối soát | Ghi thành backlog item riêng, ứng viên sau MVP; MVP hiện chỉ lưu phương thức/số tiền theo contract được chốt. | Chọn nhà cung cấp và quy trình hoàn/đối soát nếu mở phạm vi. |
| Hủy hóa đơn | Ghi thành backlog item riêng phụ thuộc SAL-01; chưa chốt sprint. | Chốt quy tắc hoàn tồn theo lô, trạng thái thanh toán và quyền duyệt. |
| Xử lý hàng hỏng/hết hạn trong vận hành | Ghi thành backlog item riêng phụ thuộc INV-02; chưa chốt sprint. | Chốt trạng thái hàng chờ xử lý, chứng từ giảm tồn và quyền duyệt; kịch bản đào tạo không thay thế nghiệp vụ vận hành. |
| Kiểm kê offline, đồng bộ xung đột và duyệt tồn | Giữ ở Sprint 3 theo SYN-01, SYN-02 và INV-03. | Sprint 1 chỉ thiết kế ràng buộc/version cần thiết trong DB-01. |
| Một kịch bản đào tạo và lưu kết quả | Giữ ở Sprint 4 theo TRN-01..03. | Bốn vai trò server không tạo vai trò người học thứ năm. |
| Mục tiêu 100 người dùng đồng thời trong Word | Ghi thành NFR đề xuất, chưa thành tiêu chí nghiệm thu MVP. | Cần môi trường, bộ dữ liệu và phép đo trước khi cam kết con số. |

## Cách cập nhật từng sprint

1. Chốt REQ-01 và các khoảng trống Sprint 1 ở trên với Project Owner, owner module và reviewer; ghi quyết định trong Issue/biên bản.
2. Cập nhật đúng các dòng Sprint 1 trong backlog và Issue tương ứng; giữ trạng thái đề xuất/đã cam kết rõ ràng.
3. Khi Sprint 1 đã có contract và migration nguồn, đối chiếu tiếp Chương 1, 3, 4 của Word với Sprint 2; sau đó mới đến Sprint 3 và Sprint 4.
4. Chỉnh bản Word theo các quyết định đã chốt, phân biệt thiết kế mục tiêu với chức năng đã chạy, và giữ ghi chú ngày thay đổi phạm vi.
