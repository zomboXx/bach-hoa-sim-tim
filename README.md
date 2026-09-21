# Sim Tím Workspace — Sprint 1

Bản hiện tại gồm **Vue PWA + Spring Boot API + PostgreSQL/Flyway + Godot Chapter 0**.
Đọc [hướng dẫn chạy, kiểm thử và phạm vi Sprint 1](docs/SPRINT_1.md) và
[mô hình dữ liệu / REST contract](docs/DATA_MODEL.md) trước khi chạy.

```powershell
docker compose up -d
mvn -f backend/pom.xml spring-boot:run
# Terminal khác, sau khi cài Godot 4.6.3 và Web export templates:
pwsh -File scripts/export-training.ps1 -Godot "duong-dan-godot-console.exe"
cd prototype-v2
npm ci
npm run dev
```

Mở `http://127.0.0.1:5174`; NV001 / demo123 là nhân viên bán hàng mẫu.
Mở ảnh mentor `?` → Chapter 0 để chơi; Chapter 1–6 hiện thông báo đang cập nhật.
Đã tách 5 role; xem [ma trận quyền và phạm vi hiện thực](docs/ROLES_AND_CHAPTER_0.md). Chapter 0 có nhân vật 4 hướng, 6 điểm khám phá và 3 câu xác nhận.
Không có thanh toán thật, SaaS đa tenant hoặc toàn bộ chapter nghiệp vụ.

## Lịch sử prototype (không phải hướng dẫn cho bản API hiện tại)

Quy trình nhóm, phân công, Scrum và backlog được quản lý tại
[docs/project-management](docs/project-management/README.md). Điều kiện để tạo
baseline qua lần merge đầu tiên nằm trong
[MERGE_01.md](docs/project-management/MERGE_01.md).

## Bản mới theo kiến trúc đã thống nhất

Prototype 02 nằm tại [prototype-v2](prototype-v2/README.md), dùng Vue 3 + TypeScript + Vite để đánh giá hướng frontend đề xuất. Đây chưa phải quyết định khóa stack của nhóm.

```powershell
cd prototype-v2
npm ci
npm run build
npm run preview
```

Mở `http://127.0.0.1:4174`. Xem [phạm vi và API contract](prototype-v2/ARCHITECTURE.md).

Bản cũ bên dưới được giữ tại commit `46ce071` và vẫn chạy độc lập bằng các file ở thư mục gốc.

## Bản đầu tiên

Prototype giao diện quản lý cửa hàng dành cho **nhân viên bán hàng**, kết hợp khu đào tạo nghiệp vụ 2D.

## Chạy prototype

Không cần cài dependency. Tại thư mục dự án, chạy:

```powershell
npx serve .
```

Sau đó mở địa chỉ do terminal cung cấp. Có thể mở `index.html` trực tiếp, nhưng chạy qua local server sẽ ổn định hơn.

## Tài khoản demo

- Mã nhân viên: `NV001`
- Mật khẩu: `demo123`

## Luồng nên thử

1. Đăng nhập bằng tài khoản demo.
2. Xem dashboard ca làm việc của nhân viên bán hàng.
3. Nhấn avatar **Mentor Mai** có dấu `?` ở góc phải.
4. Xem 7 chapter trên bản đồ đào tạo.
5. Nhấn bất kỳ nút chapter nào để xem thông báo nội dung đang cập nhật.

Đây là prototype giao diện chạy hoàn toàn ở phía trình duyệt, chưa có backend hay dữ liệu thật.
