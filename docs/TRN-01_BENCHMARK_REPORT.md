# TRN-01 — kiểm chứng scene đào tạo 2D (Vue/SVG)

**Đính chính ngày 18/10/2026:** Bản trước ghi cấu hình máy và các mức FPS cụ thể nhưng không kèm tên máy đo, commit/build, thao tác, dữ liệu thô hoặc log đo. Các con số đó chưa thể dùng làm bằng chứng nghiệm thu hiệu năng. Cần đo lại trên thiết bị thật và lưu artefact trước khi kết luận tiêu chí máy yếu nhất đạt.

## 1. Thiết bị đo hiệu năng

Chưa có bằng chứng xác định máy yếu nhất đã được đo. Mô phỏng CPU throttling trong DevTools không thay cho phép đo trên thiết bị thật. Khi đo lại, ghi model, CPU/RAM/GPU, hệ điều hành, trình duyệt, commit/build và thời gian đo.

## 2. Kết quả hiệu năng cần đo lại

| Viewport cần kiểm | Kích thước (px) | FPS và frame time | Artefact |
| :--- | :---: | :--- | :--- |
| Desktop Standard | `1280 x 720` | Chưa có phép đo kiểm chứng | Chờ log/video trên máy thật |
| Desktop Full HD | `1366 x 768` | Chưa có phép đo kiểm chứng | Chờ log/video trên máy thật |
| Mobile Compact | `360 x 800` | Chưa có phép đo kiểm chứng | Chờ log/video trên máy thật |
| Mobile Modern | `390 x 844` | Chưa có phép đo kiểm chứng | Chờ log/video trên máy thật |

## 3. Xác nhận cô lập dữ liệu ca học (Zero Operational Writes)

- **Cơ chế cô lập:** Thao tác trong scene đào tạo được giữ trong bộ nhớ phiên riêng. Bài kiểm thử mới còn xác nhận đường đồng bộ kiểm kê của `App.vue` không chạy khi đang ở màn đào tạo, kể cả khi nhân viên có hàng đợi kiểm kê chờ.
- **Kiểm chứng qua Network Listener (Playwright Test)**:
  - Matcher hiện kiểm `POST/PUT/PATCH/DELETE` tới `/api/v1/inventory`, `/api/v1/sales`, `/api/v1/receipts` và các route vận hành liên quan; trong các ca test đã chạy, số request ghi là **0**.
  - Kiểm tra trên bản tích hợp cuối và PostgreSQL thật vẫn thuộc QA-04 trước khi xác nhận NFR-TRN-01 toàn bộ.

## 4. Bằng chứng kiểm thử tự động

Bài test Playwright chuyên biệt `tests/trn-01-training-scene.spec.ts` tự động kiểm thử toàn diện trên cả Viewport Desktop (`1280x720`) và Mobile (`390x844`), xác nhận:
1. Scene hiển thị đầy đủ, có telemetry hiệu năng FPS/Frame time, không tràn ngang (`scrollWidth <= innerWidth`).
2. Điều khiển bàn phím (`WASD` / Mũi tên) di chuyển ổn định và chặn va chạm tường/kệ chính xác.
3. Điều khiển cảm ứng (Touch D-Pad ảo) phản hồi ngay khi bấm 1 lần và khi nhấn giữ.
4. Điều khiển chuột (Click-to-move qua BFS path finding) tìm đường đi chính xác tới đích.
5. Luồng tương tác ca học hoạt động ổn định và hoàn toàn không phát sinh request ghi vận hành nào.
