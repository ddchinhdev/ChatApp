package com.chatapp.dto.user;

import com.chatapp.entity.Role;
public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        Role role
) {}
