# Prototype Sim Tím Workspace

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
