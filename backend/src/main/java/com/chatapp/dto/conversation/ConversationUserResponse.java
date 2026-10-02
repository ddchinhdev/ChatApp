package com.chatapp.dto.conversation;

import java.time.Instant;

public record ConversationUserResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        boolean online,
        Instant lastSeenAt
) {}
