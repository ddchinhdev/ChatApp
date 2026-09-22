# Database

Tạo database:

```sql
CREATE DATABASE chatapp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Spring Boot đang dùng:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Nên Hibernate sẽ tự tạo/cập nhật bảng khi backend chạy.

Các bảng nền tảng:
- users
- conversations
- conversation_members
- messages

Ở tuần 2, chủ yếu sử dụng bảng `users`. Các bảng chat sẽ được triển khai tiếp ở tuần 3-5.
