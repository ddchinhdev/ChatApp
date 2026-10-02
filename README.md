# ChatApp MVP

Ứng dụng chat realtime gồm Spring Boot, React, MySQL và WebSocket/STOMP. MVP hỗ trợ chat 1-1, group chat, lịch sử phân trang, đồng bộ tin nhắn bỏ lỡ, receipt, presence, typing và trang quản trị.

## Chức năng

- Đăng ký, đăng nhập JWT, cập nhật hồ sơ và tìm kiếm người dùng.
- Direct conversation duy nhất cho mỗi cặp user.
- Group chat với OWNER/ADMIN/MEMBER, chuyển owner và system message.
- REST fallback và realtime message qua STOMP; retry an toàn bằng `clientMessageId`.
- Cursor pagination, missed-message sync, unread count, SENT/DELIVERED/READ.
- Presence nhiều tab, last seen UTC, typing có throttle và timeout.
- Admin dashboard: thống kê, tìm kiếm user, khóa/mở khóa, audit log.
- Admin không có API mặc định để đọc nội dung chat riêng tư.

## Công nghệ

- Backend: Java 17, Spring Boot 3.5, Spring Security, JPA, Flyway, MySQL, STOMP.
- Frontend: React 19, Vite, React Router, Axios, STOMP.js.
- Đóng gói: Docker multi-stage, Nginx, Docker Compose.

## Chạy nhanh bằng Docker

```bash
copy .env.example .env
```

Thay toàn bộ giá trị `replace_with_...` trong `.env`; JWT secret phải là chuỗi ngẫu nhiên tối thiểu 48 ký tự. Sau đó:

```bash
docker compose up --build -d
docker compose ps
```

- Ứng dụng: http://localhost:8088
- Backend health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Dừng bằng `docker compose down`; thêm `-v` nếu muốn xóa volume MySQL local.

## Chạy development

Backend cần `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; `CORS_ALLOWED_ORIGINS` mặc định là `http://localhost:5173`.

```bash
cd backend
mvn spring-boot:run

cd ../frontend
npm ci
npm run dev
```

Frontend có thể đặt `VITE_API_URL=http://localhost:8080/api`.

## Seed demo tùy chọn

Seed mặc định tắt. Chỉ bật trong môi trường demo:

```dotenv
DEMO_SEED_ENABLED=true
DEMO_USER_PASSWORD=<mật khẩu tối thiểu 8 ký tự>
DEMO_ADMIN_PASSWORD=<mật khẩu tối thiểu 8 ký tự>
```

Seeder tạo user `demo` và `admin` nếu chưa tồn tại. Không commit `.env` hoặc mật khẩu thật.

## Test và build

```bash
cd backend
mvn test
mvn package -DskipTests

cd ../frontend
npm test
npm run build
```

## Tài liệu

- [WebSocket/STOMP](docs/WEBSOCKET.md)
- [ERD](docs/ERD.md)
- [Kiến trúc](docs/ARCHITECTURE.md)
- [Hướng dẫn demo](docs/DEMO_GUIDE.md)
- [Test case bảo vệ](docs/TEST_CASES.md)

## Bảo mật vận hành

- JWT chỉ đi trong HTTP Authorization hoặc STOMP CONNECT header, không nằm trong message body/log.
- REST/WebSocket action kiểm tra membership hoặc role tại server.
- User bị khóa không thể login, dùng REST token cũ hoặc reconnect WebSocket.
- Thời gian lưu/trao đổi theo UTC; frontend định dạng theo locale.
- Production phải dùng HTTPS/WSS, secret manager, rate limiting và backup MySQL.
