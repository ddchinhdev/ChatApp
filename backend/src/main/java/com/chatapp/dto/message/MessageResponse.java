package com.chatapp.dto.message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderUsername,
        String senderDisplayName,
        String content,
        String clientMessageId,
        String receiptStatus,
        boolean system,
        LocalDateTime createdAt
) {}
