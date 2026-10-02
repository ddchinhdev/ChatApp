package com.chatapp.dto.message;

import java.time.Instant;

public record TypingEventResponse(
        Long conversationId,
        Long userId,
        String displayName,
        boolean typing,
        Instant expiresAt
) {}
