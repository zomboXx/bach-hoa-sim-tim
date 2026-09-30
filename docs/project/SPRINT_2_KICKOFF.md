# Sprint 2 — Kickoff và thứ tự tích hợp

- **Working baseline, 30/09/2026.** TV1 — Nguyễn Đức Phát điều phối. [Milestone Sprint 2](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/2) kết thúc ngày **07/10/2026** theo Project Owner; Issue là nơi owner cập nhật trạng thái/bằng chứng. Tài liệu này giữ quy tắc chung để bắt đầu code.
- Phạm vi và owner lấy từ [backlog hiện hành](governance/BACKLOG.md). [ADR 0003](../architecture/adr/0003-application-architecture.md) quy định module theo nghiệp vụ; [DB-01](../architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md) là thiết kế dữ liệu đích đã được nhóm đồng ý. Tên giao tiếp cần review nằm trong [contract ranh giới](../../contracts/SPRINT_2_BOUNDARY_DRAFT.md).
- Quy tắc kỹ thuật trong contract là mặc định cho các Issue Sprint 2. Owner đề xuất thay đổi qua PR và nhờ reviewer của module liên quan kiểm tra; không tự đổi wire/schema một phía.

## Quyết định nghiệp vụ đã có

| Quy tắc | Xác nhận |
|---|---|
| Bán được lô có hạn đúng ngày bán; cận hạn gồm hôm nay đến hôm nay + 7 ngày; ngày nghiệp vụ theo `Asia/Ho_Chi_Minh`. | Project Owner, 29/09/2026 |
| FEFO quyết định lô; mỗi phần lô chọn **một** khuyến mãi giảm tốt nhất, không cộng dồn. | Project Owner, 29/09/2026 |
| Sprint 2 chỉ ghi thanh toán CASH. | Project Owner, 29/09/2026 |
| Hóa đơn 0 VND vẫn hoàn tất và trừ tồn, kể cả khi giảm 100%. | Project Owner, 30/09/2026 |

Nền Sprint 1 dùng bốn vai trò server; người học là trạng thái nhân viên. Tiền biểu diễn bằng số nguyên VND, quantity tối đa ba chữ số thập phân theo quyết định DB-01; giá trị trên wire cần TV2/TV3 chốt trong contract. Nhận thiếu/thừa/hư hỏng ghi số lượng và lý do trên phiếu, chưa mở quy trình xử lý riêng.

## Phạm vi, đầu ra và thứ tự

