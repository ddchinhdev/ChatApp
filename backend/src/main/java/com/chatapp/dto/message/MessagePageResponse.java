package com.chatapp.dto.message;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> messages,
        String nextCursor,
        boolean hasMore
) {}
