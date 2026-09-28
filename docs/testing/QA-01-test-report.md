# QA-01 — Báo cáo kiểm thử API nền tảng

- Trạng thái: **Checkpoint — chưa đủ điều kiện kết luận**
- Ngày báo cáo: 2026-09-28
- Owner QA: TV4 — Lê Văn Chiến
- Commit checkout QA: `cced1a1`
- Phạm vi dự kiến: `BE-02`, `BE-03`

## 1. Kết luận hiện tại

QA-01 **chưa đạt và cũng chưa thất bại** vì các API thuộc dependency chưa có đầy đủ trong checkout QA:

- `main` local có `DB-01/BE-01`, chưa có implementation `BE-02/BE-03`.
- Handoff BE-02 báo 10 API tests đạt tại commit `4801d07`, nhưng TV4 chưa có code/branch đó để đối chiếu từng test method và chạy độc lập.
- `BE-03` chưa có API thật, nên duplicate/validation/catalog CRUD đều đang `Blocked`.

Không dùng 4 Playwright tests của PWA Sprint 0 hoặc controller catalog test fixture của BE-02 làm bằng chứng API CRUD của BE-03.

## 2. Tổng hợp

| Nhóm | Planned | QA Pass | QA Fail | Owner reported | Blocked/TBD |
|---|---:|---:|---:|---:|---:|
| BE-02 auth/RBAC/security | 17 | 0 | 0 | 16 | 1 |
| BE-03 catalog/supplier | 12 | 0 | 0 | 0 | 12 |
| Tổng | 29 | 0 | 0 | 16 | 13 |

Chi tiết từng dòng nằm tại [QA-01-traceability.md](QA-01-traceability.md).

## 3. Bằng chứng đã nhận nhưng chưa được QA xác nhận

Theo handoff BE-02:

- 10 API tests cùng root verification đã đạt.
- Migration database sạch và upgrade từ BE-01 V3 lên V4 đã đạt.
- CI chạy Java 21/PostgreSQL 17 tại commit `4801d07`.
- Test được báo gồm đăng nhập sai, bốn role, training flag, logout/expiry, account khóa/mất role, thu hồi permission, store scope, token hash, demo opt-in và rate limit.

Link được cung cấp: [CI run 36404996237](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/36404996237).

Các nội dung trên chỉ là `Reported` cho đến khi TV4:

1. Checkout đúng commit/PR.
2. Đối chiếu OpenAPI/permission matrix.
3. Ghi đúng tên class/method test vào traceability.
4. Chạy test trên môi trường phù hợp.
5. Lưu lệnh, kết quả và commit làm evidence.

## 4. Lệnh cần chạy khi dependency sẵn sàng

Lệnh chính xác của backend phải theo `services/api/README.md` tại commit BE-02/BE-03. Kế hoạch tối thiểu:

```powershell
pwsh -File scripts/setup.ps1
pwsh -File scripts/verify.ps1
```

Nếu root gate cung cấp tham số bỏ qua cài đặt hoặc module gate riêng, report phải ghi đúng lệnh thực tế đã dùng; không thay bằng câu “test đều đạt”.

## 5. Các bước còn lại để đóng QA-01

- [ ] BE-02 có PR/commit checkout được và contract ghi trạng thái rõ ràng.
- [ ] Điền tên automated test chính xác cho 17 dòng BE-02.
- [ ] TV4 chạy độc lập test BE-02 và cập nhật `Pass/Fail`.
- [ ] Reviewer chốt permission matrix và các câu hỏi `TBD`.
- [ ] BE-03 cung cấp OpenAPI, migration/fixture và provider tests.
- [ ] Cập nhật 12 dòng BE-03 bằng endpoint, error code và test method thật.
- [ ] Chạy test duplicate product code/barcode và validation trên API thật.
- [ ] Kiểm tra request bị từ chối không để lại dữ liệu.
- [ ] Tạo Bug Issue cho mọi sai lệch chưa được xử lý.
- [ ] Root verification và CI đạt tại commit cuối.
- [ ] Reviewer chấp nhận test plan, matrix và final report.

## 6. Rủi ro còn lại

- Permission matrix BE-02 vẫn Draft; đặc biệt quyền ghi catalog của `STOCK` và scope của `ADMIN` cần review.
- Chưa biết exact endpoint, operation ID, status/error code và field validation của BE-03.
- CI của owner không thay thế việc QA chạy độc lập và kiểm tra hậu điều kiện database.
- Nếu BE-02/BE-03 đổi contract sau khi test được viết, traceability và consumer test phải cập nhật cùng thay đổi.

## 7. Mẫu cập nhật sau lần chạy tiếp theo

```text
Commit/API version:
Môi trường:
Lệnh đã chạy:
Kết quả: total / pass / fail / blocked
Bug Issue:
Evidence/CI:
Rủi ro còn lại:
```
