# Hướng dẫn demo

1. Chạy Compose và chờ ba service healthy.
2. Mở hai browser profile, đăng nhập hai user.
3. Tạo direct chat hai lần để chứng minh không duplicate.
4. Gửi realtime và trình bày SENT → DELIVERED → READ, unread badge.
5. Cho receiver offline, gửi message, reconnect và kiểm tra sync không duplicate.
6. Trình bày typing throttle/timeout và presence với hai tab.
7. Tạo group, thêm member, thăng admin, system message và tên sender.
8. Xóa member; chứng minh user đó không thể đọc/gửi. Chuyển owner rồi rời nhóm.
9. Mở admin dashboard, ban user, xem audit log và thử login/reconnect thất bại.
10. Mở Swagger, health, ERD và architecture; nhấn mạnh admin không có API đọc chat riêng tư.
