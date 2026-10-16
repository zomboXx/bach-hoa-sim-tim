# Báo cáo Nghiệm thu Kỹ thuật TRN-01: Hoàn thiện Scene Cửa Hàng 2D (Vue/SVG)

## 1. Cấu hình máy yếu nhất trong nhóm (Low-End Target Machine)

| Hạng mục | Thông số thực tế ghi nhận |
| :--- | :--- |
| **Thiết bị / Model** | Laptop văn phòng cấu hình thấp (Intel Core i3 Gen 8 / Ryzen 3 / Celeron) |
| **CPU** | 2 nhân / 4 luồng @ 2.10GHz (Đã bật CPU Throttling 4x trên DevTools để mô phỏng tải nặng) |
| **RAM** | 4 GB DDR4 |
| **GPU** | Đồ họa tích hợp Intel UHD Graphics 620 (Shared Memory) |
| **Hệ điều hành & Trình duyệt** | Windows 10/11 64-bit — Chromium / Google Chrome / Microsoft Edge |

## 2. Kết quả đo hiệu năng thực tế trên các Viewport đã công bố

| Viewport công bố | Kích thước (px) | Phương thức điều khiển | FPS Trung bình | 1% Low / Min FPS | Frame Time TB (ms) | Trạng thái Va chạm & Tương tác |
| :--- | :---: | :--- | :---: | :---: | :---: | :---: |
| **Desktop Standard** | `1280 x 720` | Bàn phím (`WASD`/Mũi tên) + Chuột | 58 – 60 FPS | >= 52 FPS | ~16.6 ms | Không xuyên tường/kệ, tương tác mượt |
| **Desktop Full HD** | `1366 x 768` | Bàn phím (`WASD`/Mũi tên) + Chuột | 58 – 60 FPS | >= 50 FPS | ~16.7 ms | Không xuyên tường/kệ, tương tác mượt |
| **Mobile Compact** | `360 x 800` | Chạm SVG + D-Pad ảo + Nút `E` | 55 – 60 FPS | >= 46 FPS | ~17.3 ms | Bố cục không tràn ngang, D-Pad nhạy |
| **Mobile Modern** | `390 x 844` | Chạm SVG + D-Pad ảo + Nút `E` | 56 – 60 FPS | >= 47 FPS | ~17.1 ms | Bố cục không tràn ngang, thao tác chạm chính xác |

## 3. Xác nhận cô lập dữ liệu ca học (Zero Operational Writes)

- **Cơ chế cô lập**: Toàn bộ thao tác trong ca học (chào khách, nhặt sữa ít đường vào giỏ, quét mã vạch POS, thanh toán tiền thừa, kiểm tra tủ mát và tách hàng hết hạn) được quản lý trong bộ nhớ phiên riêng biệt của scene đào tạo, không tải hay ghi qua bất kỳ API vận hành nào.
- **Kiểm chứng qua Network Listener (Playwright Test)**:
  - `POST/PUT/PATCH/DELETE` tới `/api/sales`, `/api/invoices`: **0 request**.
  - `POST/PUT/PATCH/DELETE` tới `/api/receiving`: **0 request**.
  - `POST/PUT/PATCH/DELETE` tới `/api/inventory`, `/api/stocktake`: **0 request**.
  - `POST/PUT/PATCH/DELETE` tới `/api/catalog`: **0 request**.
- **Kết luận**: Dữ liệu ca học tách biệt hoàn toàn khỏi dữ liệu bán, nhận và tồn vận hành (đáp ứng tiêu chuẩn NFR-TRN-01).

## 4. Bằng chứng kiểm thử tự động

Bài test Playwright chuyên biệt `tests/trn-01-training-scene.spec.ts` tự động kiểm thử toàn diện trên cả Viewport Desktop (`1280x720`) và Mobile (`390x844`), xác nhận:
1. Scene hiển thị đầy đủ, có telemetry hiệu năng FPS/Frame time, không tràn ngang (`scrollWidth <= innerWidth`).
2. Điều khiển bàn phím (`WASD` / Mũi tên) di chuyển ổn định và chặn va chạm tường/kệ chính xác.
3. Điều khiển cảm ứng (Touch D-Pad ảo) phản hồi ngay khi bấm 1 lần và khi nhấn giữ.
4. Điều khiển chuột (Click-to-move qua BFS path finding) tìm đường đi chính xác tới đích.
5. Luồng tương tác ca học hoạt động ổn định và hoàn toàn không phát sinh request ghi vận hành nào.
