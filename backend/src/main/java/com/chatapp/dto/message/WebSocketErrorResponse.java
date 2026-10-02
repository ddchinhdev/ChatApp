package com.chatapp.dto.message;

import java.time.OffsetDateTime;

public record WebSocketErrorResponse(
        OffsetDateTime timestamp,
        String code,
        String message,
        String eventId
) {}