| Thứ tự | Item | Owner | Reviewer | Đầu ra cần review |
|---:|---|---|---|---|
| 1 | [INV-01 #18](https://github.com/zomboXx/bach-hoa-sim-tim/issues/18), P0 | TV2 | TV1 | Phiếu nhận, lô, balance, movement trong một transaction; migration riêng, provider test |
| 2 | [INV-02 #19](https://github.com/zomboXx/bach-hoa-sim-tim/issues/19), P0 | TV2 | TV4 | API đọc tồn/lô/hạn/biến động; public port cấp phân bổ FEFO cho sales |
| 3 | [FE-02 #20](https://github.com/zomboXx/bach-hoa-sim-tim/issues/20), P0 | TV4 | TV3 | PWA API mode nhận hàng, xem phiếu/tồn/lô; demo mode tiếp tục chạy; consumer E2E |
| 4 | [SAL-01 #21](https://github.com/zomboXx/bach-hoa-sim-tim/issues/21), P0 | TV3 | TV2 | Quote và checkout CASH; hóa đơn snapshot, giảm tồn nguyên tử; backend và POS có consumer test |
| 5 | [REP-01 #22](https://github.com/zomboXx/bach-hoa-sim-tim/issues/22), P0 | TV2 | TV3 | Báo cáo doanh thu/tồn từ chứng từ đã commit; test đối chiếu |
| Xuyên suốt | [QA-02 #24](https://github.com/zomboXx/bach-hoa-sim-tim/issues/24), P0 | TV4 | TV1 | Kế hoạch/test fixture từ đầu, E2E sau REP-01; rollback, quyền, FEFO và số tiền |
| Sau SAL-01 | [PRO-01B #23](https://github.com/zomboXx/bach-hoa-sim-tim/issues/23), P1 | TV3 | TV1 | Khuyến mãi theo thời gian/phạm vi, regression cho quote/checkout; PR riêng |

FE-02 là phần giao diện của INV-01/02 được tách để có owner rõ ràng; không thêm chức năng mới ngoài luồng nhận/tồn đã có trong backlog. P0 có thể hoàn thành khi PRO-01B chưa làm; khi đó sales chạy không khuyến mãi. Không đưa quản trị giá/tài khoản, hủy/hoàn tiền, điều chỉnh hàng hỏng, kiểm kê offline, hoặc kết quả đào tạo vào các PR này. Tạo sản phẩm/giá hợp lệ trước luồng demo bằng nền catalog hiện có và fixture có kiểm soát.

## Ranh giới để các nhánh không chồng việc

- TV2 sở hữu `services/api/.../inventory/`, `.../reports/` và migration nhận/tồn. TV3 sở hữu `.../sales/`, web bán hàng và review web báo cáo. TV4 sở hữu web nhận/tồn, kế hoạch, fixture và bằng chứng QA-02. TV1 điều phối và review, không nhận thêm feature. Người sửa file chung nêu rõ trong mô tả PR.
- `App.vue`, auth/permission, `CHANGELOG.md`, `CONTRIBUTION_LOG.md`, contract và Flyway grants là file chung. Mỗi feature PR chỉ thêm phần cần cho feature đó; sau khi migration đã merge, mở migration kế tiếp để sửa.
- Sales gọi public interface của inventory trong **cùng transaction**; không import JPA entity/repository của inventory và không tự ghi balance. Inventory và sales thống nhất thứ tự khóa trước khi hai bên triển khai song song.
- Mỗi nhánh feature tạo từ `main` mới nhất, lấy cập nhật từ `main`, không đưa nhánh feature chưa merge của đồng đội vào PR của mình. Conflict trong nhật ký đóng góp giữ đủ các mục của từng người.

## Cách bắt đầu và điểm review

TV2 bắt đầu INV-01, sau INV-02 mới nhận REP-01 khi SAL-01 cung cấp dữ liệu bán đã commit. TV4 có thể dựng FE-02 adapter/UI bằng contract và mock response nhưng chỉ bật API mode khi provider test INV-01/02 đạt; đồng thời chuẩn bị fixture/test plan QA-02. TV3 tập trung SAL-01, review web FE-02/REP-01; PRO-01B là P1 sau các P0. SAL-01 chỉ nối giảm tồn sau public port INV-02. Một người tối đa một Issue `In Progress` theo Scrum của nhóm; các Issue phụ thuộc có thể ở Backlog/Ready để chuẩn bị. TV2 có ba Issue backend phụ thuộc nhau; nhóm kiểm lại năng lực và tiến độ trước khi coi toàn bộ P0 là cam kết chắc chắn.

```powershell
git fetch origin
git switch main
git pull --ff-only
git switch -c feature/INV-01-receipts # thay mã/tên theo Issue của mình
pwsh -File scripts/verify.ps1
```

Mỗi PR ghi Issue, owner/reviewer, contract/migration bị ảnh hưởng, lệnh test và kết quả. Với thay đổi wire hoặc schema, TV2/TV3 review cùng PR trước merge. Reviewer kiểm riêng permission, phạm vi store, số tiền, quantity, khóa, rollback và replay. Conflict trong `CONTRIBUTION_LOG.md` giữ đủ mục của từng người.

TV4 đối chiếu độc lập: nhận sai lệch, hết hạn/FEFO, thiếu tồn, giá đổi, 0 VND, replay, rollback, sai quyền và báo cáo; khuyến mãi P1 có regression riêng. TV1 điều phối thứ tự merge và ghi quyết định thay đổi contract trên Issue/PR, không tự gọi tính năng Done từ bản kế hoạch.

## Điều kiện trước khi merge từng feature

PR có owner/reviewer, Issue, contract thay đổi, migration sạch và upgrade (nếu có), provider/consumer test, CI/root gate, bằng chứng QA phù hợp. Chỉ ghi một API hoặc ca kiểm thử là hoàn tất sau khi code đã tích hợp và chạy trên nền mới.
