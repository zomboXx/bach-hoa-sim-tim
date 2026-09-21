# Sprint 1 — API trung tâm và Chapter 0

Nhánh triển khai: `codex/sprint-1-central-api-chapter-0`.

## Phạm vi giao kỹ thuật

Vue 3 + TypeScript + Vite/CSS, Spring Boot 3.5 + Spring Security + Spring Data JPA, PostgreSQL 17 + Flyway, Godot 4.6.3 + typed GDScript. Không có backend JavaScript hay game Vue thay thế Godot trong luồng hiện tại. Mã prototype cũ vẫn giữ để đối chiếu.

Sprint 1 trong backlog là nền tảng đăng nhập, dữ liệu trung tâm, danh mục và kiểm thử. Theo yêu cầu cập nhật, bản này nối thêm các thao tác vận hành cơ bản và đưa Chapter 0 lên sớm; không đánh dấu toàn bộ Sprint 2–4 đã hoàn thành.

| Vai trò | Được thực hiện |
|---|---|
| `NV001` · sales | POS: tên/mã vạch + Enter, giỏ, tính tiền, hóa đơn của mình, đào tạo |
| `KHO001` · stock | Nhận hàng, tồn/lô và biến động hỗ trợ nghiệp vụ kho, đào tạo; xuất/điều chuyển chưa mở |
| `QL001` · manager | Danh mục/nhà cung cấp, tồn, kiểm kê và duyệt chênh lệch, khuyến mãi, báo cáo, đào tạo |
| `KT001` · accountant | Xem doanh số và tra cứu hóa đơn; chưa có ghi sổ thu chi/công nợ/đối soát |
| `ADMIN001` · admin | Toàn quyền API hiện có; cấu hình/chuỗi chưa triển khai |

Chi tiết các chức năng còn thiếu và chốt quyền: [ROLES_AND_CHAPTER_0.md](../product/ROLES_AND_CHAPTER_0.md).

Mật khẩu của dữ liệu mẫu: `demo123`, lưu bằng BCrypt. Quyền được kiểm tra ở API trước validation và ở service controller, không chỉ ẩn nút. Phiên đăng nhập dùng cookie HttpOnly/SameSite và CSRF; ID nhân viên trong trình duyệt không phải thông tin xác thực.

## Chạy từ checkout sạch

Yêu cầu: Node 22+, JDK 21–25, Maven 3.9+, PostgreSQL 17 (hoặc Docker), Godot **4.6.3** kèm export templates cùng phiên bản. Cài templates qua Editor → Manage Export Templates hoặc gói chính thức. Web dùng Compatibility và single-thread, cần WebGL 2.

1. Tại thư mục gốc: `docker compose up -d`. PostgreSQL chỉ bind localhost:5433, volume `simtim_pg` giữ dữ liệu qua restart. Không dùng `down -v` nếu muốn giữ dữ liệu.
2. `mvn -f services/api/pom.xml spring-boot:run`. Flyway tự tạo schema; seed 5 tài khoản mẫu, bổ sung tài khoản còn thiếu nhưng không ghi đè mật khẩu đã có. Hàng mẫu chỉ seed lần đầu. API mặc định localhost:8080. `GET http://127.0.0.1:8080/api/health` phải trả `UP`.
3. `pwsh -File scripts/export-training.ps1 -Godot "đường-dẫn-godot-console.exe"`. Linux: `godot --headless --path apps/training-godot --editor --import` rồi `godot --headless --path apps/training-godot --export-release Web`.
4. `cd apps/web`, `npm ci`, `npm run dev`, mở `http://127.0.0.1:5174`.
5. Bản build: `npm run build`, `npm run preview`, mở `http://127.0.0.1:4174`. Export Godot **trước** khi build PWA. File Web sinh ra được ignore; source Godot/map có trong Git.

Vite proxy `/api` → localhost:8080 cho cả dev và preview. Khi triển khai tập trung, host PWA và Godot cùng origin; reverse proxy `/api` tới Spring Boot, dùng HTTPS. Preview Vite là phục vụ kiểm thử cục bộ, không phải máy chủ production.

Biến môi trường backend: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `BIND_ADDRESS`, `COOKIE_SECURE`, `SEED_DEMO`, `DEMO_PASSWORD`. Khi không dùng môi trường mẫu: cung cấp secret riêng, không bật seed mặc định, bật secure cookie sau HTTPS. Thay `DEMO_PASSWORD` không tự đổi mật khẩu tài khoản đã seed; không ghi đè tài khoản có sẵn.

Frontend mặc định API; có thể đặt `VITE_DATA_MODE=demo` trước dev/build để mở sandbox IndexedDB độc lập. **Không tự chuyển sang demo khi API lỗi.** Demo chỉ phục vụ so sánh UI, không xác thực thật.

### Môi trường cục bộ đã chuẩn bị ở máy hiện tại

Docker engine không lên được, nên đã chạy PostgreSQL 17.11 từ binary chính thức trong môi trường riêng `D:/ducph/simtim-sprint1-runtime` (bind 127.0.0.1:5433), không sửa dịch vụ PostgreSQL khác. `data` là CSDL vận hành mẫu; **không xóa** nếu cần giữ dữ liệu. Binary `pgsql` là junction tới công cụ tải trong `.codex_tmp/tools/pgsql`; nếu dọn `.codex_tmp`, giữ binary hoặc cài PostgreSQL riêng trước.

