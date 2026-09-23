# Runbook thiết lập GitHub cho nhóm

Tài liệu này mô tả thao tác quản trị ngoài Git. Các checkbox chỉ được đánh dấu khi Project Owner đã xác nhận trực tiếp trên GitHub; file trong repository không thể tự bật ruleset, cấp quyền hoặc tạo Project.

## 1. Thành viên và merge settings

- [ ] Xác nhận bốn tài khoản trong `TEAM.md` và thêm ba thành viên còn lại làm collaborator; thành viên phát triển dùng quyền `Write`, không cấp `Admin` nếu không cần.
- [ ] Trong `Settings → General → Pull Requests`, chỉ bật `Allow squash merging`.
- [ ] Bật tự động xóa head branch sau merge.

## 2. Ruleset `protect-main`

Tạo branch ruleset, giữ `Disabled` trong lúc cấu hình rồi chuyển `Active` sau khi kiểm tra đầy đủ:

- Target: `Include default branch` (`main`), không áp dụng cho mọi feature branch.
- Không thêm bypass mặc định.
- Bật `Restrict deletions`, `Require linear history`, `Require a pull request before merging` và `Block force pushes`.
- Required approvals: `1`.
- Bật `Require approval of the most recent reviewable push` và `Require conversation resolution before merging`.
- Allowed merge methods: chỉ `Squash`.
- Chưa bật Code Owners cho tới khi mỗi pattern có ít nhất hai reviewer hợp lệ và cấu hình đã được thử bằng PR.
- Chưa bật deployment, signed commits, code scanning, code quality hoặc coverage nếu pipeline tương ứng chưa tồn tại.

Sau khi có một PR chạy CI thành công, bật `Require status checks to pass` và thêm chính xác:

- `Repository policy`
- `Web baseline`

Sau khi hai check hoạt động ổn định, có thể bật `Require branches to be up to date before merging`.

## 3. Project và milestone

Tạo GitHub Project `Bách Hóa Sim Tím` với một board:

```text
Backlog → Ready → In Progress → In Review → Done
                            └──→ Blocked
```

Dùng `Status` và `Assignees` có sẵn. Trường tùy chỉnh tối thiểu chỉ gồm `Sprint`, `Priority`, `Effort`; reviewer được ghi trong Issue và gán khi mở PR. Tạo milestone `Sprint 1` và đưa mỗi backlog item đã chọn vào một Issue; Markdown backlog chỉ giữ phạm vi và dependency.

Nhãn tối thiểu, không dùng label để lặp lại Project status:

- `type:feature`, `type:bug`, `type:decision`
- `area:web`, `area:api`, `area:data`, `area:docs`
- `priority:P0`, `priority:P1`, `priority:P2`

## 4. PR thử nghiệm đầu tiên

1. Chọn một thay đổi governance/docs nhỏ có Issue và reviewer.
2. Tạo branch từ `main`, mở Draft PR rồi chuyển Ready for review.
3. Xác nhận CODEOWNERS yêu cầu đúng người và CI sinh hai check mong đợi.
4. Reviewer approve, giải quyết toàn bộ conversation, squash merge và xác nhận branch được xóa.
5. Chỉ sau lần thử này mới bật Code Owners và required status checks trong ruleset.

## 5. Quy tắc vận hành

- Mỗi Issue có một owner chính và một reviewer; một người tối đa một Issue `In Progress`.
- Mỗi branch gắn một Issue và bị xóa sau merge.
- `Done` chỉ khi PR đã vào `main`, CI đạt và acceptance criteria có bằng chứng.
- Tag chỉ dùng cho baseline/release, không dùng thay trạng thái Issue.
