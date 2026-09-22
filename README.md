# ChatApp

Ứng dụng trò chuyện thời gian thực - giai đoạn hoàn thành đến hết tuần 2.

## Phạm vi đã hoàn thành

### Tuần 1
- Khởi tạo Spring Boot backend
- React frontend
- MySQL configuration
- Database entities: User, Conversation, ConversationMember, Message
- REST API architecture
- Git-friendly project structure

### Tuần 2
- Register
- Login
- BCrypt password hashing
- JWT authentication
- Spring Security
- Get current user
- Update profile
- React Login/Register pages
- Lưu JWT ở frontend và gọi API có Authorization Bearer token

> WebSocket chưa triển khai trong tuần 2. Sẽ bắt đầu ở tuần 4.

## Công nghệ

Backend:
- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT (JJWT)
- MySQL
- Maven

Frontend:
- React
- Vite
- JavaScript
- React Router

## Chạy Backend

1. Tạo database:
```sql
CREATE DATABASE chatapp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Kiểm tra `backend/src/main/resources/application.properties` và sửa username/password MySQL nếu cần.

3. Chạy:
```bash
cd backend
mvn spring-boot:run
```

Backend chạy tại:
`http://localhost:8080`

## Chạy Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chạy tại:
`http://localhost:5173`

## API tuần 2

POST `/api/auth/register`

```json
{
  "username": "chinh",
  "email": "chinh@example.com",
  "password": "123456"
}
```

POST `/api/auth/login`

```json
{
  "username": "chinh",
  "password": "123456"
}
```

GET `/api/users/me`

Header:
`Authorization: Bearer <JWT>`

PUT `/api/users/me`

```json
{
  "displayName": "Dương Đăng Chinh"
}
```

## Lưu ý

- Đây là baseline để tiếp tục tuần 3.
- Conversation/Message đã có entity và database model nhưng API chat chưa triển khai.
- WebSocket/STOMP sẽ được triển khai sau khi phần authentication và user hoàn chỉnh.
