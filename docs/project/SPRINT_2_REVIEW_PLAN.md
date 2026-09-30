# Sprint 2 — Kế hoạch review trước triển khai

- **Draft, 30/09/2026.** TV1 — Nguyễn Đức Phát điều phối; tài liệu này không mở Issue Sprint 2 hoặc triển khai migration/API.
- Phạm vi và owner lấy từ [backlog hiện hành](governance/BACKLOG.md). [ADR 0003](../architecture/adr/0003-application-architecture.md) quy định module theo nghiệp vụ; [DB-01](../architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md) là thiết kế dữ liệu đích đã được nhóm đồng ý. Tên giao tiếp cần review nằm trong [contract ranh giới](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md).
- Bộ review này ghi các quyết định và điểm giao cần chốt trước code; chi tiết migration, OpenAPI và ca QA do PR của item sở hữu bổ sung sau review.

## Quyết định nghiệp vụ đã có

| Quy tắc | Xác nhận |
|---|---|
| Bán được lô có hạn đúng ngày bán; cận hạn gồm hôm nay đến hôm nay + 7 ngày; ngày nghiệp vụ theo `Asia/Ho_Chi_Minh`. | Project Owner, 29/09/2026 |
| FEFO quyết định lô; mỗi phần lô chọn **một** khuyến mãi giảm tốt nhất, không cộng dồn. | Project Owner, 29/09/2026 |
| Sprint 2 chỉ ghi thanh toán CASH. | Project Owner, 29/09/2026 |
| Hóa đơn 0 VND vẫn hoàn tất và trừ tồn, kể cả khi giảm 100%. | Project Owner, 30/09/2026 |

Nền Sprint 1 dùng bốn vai trò server; người học là trạng thái nhân viên. Tiền biểu diễn bằng số nguyên VND, quantity tối đa ba chữ số thập phân theo quyết định DB-01; giá trị trên wire cần TV2/TV3 chốt trong contract. Nhận thiếu/thừa/hư hỏng ghi số lượng và lý do trên phiếu, chưa mở quy trình xử lý riêng.

## Phạm vi, đầu ra và thứ tự

| Thứ tự | Item | Owner / reviewer | Đầu ra cần review |
|---:|---|---|---|
| 1 | INV-01, P0 | TV2 / TV1 | Phiếu nhận, lô, balance, movement trong một transaction; migration riêng, provider test |
| 2 | INV-02, P0 | TV2 / TV4 | API đọc tồn/lô/hạn/biến động; public port cấp phân bổ FEFO cho sales |
| 3 | SAL-01, P0 | TV3 / TV2 | Quote và checkout CASH; hóa đơn snapshot, giảm tồn nguyên tử; backend và web có consumer test |
| 4 | REP-01, P0 | TV3 / TV2 | Báo cáo doanh thu/tồn từ chứng từ đã commit; test đối chiếu |
| 5 | QA-02, P0 | TV4 / TV1 | E2E nhận → tồn → bán → báo cáo trên DB sạch; rollback, quyền, FEFO và số tiền |
| Sau SAL-01 | PRO-01B, P1 | TV3 / TV1 | Khuyến mãi theo thời gian/phạm vi, regression cho quote/checkout; PR riêng |

P0 có thể hoàn thành khi PRO-01B chưa làm; khi đó sales chạy không khuyến mãi. Không đưa quản trị giá/tài khoản, hủy/hoàn tiền, điều chỉnh hàng hỏng, kiểm kê offline, hoặc kết quả đào tạo vào các PR này. Tạo sản phẩm/giá hợp lệ trước luồng demo bằng nền catalog hiện có và fixture có kiểm soát.

## Ranh giới để các nhánh không chồng việc

- TV2 sở hữu `services/api/.../inventory/` và migration nhận/tồn. TV3 sở hữu `.../sales/`, `.../reports/` và web feature bán/báo cáo. TV4 sở hữu kế hoạch, fixture và bằng chứng QA-02. Người sửa file chung nêu rõ trong mô tả PR.
- `App.vue`, auth/permission, `CHANGELOG.md`, `CONTRIBUTION_LOG.md`, contract và Flyway grants là file chung. Mỗi feature PR chỉ thêm phần cần cho feature đó; sau khi migration đã merge, mở migration kế tiếp để sửa.
- Sales gọi public interface của inventory trong **cùng transaction**; không import JPA entity/repository của inventory và không tự ghi balance. Inventory và sales thống nhất thứ tự khóa trước khi hai bên triển khai song song.
- Mỗi nhánh feature tạo từ `main` mới nhất, lấy cập nhật từ `main`, không đưa nhánh feature chưa merge của đồng đội vào PR của mình. Conflict trong nhật ký đóng góp giữ đủ các mục của từng người.

## Những điểm nhóm cần chốt trong review

1. **TV2 + TV3:** chữ ký public port, payload/response, permission, thứ tự khóa, cách chụp giá và lượng tại checkout; xem [contract ranh giới](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md).
2. **TV2 + TV3:** kiểu wire quantity, cách làm tròn tiền cho quantity lẻ, cơ chế chống ghi trùng khi POST timeout, và migration nào sở hữu FK ledger sang invoice. Đây là đề xuất kỹ thuật chưa được duyệt.
3. **TV4:** tập ca P0 và oracle độc lập: nhận sai lệch, hết hạn/FEFO, thiếu tồn, giá đổi, 0 VND, replay, rollback, sai quyền, đối chiếu báo cáo. Tách regression khuyến mãi P1.
4. **TV1:** ghi quyết định review có ngày và người xác nhận; khi các điểm giao đã thống nhất mới chuyển từng Issue sang Ready, không coi Draft là Done.

## Điều kiện trước khi merge từng feature

PR có owner/reviewer, Issue, contract thay đổi, migration sạch và upgrade (nếu có), provider/consumer test, CI/root gate, bằng chứng QA phù hợp. Chỉ ghi một API hoặc ca kiểm thử là hoàn tất sau khi code đã tích hợp và chạy trên nền mới.
