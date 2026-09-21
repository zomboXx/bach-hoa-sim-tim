# Asset provenance

Map và manifest do người dùng cung cấp tại `D:/ducph/Downloads/store_interactable_crops_v1/store_interactable_crops_v1/`.

- `assets/store_map.png`: bản sao nguyên bản `store_map_original.png` (1986×792).
- `assets/crop_manifest.csv`: dữ liệu tọa độ gốc để truy nguyên.
- `assets/layout.txt`: bản sao CSV dưới phần mở rộng không bị Godot tự import thành Translation; dùng dựng collision chân quầy/kệ. Kích thước crop gồm cả phần đỉnh theo phối cảnh nên không dùng nguyên hình chữ nhật làm collision.
- `apps/web/public/training-map.png`: cùng map dùng làm preview chương, không chỉnh sửa ảnh nguồn.
- `assets/employee-walk-v2.png`, `assets/mentor-walk-v2.png`: sprite PNG có alpha thật, dùng bởi `player.gd` (4 hướng × 6 frame). Imagegen chỉnh sửa từ hai sheet người dùng cung cấp, không phải bộ nhân vật vẽ bằng code cũ. Atlas chọn vùng và bỏ padding trong bộ nhớ, không ghi đè ảnh nguồn. Các người trong ảnh map vẫn là nền tĩnh.
- `art-source/employee-original.png`, `art-source/mentor-original.png`: giữ bản nguồn; không đóng gói vào bản Web export.

## Nhật ký tạo sprite — 16/09/2026

Skill: imagegen; mode: edit / background-extraction; 2 lần gọi builtin imagegen riêng, mỗi lần tham chiếu đúng sheet gốc. Kết quả AI là biến thể có thể khác vài pixel so với nguồn, không phải tách nền lossless. Đã kiểm tra alpha và hiển thị trong Godot Web.

Prompt chung đã gửi (nguyên văn):

```text
Use case: background-extraction. Asset type: Godot 2D four-direction walking sprite sheet. Input image is the EDIT TARGET. Remove the background completely to genuinely transparent alpha (not a painted checkerboard). Preserve the exact character identity, outfit, green vest with purple trim, pixel-art look and all 24 existing poses. Normalize into exactly 6 equal columns by 4 equal rows on a 1536x1024 canvas; each 256x256 cell contains one complete character centered horizontally at cell x=128, feet on y=238, consistent height about 218px. Row 1 front/down, row 2 left, row 3 right, row 4 back/up. Keep individual gait variations from the source. No new poses, props, text, grid lines, floor, glow, shadow or colored background. Do not clip shoes or hair. Transparent margins around every character.
```

Hậu tố employee: `Subject: black-haired male employee. Keep dark hair, white short-sleeve shirt, black trousers and shoes.`

Hậu tố mentor: `Subject: female mentor with orange bob hair and black glasses. Keep the orange hair, glasses, white short-sleeve shirt and dark trousers.`

Output không có ô đều tuyệt đối như prompt; `player.gd` khai báo cửa sổ atlas riêng cho từng sheet. Cả 24 frame được nạp; chu kỳ 6 frame cho mỗi hướng chạy ở 8 fps khi đang đi. Mentor đứng yên ở hướng trước trong Chapter 0.

Quyền sở hữu/giấy phép thương mại của bộ map chưa được cung cấp. Chỉ dùng trong project mẫu theo yêu cầu; nhóm cần xác nhận nguồn và quyền trước khi phân phối công khai. Không suy đoán rằng tài sản người dùng đưa là public domain.
