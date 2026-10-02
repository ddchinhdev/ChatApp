package com.chatapp.dto.user;

public record UserSearchResponse(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl
) {}