Khởi động lại DB bằng `D:/ducph/simtim-sprint1-runtime/pgsql/bin/pg_ctl.exe -D D:/ducph/simtim-sprint1-runtime/data -l D:/ducph/simtim-sprint1-runtime/postgres.log -o "-h 127.0.0.1 -p 5433" -w start`. Dừng bằng cùng `pg_ctl -D ... -m fast -w stop`; không xóa data. Tài khoản DB mẫu `simtim`, mật khẩu `simtim-local-only` — chỉ localhost, không dùng cho triển khai thật.

JDK hiện tại: `C:/Program Files/Java/jdk-25`; Maven: `D:/ducph/Downloads/apache-maven-3.9.11/bin/mvn.cmd`. Dùng `scripts/start-api.ps1` với `-JavaHome` và `-Maven` nếu PATH/JAVA_HOME chưa đúng.

## Luồng demo để nghiệm thu

1. Đăng nhập QL001: thêm danh mục → sản phẩm với barcode mới → nhà cung cấp. Sửa và thử barcode trùng để thấy lỗi server.
2. KHO001: chọn sản phẩm/nhà cung cấp, mã lô mới, hạn tương lai, số giao/nhận. Nhận ít hơn giao phải có lý do. Xem lô và biến động nhận hàng.
3. NV001: tra tên/mã vạch, thêm giỏ, chọn số lượng, nhập tiền khách đưa rồi xác nhận. Server tính giá/khuyến mãi, xuất FEFO chỉ lô còn hạn. Xem hóa đơn; reload/đăng nhập lại vẫn còn dữ liệu. Chuyển khoản chỉ **ghi nhận phương thức**, không xử lý tiền thật.
4. Bấm ảnh mentor có `?` → màn 7 chương. Chapter 0 → vào Godot. Đi bằng WASD/mũi tên hoặc nút cảm ứng; đến gần 6 điểm sáng, E để đọc, E/tiếp tục để xác nhận. Sau đó trả lời 3 câu làm quen cùng mentor; sai được giải thích/thử lại. Đủ cả hai điều kiện mới hoàn thành; PWA xác nhận kết quả đã lưu API. Chapter 1–6 hiện đúng “Nội dung này đang được cập nhật, vui lòng thử lại sau”.
5. QL001 kiểm kê: đang trong phiên, tắt mạng/thử mất mạng → ghi phiếu → nối lại. Phiếu gửi với mã idempotency và version; nếu tồn đã đổi thì CONFLICT, không tự ghi đè. Quản lý chỉ duyệt REVIEW; phát sinh movement nguyên tử. Hàng đợi IndexedDB tách theo người tạo, tồn/hóa đơn không cache offline. Warehouse đã bỏ quyền kiểm kê theo ma trận 5 role mới; queue cũ giữ nguyên nhưng không tự gửi khi thiếu quyền.

**Giới hạn offline có chủ ý:** chỉ lưu kiểm kê trong phiên đã mở; queue giữ qua reload nhưng tải lại app khi mất mạng không cho đăng nhập mới/khôi phục quyền bằng dữ liệu client. Nối mạng và xác thực lại để gửi queue. Không hỗ trợ bán hàng offline hoặc chơi/lưu đào tạo offline trong API mode.

## Kiểm thử và đối chiếu backlog

Tạo DB test riêng: `createdb ... simtim_test` (không dùng DB vận hành). `mvn -f services/api/pom.xml test`; có thể đặt `TEST_DB_URL` và DB credentials qua môi trường. Test dùng PostgreSQL thật, migration thật, Spring Security/MockMvc, rollback sau mỗi test chức năng.

`npm run build` rồi `npm run test:e2e` trong `apps/web`; API và Godot export phải sẵn sàng. `E2E_URL` cho phép trỏ dev server khi kiểm thử; mặc định preview:4174. Các test legacy `workflows.spec.ts` thuộc prototype SVG cũ, không phải suite nghiệm thu bản Godot hiện tại.

`godot --headless --path apps/training-godot --script res://tests/tour_test.gd` thực sự di chuyển nhân vật qua bản đồ có collision tới đủ 6 điểm; không teleport, không thêm API debug vào game Web.

| Backlog | Hiện thực / bằng chứng | Cần nhóm xác nhận |
|---|---|---|
| REQ-01 | Phạm vi cập nhật ở tài liệu này; bản đồ/chương theo yêu cầu người dùng | Biên bản và quyết định của nhóm, các mục cần khảo sát trong SRS |
| DB-01 | `docs/architecture/DATA_MODEL.md`, Flyway V1/V2/V3 | Review ERD/data dictionary |
| BE-01 | `services/api`, `infra/compose.yaml`; health và migration trên PostgreSQL thật | Kiểm tra máy các thành viên |
| BE-02 | SecurityConfig, BCrypt, session/CSRF, test login và quyền | Review bảo mật |
| BE-03 | CRUD có soft deactivate; barcode unique/search, categories/suppliers | Review nghiệp vụ ngừng sử dụng |
| FE-01 | `server.ts`, `VITE_DATA_MODE`, lỗi API hiển thị không fallback | Review UI trên thiết bị mục tiêu |
| QA-01 | `SprintOneTest.java`, `sprint1.spec.ts`, Godot tour test | Chạy CI và review/merge theo DoD |

Các mục ở trạng thái **đã triển khai, chờ review nhóm**, không tự tuyên bố Sprint đã đóng hoặc phân công/stack đã được tất cả thành viên ký duyệt. Chapter 0 là làm quen, không thay cho chapter nghiệp vụ có đánh giá đúng/sai cuối kỳ.

Nguồn tương thích runtime: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Godot Web export](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_web.html), [PostgreSQL Windows binaries](https://www.postgresql.org/download/windows/).
