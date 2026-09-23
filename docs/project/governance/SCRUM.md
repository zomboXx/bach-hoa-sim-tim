# Scrum rút gọn cho đồ án

Nhóm làm sprint một tuần. Product Owner và Scrum Master do trưởng nhóm kiêm nhiệm trong phạm vi môn học; giảng viên/người dùng khảo sát là nguồn phản hồi, không bị gán vai trò nội bộ của nhóm.

## Nhịp làm việc mỗi tuần

| Thời điểm | Hoạt động | Thời lượng | Đầu ra |
|---|---|---:|---|
| Đầu tuần | Sprint planning | 45–60 phút | Mục tiêu tuần, owner, reviewer, task ở `Ready` |
| Giữa tuần | Đồng bộ blocker | 15–20 phút | Quyết định kỹ thuật và việc cần hỗ trợ |
| Cuối tuần | Review + demo | 45–60 phút | Bản chạy/sơ đồ được review, kết quả test |
| Sau review | Retrospective | 15 phút | Một việc giữ lại, một việc cần sửa |
| Sau retrospective | Chốt báo cáo tuần | 20–30 phút | Báo cáo và kế hoạch tuần kế tiếp |

Mỗi ngày thành viên cập nhật Issue đang làm bằng một dòng: kết quả mới, bước tiếp theo và blocker. GitHub Project giữ trạng thái `Backlog`, `Ready`, `In Progress`, `In Review`, `Blocked`, `Done`; không nhân đôi trạng thái này trong tài liệu. Không mở cuộc họp riêng nếu Issue đã đủ thông tin.

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
