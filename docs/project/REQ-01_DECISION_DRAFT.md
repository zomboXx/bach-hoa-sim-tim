# REQ-01 — Bản chuẩn bị quyết định phạm vi

- Trạng thái: **Bản nháp lịch sử ngày 26/09/2026 — chưa được nhóm xác nhận tại thời điểm lập**.
- Bản thay thế: [REQ-01 ngày 29/09/2026](REQ-01_SCOPE_RECORD_2026-09-29.md), đã tích hợp qua PR #17 và đóng Issue #2. Các yêu cầu lập biên bản dưới đây lưu đề xuất cũ, không yêu cầu nhóm tạo lại cuộc họp.
- Ngày lập: 26/09/2026.
- Issue: [REQ-01 #2](https://github.com/zomboXx/bach-hoa-sim-tim/issues/2).
- Nguồn đối chiếu: [backlog](governance/BACKLOG.md), [SRS 0.1-draft](../product/analysis/02-danh-sach-yeu-cau.md), [thiết kế dữ liệu đề xuất](../architecture/database/01-thiet-ke-csdl-khai-niem.md).

Tài liệu này chỉ gom phương án để Project Owner và các thành viên quyết định. Các trạng thái trong SRS chưa đổi. Sau khi có biên bản xác nhận, cập nhật từng ID trong SRS và ghi ngày, người xác nhận, liên kết quyết định; không coi phương án dưới đây là phê duyệt.

## Quyết định đã nhận từ Project Owner

- Ngày 26/09/2026, Nguyễn Đức Phát xác nhận **bốn vai trò server**: bán hàng, hàng hóa, quản lý, quản trị viên. “Người học” là trạng thái của nhân viên, không phải vai trò thứ năm. Quyết định này thay thế cách ghi “năm vai trò” trong backlog và Sprint 1 kickoff trước ngày này. Contract session/RBAC vẫn cần mô tả quyền cụ thể và kiểm thử; REQ-01 vẫn cần cả nhóm xác nhận theo tiêu chí Issue #2.
- Cùng ngày, Project Owner xác nhận **tiền là số nguyên VND trong API và CSDL**. Quyết định này thay thế phần tiền `numeric(14,2)` của DB-10; thiết kế dữ liệu và ERD đã được điều chỉnh. Quy tắc làm tròn khi có số lượng lẻ hoặc giảm giá theo tỷ lệ còn cần chốt trước migration.
- Cùng ngày, Project Owner xác nhận **số lượng có tối đa ba chữ số thập phân** trong MVP. Thiết kế DB-10 giữ `numeric(14,3)`; quy tắc làm tròn thành tiền từng dòng được đề xuất trong tài liệu DB-01 và chờ review.
- Cùng ngày, Project Owner xác nhận MVP chỉ **ghi số lượng giao/nhận/từ chối và lý do trên phiếu nhận** khi hàng giao thiếu, thừa hoặc hư hỏng; chưa thêm quy trình điều chỉnh riêng. Phần này được bổ sung vào tiêu chí INV-01, còn trạng thái SRS chờ cả nhóm xác nhận.

Ngày 27/09/2026, Project Owner làm rõ rằng **bốn vai trò server là quyết định quan trọng cần giữ**; quy ước tiền nguyên VND và giới hạn ba chữ số thập phân có thể điều chỉnh nếu thiết kế DB-01 cho thấy phương án khác phù hợp hơn. Hai quy ước số học hiện là đầu vào đề xuất, không phải lý do giữ các Issue Sprint 1 ở trạng thái chờ. Nếu thay đổi, DB-01 phải ghi kiểu dữ liệu và quy tắc tính tiền thay thế trước khi tạo bảng nghiệp vụ.

## 1. Các yêu cầu P1 trong SRS

Đề nghị **chấp nhận cho phạm vi cuối kỳ** các nhóm sau, vì chúng cấu thành luồng đã ghi trong backlog. “Chấp nhận” tại đây là phương án cần nhóm duyệt, không có nghĩa hoàn thành triển khai.

| Nhóm | ID | Backlog liên quan |
|---|---|---|
| Tài khoản, quyền | FR-AUTH-01..04 | BE-02; cần chốt vai trò ở mục 3 |
| Danh mục | FR-CAT-01..05 | BE-03 |
| Nhận hàng | FR-REC-01..03, FR-REC-05 | BE-03, INV-01 |
| Tồn và kiểm kê | FR-INV-01..05, FR-INV-07..08 | INV-02, SYN-01, INV-03 |
| Bán hàng | FR-SAL-01..08 | SAL-01 |
| Khuyến mãi | FR-PRO-01..03 | PRO-01B |
| Báo cáo và audit | FR-REP-01..04, FR-AUD-01 | REP-01, BE-02; audit cần tiêu chí test cụ thể |
| Thiết bị và dữ liệu chung | FR-DEV-01..02 | FE-01, MOB-01 |

## 2. Các mục đang ghi “Cần khảo sát”

| ID | Phương án | Lý do và điều kiện trước khi đổi trạng thái SRS |
|---|---|---|
| FR-REC-04 | Project Owner chốt ghi số lượng và lý do trên phiếu nhận; chờ nhóm xác nhận trạng thái SRS | Không có quy trình điều chỉnh riêng trong MVP. |
| FR-REC-06 | Hoãn thao tác hủy/điều chỉnh phiếu | Chưa có backlog cho luồng sửa sai; giữ nguyên nguyên tắc không xóa chứng từ đã xác nhận. |
| FR-INV-06 | Chấp nhận FEFO | Baseline và SAL-01 đã yêu cầu chọn lô còn hạn theo FEFO. |
| FR-INV-09 | Chấp nhận duyệt điều chỉnh | INV-03 của Sprint 3 nêu quyền quản lý và biến động tồn. |
| FR-INV-10 | Hoãn nghiệp vụ loại bỏ hàng vận hành | Chưa có backlog triển khai; kịch bản đào tạo không tạo biến động vận hành. |
| FR-INV-11 | Hoãn | Backlog không có mục triển khai vị trí kho/kệ; thiết kế DB-05 dùng một tồn chung. |
| FR-SAL-09 | Hoãn luồng hủy hóa đơn | Chưa có backlog triển khai; vẫn giữ nguyên tắc không xóa chứng từ. |
| FR-SAL-10 | Hoãn | Trả hàng/hoàn tiền nằm ngoài hai luồng nghiệm thu MVP. |
| FR-CUS-01..03 | Hoãn | Khách hàng thành viên/điểm thưởng ở Icebox và DB-07 chưa đưa vào MVP. |
| FR-SYNC-02..04 | Chấp nhận cho **kiểm kê offline** | SYN-01 và SYN-02 của Sprint 3; không mở rộng sang bán hàng offline. |

Các mục P2/P3 khác không mang trạng thái “Cần khảo sát” sẽ được xét theo backlog tương ứng; bảng này chưa thay thế việc cập nhật toàn bộ SRS sau quyết định của nhóm.

## 3. Quyết định cần Project Owner và nhóm xác nhận

1. **Quyền từng vai trò:** [Issue #5](https://github.com/zomboXx/bach-hoa-sim-tim/issues/5) chỉ nêu ba vai trò vận hành; cần thêm quyền quản trị viên và quy tắc người học truy cập phiên đào tạo vào contract session/RBAC theo quyết định bốn vai trò ở trên.
2. **Quy tắc số học:** tiền nguyên VND và số lượng tối đa ba chữ số thập phân là phương án hiện có, có thể đổi khi DB-01 review. Nếu giữ phương án đó, cần chốt cách làm tròn thành tiền từng dòng, tiền giảm và trường hợp đúng nửa VND; nếu đổi, ghi phương án thay thế và test API/CSDL trước migration.
3. **Nguồn khảo sát:** [ma trận pain point → yêu cầu](../product/analysis/02-danh-sach-yeu-cau.md#28-ma-tran-truy-vet-muc-tieu--pain-point--yeu-cau) đã có, nhưng hiện là giả định khảo sát. Cần xác nhận nguồn thực tế/giảng viên và ghi bằng chứng trước khi đóng REQ-01.
4. **Biên bản:** cả nhóm xác nhận phương án chấp nhận/hoãn/loại cho các ID trên; ghi người, ngày và liên kết trong Issue #2. Chỉ sau đó cập nhật trạng thái SRS và bắt đầu chốt DB-01.
