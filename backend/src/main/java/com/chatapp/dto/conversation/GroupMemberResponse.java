package com.chatapp.dto.conversation;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        String role,
        LocalDateTime joinedAt
) {}
