# Test case bảo vệ đồ án

| Nhóm | Test case | Kết quả |
|---|---|---|
| Auth | Username/email trùng | 409 |
| Auth | Sai password/user bị khóa | 401/403 |
| Direct | Hai request đảo thứ tự user | Một conversation |
| Message | Retry clientMessageId | Không duplicate |
| Message | Cursor pagination | Không thiếu/trùng |
| Authorization | Người ngoài đọc/gửi | 403 |
| Realtime | Receiver offline | Message vẫn persist |
| Reconnect | Realtime giao sync | UI chỉ một message |
| Receipt | Cursor cũ đến sau | Không lùi |
| Presence | Đóng một trong hai tab | Vẫn online |
| Typing | Nhiều phím nhanh | Không gửi từng phím |
| Typing | Thiếu stop | Tự hết hạn |
| Group | MEMBER quản trị | 403 |
| Group | Owner rời chưa transfer | 409 |
| Group | Thêm trùng | 409 |
| Group | Member bị xóa đọc/gửi | 403 |
| Admin | USER gọi admin API | 403 |
| Admin | Ban/unban | Có audit log |
| Privacy | Admin đọc private chat | Không có endpoint |
| Responsive | 360/768/1280 px | Không tràn ngang |
| Health | MySQL/backend/frontend | Healthy |

Automated tests nằm trong `backend/src/test` và `frontend/src/*.test.js`.
