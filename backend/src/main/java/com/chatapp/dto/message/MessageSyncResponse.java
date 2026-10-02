package com.chatapp.dto.message;

import java.util.List;

public record MessageSyncResponse(
        List<MessageResponse> messages,
        Long nextAfterMessageId,
        boolean hasMore
) {}
