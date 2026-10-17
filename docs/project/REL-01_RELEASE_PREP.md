# REL-01 — Chuẩn bị bản chạy thử và vấn đáp

**Trạng thái: Working, ngày 14/10/2026.** Owner: TV1 — Nguyễn Đức Phát; reviewer: cả nhóm. Tài liệu này chuẩn bị cho [REL-01 #59](https://github.com/zomboXx/bach-hoa-sim-tim/issues/59), **chưa xác nhận release candidate, chưa tạo tag hoặc video**. Chỉ chốt sau [DOC-01 #58](https://github.com/zomboXx/bach-hoa-sim-tim/issues/58) và [QA-04 #57](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57) trên cùng commit tích hợp.

## Cổng chọn release candidate

| Bằng chứng phải gắn đúng một commit | Trạng thái hiện tại | Người xác nhận |
|---|---|---|
| TRN-01/02/03 merge, test browser/provider/consumer và luật chấm được review | Chưa đủ; [TRN-01 #54](https://github.com/zomboXx/bach-hoa-sim-tim/issues/54), [TRN-02 #55](https://github.com/zomboXx/bach-hoa-sim-tim/issues/55), [TRN-03 #56](https://github.com/zomboXx/bach-hoa-sim-tim/issues/56) còn mở | TV1, TV2, TV3, TV4 theo Issue |
| QA-03, QA-04 có test report với SHA, môi trường, expected/actual và bằng chứng backup/restore | [QA-03 #47](https://github.com/zomboXx/bach-hoa-sim-tim/issues/47) và [QA-04 #57](https://github.com/zomboXx/bach-hoa-sim-tim/issues/57) còn mở | TV4, TV2; TV1 xác nhận |
| Sáu ngoại lệ QA-02 được xử lý hoặc Project Owner ghi quyết định chấp nhận rủi ro cụ thể | [#34](https://github.com/zomboXx/bach-hoa-sim-tim/issues/34) còn mở; [PR #53](https://github.com/zomboXx/bach-hoa-sim-tim/pull/53) là checkpoint tái hiện, chưa sửa lỗi | TV1 và reviewer từng GAP |
| DOC-01 đối chiếu báo cáo Word, [ma trận bản làm việc](../testing/DOC-01-traceability-working.md), đóng góp và ngoại lệ với commit cuối | Chưa đủ bằng chứng QA-04 | TV1; cả nhóm review |
| Root gate và CI đạt trên chính commit được chọn; Compose sạch lên web/API/PostgreSQL và demo theo kịch bản dưới | Chưa chọn commit ứng viên | TV1; cả nhóm review |
| Rà soát `npm audit` tại commit ứng viên và ghi quyết định với cảnh báo còn lại | Lượt local ngày 14/10 báo 1 High ở `source-map-js` 1.2.1; chưa đánh giá đường khai thác trong bản build hoặc sửa dependency | TV1 phối hợp TV3/TV2 |

Không điền Pass từ CI của nhánh khác hoặc từ [baseline `5435d30`](https://github.com/zomboXx/bach-hoa-sim-tim/commit/5435d30b1d62227c4f6cfd87e59a8ebd66b0e92a). Khi chọn ứng viên, ghi SHA đầy đủ, tag dự kiến, ngày, link CI và báo cáo QA vào Issue #59 trước khi tạo tag. Tag chỉ tạo sau khi nhóm nghiệm thu.

## Lượt chạy sạch cần ghi hình/bằng chứng

1. Tạo project Compose **mới** với file env local riêng, mật khẩu riêng và cổng host còn trống; không dùng lại named volume demo đang chứa dữ liệu của thành viên. Làm theo [hướng dẫn Compose](../../infra/README.md). Ghi `docker compose version`, phiên bản Docker, PostgreSQL, Java và trình duyệt; không chụp mật khẩu hoặc token.
2. Chạy `pwsh -File scripts/verify.ps1` tại commit ứng viên. Lưu SHA, tổng số test web/API/backend và link CI. Nếu test bị chặn bởi môi trường, ghi `Blocked` cùng nguyên nhân; không ghi Pass.
3. Trong cùng phiên PowerShell, đặt `$project = 'simtim-rc-' + (Get-Date -Format yyyyMMddHHmmss)`, rồi chạy `docker compose -p $project --env-file infra/.env -f infra/compose.demo.yml up --build -d --wait`. Dùng cùng `-p $project` khi kiểm `ps`; gọi `/actuator/health` ở `API_PORT` đã chọn, mở web ở `WEB_PORT`. Ghi ảnh màn hình hoặc log đã che thông tin nhạy cảm.
4. Theo [hướng dẫn Compose](../../infra/README.md), đăng nhập tài khoản demo server `manager`, xem sản phẩm/lô/tồn, tạo một phiếu nhận hợp lệ và đọc lại số dư/biến động. Chỉ trình diễn checkout, kiểm kê offline và báo cáo theo trạng thái QA đã xác nhận ở commit ứng viên; không dùng demo mode làm bằng chứng giao dịch API.
5. Sau khi TRN-03 tích hợp, đăng nhập người học có quyền, hoàn thành một ca, tải lại trang và đọc kết quả từ API/PostgreSQL. Chạy ca sai và chơi lại; đối soát tồn, movement, hóa đơn và doanh thu trước/sau. Hiện bản chơi thử trong PWA chỉ giữ trạng thái ở bộ nhớ trình duyệt, nên bước này **Blocked** cho tới khi có mã và test TRN-03.
6. TV2 cung cấp lệnh và kết quả backup/restore Compose sạch theo QA-04. TV1 đối chiếu dữ liệu phục hồi và gắn bằng chứng vào Issue #59; bản chạy demo của TV1 không thay thế test report QA-04.

## Dàn ý vấn đáp và video dự phòng

- **Phạm vi:** bán hàng, nhận/tồn, kiểm kê và một ca đào tạo 2D; giải thích chức năng đã tích hợp bằng ma trận DOC-01, nêu ngoại lệ còn mở theo #34/#47/QA-04.
- **Kiến trúc:** [ADR 0004](../architecture/adr/0004-training-2d-web.md) chọn Vue/SVG trong PWA để giữ một stack; module đào tạo và dữ liệu kết quả tách giao dịch bán/nhận/tồn.
- **Dữ liệu và quyền:** PostgreSQL/Flyway cho dữ liệu vận hành; server lấy actor, tổ chức, cửa hàng từ session, không tin điểm client. Khi TRN-03 được chấp nhận, giải thích quyền người học/MANAGER/ADMIN và retry không cộng điểm hai lần bằng test thực tế.
- **Tình huống lỗi:** mất mạng khi kiểm kê, gửi lại thao tác, thay đổi tồn khi chờ duyệt, chuyển cửa hàng của nhân viên, khôi phục DB; chỉ nói kết quả đã được QA xác nhận.
- **Video:** quay đúng commit ứng viên sau khi Compose sạch và QA đạt; ghi SHA/phiên bản ở đầu video, che secret, lưu đường dẫn nội bộ trong Issue #59. Chưa quay video khi kịch bản còn phụ thuộc TRN-03/QA-04.
