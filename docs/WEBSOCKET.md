# WebSocket/STOMP protocol

## Kết nối

- Endpoint `/ws`, STOMP 1.2 over native WebSocket.
- Heartbeat hai chiều 10 giây.
- STOMP `CONNECT` header: `Authorization: Bearer <JWT>`.
- Token thiếu, hết hạn, sai chữ ký hoặc user bị khóa bị từ chối.
- JWT không được gửi trong message body.

## SEND destinations

`/app/chat.send`:

```json
{"conversationId":12,"content":"Xin chào","clientMessageId":"e91a9e94-9d02-4c44-a13e-c51a3b15f448"}
```

Server lấy sender từ Principal, kiểm tra membership, lưu và commit trước khi publish. Unique `(sender_id, client_message_id)` bảo đảm retry không tạo duplicate.

`/app/chat.delivered` và `/app/chat.read`:

```json
{"conversationId":12,"messageId":841}
```

Receipt cursor chỉ tiến về phía trước.

`/app/chat.typing`:

```json
{"conversationId":12,"typing":true}
```

Typing không lưu DB. Client throttle start, debounce stop; receiver tự hết hạn theo `expiresAt`.

## User subscriptions

- `/user/queue/messages`: message đã persist, gửi đến receiver và mọi session sender.
- `/user/queue/receipts`: trạng thái DELIVERED/READ gửi về sender.
- `/user/queue/typing`: typing của thành viên khác.
- `/user/queue/errors`: lỗi realtime có `timestamp`, `code`, `message`, `eventId`.

## Reconnect và sync

MySQL là source of truth. Sau reconnect và subscribe lại, client gọi:

```http
GET /api/sync/messages?afterMessageId=<cursor>&limit=100
```

Realtime và sync được merge theo message ID/clientMessageId. REST send là fallback khi socket chưa connected.

## Authorization

- Client không được subscribe queue của user khác.
- SEND message/receipt/typing đều kiểm tra membership.
- Group member bị xóa không thể gửi, đọc, sync hoặc phát typing.
