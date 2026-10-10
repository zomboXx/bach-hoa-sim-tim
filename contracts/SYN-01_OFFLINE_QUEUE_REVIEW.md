# SYN-01 offline stocktake queue review

**Status:** Proposed — cần TV2 review trong Pull Request trước khi SYN-02 dùng làm consumer contract

**Owner:** TV4 — Lê Văn Chiến (@VanChien11-02)

**Reviewer:** TV2 — Nguyễn Văn Trung (@nguyentrung01ute-ui)

**Phụ thuộc:** MOB-01 (#43)

Tài liệu này ghi quyết định phía client cho Issue #44. Nó không đổi tiêu chí backlog, không tự đặt route/DTO/permission cho SYN-02 và không chứng minh provider đồng bộ đã tồn tại.

## Phạm vi queue

- IndexedDB riêng `simtim-stocktake-queue-v1` chỉ có object store `operations`; nhận hàng, bán hàng và thanh toán không dùng queue này.
- Mỗi thao tác giữ một `clientOperationId` UUID làm key, thời điểm đếm, payload theo lô và snapshot tồn lúc đếm.
- Queue lưu `actorId`, `organizationId`, `storeId`; không lưu Bearer token, mật khẩu hoặc response phiên.
- Lưu lại cùng payload đang `PENDING` trong cùng actor/organization/store trả lại thao tác cũ, không sinh UUID thứ hai.
- `CONFLICT` là trạng thái dừng: không tự retry, không tự đổi snapshot và không tự ghi đè tồn.

## Điều kiện retry

Trước mỗi lần gửi lại, client phải có phiên hiện tại và lần lượt kiểm tra:

1. organization/store của phiên khớp thao tác;
2. actor của phiên khớp người tạo thao tác;
3. phiên hiện tại còn quyền gửi kiểm kê;
4. thao tác vẫn là `PENDING`, không phải `CONFLICT`.

Sai bất kỳ điều kiện nào thì giữ nguyên thao tác trên thiết bị và hiển thị lý do khôi phục. Đổi lại đúng actor/store/quyền mới cho phép retry. Demo adapter chỉ mô phỏng bước nhận của server để browser test được lifecycle; API mode vẫn khóa ghi theo boundary MOB-01.

## Điểm SYN-02 phải chốt

TV2/TV4 cần review trong PR provider:

- route mở/đọc phiên kiểm kê và route gửi số đếm;
- permission write chính thức cho STOCK/MANAGER;
- DTO gồm `clientOperationId`, batch, số thực tế, snapshot/baseVersion và field error;
- kết quả idempotent khi cùng actor/scope/payload, cùng key nhưng khác actor/scope/payload;
- phản hồi conflict/version có đủ dữ liệu để kiểm lại;
- thời điểm xóa queue sau khi server xác nhận và cách đối soát response bị mất.

Cho tới khi các điểm trên được Accepted, client không gọi endpoint kiểm kê chưa tồn tại và không dùng `inventory.read` như quyền ghi.

## Bằng chứng consumer

`apps/web/tests/stocktake-offline-queue.spec.ts` chạy trên browser production build và kiểm:

- mất mạng → nhập → lưu lặp → reload → reconnect, chỉ một `clientOperationId`;
- đổi actor hoặc store thì retry bị chặn và thao tác vẫn còn;
- quay lại actor đúng thì retry dùng thao tác cũ, không tạo phiếu trùng;
- tồn thay đổi dẫn đến conflict, không ghi đè tồn và không nhân đôi lần đếm.

Consumer tests này không thay thế provider integration test PostgreSQL của SYN-02.
