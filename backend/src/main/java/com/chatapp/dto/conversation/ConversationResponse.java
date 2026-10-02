package com.chatapp.dto.conversation;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String type,
        ConversationUserResponse otherUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastActivityAt,
        LastMessageResponse lastMessage,
        long unreadCount,
        String name,
        String avatarUrl,
        String currentUserRole,
        int memberCount
) {}
