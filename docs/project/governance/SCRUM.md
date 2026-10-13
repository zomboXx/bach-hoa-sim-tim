# Scrum rút gọn cho đồ án

Nhóm đồng bộ, review và cập nhật báo cáo theo nhịp tuần. Ngày kết thúc của từng milestone do Project Owner chốt riêng; [Sprint 3](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/3) và [Sprint 4](https://github.com/zomboXx/bach-hoa-sim-tim/milestone/4) hiện không có ngày kết thúc. Sprint 4 theo [kickoff ngày 13/10/2026](../SPRINT_4_KICKOFF.md) giữ sáu mã backlog; QA-03 #47 và các ngoại lệ #34 vẫn theo dõi riêng. Nhịp báo cáo không tự làm Issue hoặc sprint đạt Done khi chưa có bằng chứng theo tiêu chí dưới đây. Product Owner và Scrum Master do trưởng nhóm kiêm nhiệm trong phạm vi môn học; giảng viên/người dùng khảo sát là nguồn phản hồi, không bị gán vai trò nội bộ của nhóm.

## Nhịp làm việc mỗi tuần

| Thời điểm | Hoạt động | Thời lượng | Đầu ra |
|---|---|---:|---|
| Đầu tuần | Sprint planning | 45–60 phút | Mục tiêu tuần, owner, reviewer, task ở `Ready` |
| Giữa tuần | Đồng bộ blocker | 15–20 phút | Quyết định kỹ thuật và việc cần hỗ trợ |
| Cuối tuần | Review + demo | 45–60 phút | Bản chạy/sơ đồ được review, kết quả test |
| Sau review | Retrospective | 15 phút | Một việc giữ lại, một việc cần sửa |
| Sau retrospective | Chốt báo cáo tuần | 20–30 phút | Báo cáo và kế hoạch tuần kế tiếp |

Mỗi ngày thành viên cập nhật Issue đang làm bằng một dòng: kết quả mới, bước tiếp theo và blocker. GitHub Project giữ trạng thái `Backlog`, `Ready`, `In Progress`, `In Review`, `Blocked`, `Done`; không nhân đôi trạng thái này trong tài liệu. Không mở cuộc họp riêng nếu Issue đã đủ thông tin.

Trong Sprint 4, owner chỉ kéo Issue sang `In Progress` khi phụ thuộc và điểm giao cần thiết đã được review; mỗi người tối đa một Issue ở trạng thái này. Theo điều chỉnh ngày 14/10/2026, TV4 ưu tiên hoàn tất bằng chứng QA-03 và phần việc #34 đang nhận; TV3 làm TRN-01/02 theo thứ tự phụ thuộc, TV2 chủ trì QA-04 sau khi có bản tích hợp để kiểm. Mở Issue và gắn milestone không thay cho nghiệm thu Sprint 3 hoặc bằng chứng Done của Sprint 4.

## Definition of Ready

Một task chỉ vào `Ready` khi có:

- Pain point hoặc yêu cầu nguồn.
- Kết quả cụ thể cần tạo.
- Owner và reviewer.
- Tiêu chí chấp nhận có thể kiểm tra.
- Phụ thuộc và file/API dự kiến ảnh hưởng.

## Definition of Done

- Yêu cầu và thiết kế liên quan đã cập nhật.
- Code đã build, không có lỗi rõ ràng.
- Kiểm thử phù hợp đã đạt.
- Reviewer đã kiểm tra.
- Thay đổi đã merge vào nhánh tích hợp.
- Issue liên kết PR và bằng chứng demo/test.
- Nội dung báo cáo tuần đã cập nhật.

## Quy tắc nhánh và pull request

- `main`: luôn chạy được.
- `feature/<issue>-<ket-qua>`: chức năng mới.
- `fix/<issue>-<van-de>`: sửa lỗi.
- `docs/<issue>-<noi-dung>`: tài liệu hoặc quyết định.
- Không commit build output, secret hoặc CSDL thật.
- PR phải nhỏ, có tiêu chí kiểm thử và gắn Issue.
- Thay đổi CSDL/API phải được reviewer của phần frontend/backend liên quan đồng ý.
- CI `baseline-quality` và mọi job module mới phải đạt trước khi merge.
- Không commit trực tiếp lên `main`, trừ thao tác bootstrap/tích hợp được Project Owner chỉ định rõ và ghi trong biên bản.
- Squash merge PR đã đạt để lịch sử `main` giữ một thay đổi có chủ đích cho mỗi Issue.

## Mẫu báo cáo tuần

```text
Mục tiêu sprint:

Theo từng thành viên:
- Kế hoạch
- Kết quả và liên kết Issue/PR
- Tài liệu/sơ đồ/test đã tạo
- Việc chưa xong và nguyên nhân
- Số giờ thực tế

Kết quả demo:
Rủi ro/blocker:
Quyết định thay đổi phạm vi hoặc thiết kế:
Kế hoạch tuần sau:
```
