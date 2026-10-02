package com.chatapp.dto.conversation;

import java.time.LocalDateTime;

public record LastMessageResponse(
        Long id,
        Long senderId,
        String content,
        LocalDateTime createdAt
) {}
