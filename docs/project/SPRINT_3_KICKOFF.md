# Sprint 3 kickoff — Kiểm kê hiện trường

**Bản sửa để Project Owner rà soát, 08/10/2026; chưa công bố.** Nguồn phạm vi là [BACKLOG.md trên `main`](governance/BACKLOG.md#sprint-3--hiện-trường-và-kiểm-kê), [quyết định REQ-01](REQ-01_SCOPE_RECORD_2026-09-29.md) và [Scrum DoD](governance/SCRUM.md). Sprint 3 giữ đúng năm mã đã chốt: **MOB-01, SYN-01, SYN-02, INV-03, QA-03**. Mục tiêu: nhân viên kiểm kê theo lô trên điện thoại, lưu số đếm khi mất mạng, đồng bộ có phát hiện xung đột và để quản lý duyệt chênh lệch trên dữ liệu trung tâm.

Kế hoạch thay thế P1 bằng sáu Issue S3-* trong [PR #42](https://github.com/zomboXx/bach-hoa-sim-tim/pull/42) là sai. PR đó đã chuyển Draft; Issue #36–#41 đã đóng dưới lý do phạm vi chưa được duyệt. Sáu ngoại lệ QA-02 của Sprint 2 vẫn theo dõi tại [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) và [bản nghiệm thu ngày 07/10](SPRINT_2_ACCEPTANCE_2026-10-07.md), **chưa tự trở thành đầu việc Sprint 3**. Ngày kết thúc Sprint 3 và cách ưu tiên các lỗi #34 cạnh năm mục đã chốt cần Project Owner xác nhận trước khi gắn milestone/assignee.

## Phân công và phụ thuộc đã có trong backlog

| Mã | Kết quả | Ưu tiên | Owner | Reviewer | Phụ thuộc đã ghi |
|---|---|---:|---|---|---|
| MOB-01 | Giao diện kiểm kê responsive | P1 | TV4 — Chiến | TV3 — Thi | INV-02 |
| SYN-01 | Hàng đợi kiểm kê offline | P1 | TV4 — Chiến | TV2 — Trung | MOB-01 |
| SYN-02 | API đồng bộ và xử lý xung đột | P1 | TV2 — Trung | TV4 — Chiến | SYN-01 |
| INV-03 | Duyệt điều chỉnh tồn | P1 | TV2 — Trung | TV1 — Phát | SYN-02 |
| QA-03 | Test desktop/mobile/offline | P1 | TV4 — Chiến | TV3 — Thi | INV-03 |

TV1 điều phối và review, không nhận feature code. Một owner chỉ giữ một Issue `In Progress` tại một thời điểm theo [quy trình nhóm](governance/GITHUB_SETUP.md#5-quy-tắc-vận-hành). Bảng trên giữ quyết định backlog; việc chia PR hoặc thêm reviewer cho contract được thống nhất trong Issue, không tự đổi owner.

## Điểm giao phải review trước khi triển khai

- **Trạng thái hiện tại:** PWA Sprint 0 có kiểm kê **demo trên thiết bị**; API mode chưa có feature kiểm kê. Migrations đang chạy có `inventory_balances.version` và movement `ADJUSTMENT`, nhưng chưa có bảng `stocktakes`, `stocktake_lines` hoặc `sync.processed_operations`. [DB-01 target schema](../architecture/database/DB-01_PHYSICAL_SCHEMA_DRAFT.md) và [DDL đề xuất](../architecture/database/DB-01_schema_proposal.sql) là thiết kế đích, không phải migration đã triển khai.
- **TV2 + TV4 review contract:** xác định cách mở/đọc phiên, gửi số đếm theo lô, `clientOperationId`, phiên bản tồn gốc, phản hồi khi retry và khi tồn đã đổi. Chốt route, DTO, phạm vi organization/store, quyền STOCK/MANAGER, lỗi có cấu trúc, và consumer/provider tests trước khi nối API mode. Không coi tên bảng/trường trong DDL đề xuất là wire đã Accepted.
- **Offline chỉ cho kiểm kê:** IndexedDB giữ bản nháp/hàng đợi qua reload; không lưu Bearer token bền; khi đăng nhập lại phải kiểm tra đúng store/actor/quyền trước khi gửi. Không dùng queue này cho nhận hàng, bán hàng hay thanh toán. Retry cùng thao tác không tạo phiếu trùng; xung đột yêu cầu kiểm lại, không ghi đè tự động.
- **TV2 + TV1 review dữ liệu:** migration Flyway mới cho phiên/dòng kiểm kê và chống xử lý lặp, khóa/FK cùng organization/store/batch, version và audit. Quyết định tên loại movement tham chiếu kiểm kê trong migration mới: schema hiện hành dùng `ADJUSTMENT`, DDL đích đề xuất `STOCKTAKE_ADJUSTMENT`; không sửa migration đã chạy hoặc giả vờ hai tên đã tương thích.
- **Số lượng theo đơn vị:** giao diện demo hiện chỉ nhận số nguyên; sản phẩm KG được phép tối đa ba chữ số thập phân. MOB-01/SYN-02 phải kiểm tra precision theo unit cho số đếm, đồng thời giữ S2-GAP-01 ở #34 cho receipt/sale đến khi bằng chứng đầy đủ. Không đánh dấu GAP đó Pass chỉ từ kiểm kê.

## Tiêu chí bàn giao theo mã

Các tiêu chí chi tiết dưới đây là phần refinement đề xuất để owner/reviewer kiểm tra trước khi mở Issue; chúng chưa thay tiêu chí đã chốt trong backlog.

### MOB-01 — TV4, TV3 review

- Màn hình kiểm kê dùng được trên viewport điện thoại và desktop đã công bố, không cuộn ngang; chọn đúng lô/store theo dữ liệu server ở API mode và nhập số lượng theo precision đơn vị.
- Hiển thị số hệ thống ở thời điểm bắt đầu, số thực tế/chênh lệch, trạng thái lưu/chờ đồng bộ/xung đột/chờ duyệt; không cho người thiếu quyền thao tác. Demo mode giữ regression.
- Consumer test cho nhập hợp lệ/sai, loading/401/403 và viewport; tích hợp live sau khi TV2/TV4 chốt contract.

### SYN-01 — TV4, TV2 review

- Chỉ bản nháp kiểm kê được lưu offline và còn sau reload. `clientOperationId` ổn định cho mỗi thao tác; không tạo thao tác mới khi gửi lại cùng nội dung.
- Khi reconnect, chỉ đồng bộ trong đúng session/store/quyền; trạng thái chưa xác định hoặc conflict được hiện rõ, không tự ghi đè và không tự xác nhận duyệt.
- Browser test offline → reload → reconnect, queue nhiều thao tác, logout/login khác scope và lỗi mạng. Việc POST tự động hay retry thủ công phải theo contract idempotency được review; không suy từ demo hiện tại.

### SYN-02 — TV2, TV4 review

- API và migration tiến lên ghi phiên/dòng kiểm kê, nhận `clientOperationId`, đối chiếu phiên bản tồn gốc và trả lại cùng kết quả khi retry hợp lệ; khác nội dung/actor/scope bị từ chối, không lộ dữ liệu store khác.
- Nếu balance/version đổi từ lúc lấy snapshot, trả conflict có đủ thông tin để kiểm lại; không cập nhật tồn và không tạo adjustment khi chỉ gửi số đếm.
- Provider integration tests trên PostgreSQL sạch cho idempotency, cạnh tranh, scope, conflict và rollback; OpenAPI/consumer test được review cùng thay đổi.

### INV-03 — TV2, TV1 review

- Chỉ MANAGER (và ADMIN nếu ma trận quyền chấp nhận) duyệt chênh lệch của phiếu đã gửi, trong đúng store; không duyệt hai lần.
- Trong một transaction, kiểm tra version/balance còn phù hợp, cập nhật balance và ghi movement/audit có nguồn theo từng lô. Nếu không phù hợp, từ chối duyệt và yêu cầu xử lý conflict; không âm tồn, không ghi một phần.
- Provider tests cho tăng/giảm, count bằng tồn, thiếu quyền, duyệt lặp, tồn đổi và rollback; đối chiếu balance với ledger độc lập.

### QA-03 — TV4, TV3 review

- Test tự động luồng **API/PostgreSQL thật** trên database sạch: nhân viên mở/nhập kiểm kê mobile, mất mạng, reload, reconnect, quản lý xem chênh lệch và duyệt, tồn/movement khớp. Có trường hợp tồn thay đổi trước sync và trước duyệt.
- Kiểm desktop/mobile không cuộn ngang, phạm vi quyền, retry không trùng, không gửi nghiệp vụ khác offline và dữ liệu đào tạo không thay đổi tồn vận hành.
- Báo cáo nối requirement → contract → test → evidence, chỉ ghi Pass trên source đã tích hợp; QA-02 ngày 07/10 và #34 vẫn là hồ sơ riêng.

## Cổng tích hợp và việc còn cần chốt

Mỗi PR phải qua `pwsh -File scripts/verify.ps1`, CI policy/web/API/Compose và reviewer theo [DoD](governance/SCRUM.md). Provider migration/contract cần review trước khi consumer dựa vào; QA chạy trên code tích hợp sau cùng. [Milestone #3](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/3) hiện tạm dừng vì được tạo theo kế hoạch sai; không coi hạn 14/10 hay Issue #36–#41 là cam kết của năm mã trên.

Hai điểm cần Project Owner xác nhận trước khi công bố bản sửa: (1) giữ năm mã trên là toàn bộ cam kết Sprint 3 hay bổ sung một phần lỗi #34 theo ưu tiên; (2) hạn kết thúc Sprint 3. Nếu vẫn giữ nhịp một tuần sau Sprint 2, ngày 14/10 là **đề xuất**, chưa phải quyết định đã ghi nhận.
