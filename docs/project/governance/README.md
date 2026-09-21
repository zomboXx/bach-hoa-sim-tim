# Quản lý dự án cuối kỳ

Đây là nguồn thống nhất cho kế hoạch nhóm. GitHub Issues dùng để theo dõi từng việc; tài liệu trong thư mục này giữ quy tắc, phân công và phạm vi sprint.

- [Phân công](TEAM.md)
- [Quy trình Scrum rút gọn](SCRUM.md)
- [Product backlog và sprint](BACKLOG.md)
- [Điều kiện merge đầu tiên](MERGE_01.md)

## Nguyên tắc

1. Mỗi backlog item có một owner, một reviewer và tiêu chí chấp nhận.
2. Owner chịu trách nhiệm xuyên suốt: yêu cầu, thiết kế, code, test và bằng chứng báo cáo.
3. Không đưa việc vào `Done` khi mới hoàn thành code cục bộ.
4. Mỗi tuần phải có bản chạy hoặc tài liệu có thể review, không chỉ báo cáo bằng lời.
5. `main` phải build và chạy kiểm thử; thay đổi đi qua pull request.

## Trạng thái

```text
Backlog → Ready → In progress → Review → Testing → Done
                         └──────→ Blocked
```

