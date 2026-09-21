# Frontend Sprint 1

Vue 3 PWA mặc định gọi Spring Boot qua `/api`. Chế độ `VITE_DATA_MODE=demo` chỉ là sandbox IndexedDB độc lập và không được dùng làm fallback khi API lỗi.

## Chạy

Yêu cầu Node.js 22.12 trở lên hoặc Node 20.19+.

```powershell
cd prototype-v2
npm ci
npm run dev
```

Mở `http://127.0.0.1:5174`. API mặc định chạy ở `http://127.0.0.1:8080`; xem `../docs/SPRINT_1.md` để khởi động PostgreSQL, backend và Godot Web export.

## Kiểm thử

```powershell
npm run verify
```

Suite Sprint 1 yêu cầu API và Godot export đã sẵn sàng. Playwright dùng production preview tại `http://127.0.0.1:4174`.

## Cấu trúc hiện tại

- `src/App.vue`: shell và điều phối màn hình.
- `src/api.ts`: kiểu dữ liệu và lựa chọn adapter.
- `src/server.ts`: adapter REST.
- `src/InvoiceEditor.vue`: luồng lập hóa đơn.
- `src/RoleWorkspace.vue`: workspace theo vai trò.
- `src/training/ChapterLobby.vue`: lobby tích hợp Godot.
- `tests/sprint1.spec.ts`: E2E nghiệm thu Sprint 1.

Các file prototype Vue/SVG cũ còn được giữ để đối chiếu cho đến khi đợt foundation hardening hoàn tất.
