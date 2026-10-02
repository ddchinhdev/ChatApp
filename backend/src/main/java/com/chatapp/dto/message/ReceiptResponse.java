package com.chatapp.dto.message;

public record ReceiptResponse(
        Long conversationId,
        Long messageId,
        String status
) {}
