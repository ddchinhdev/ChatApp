package com.chatapp.dto.admin;

import java.time.LocalDateTime;

public record AdminUserResponse(Long id, String username, String email, String displayName, String avatarUrl,
                                String role, boolean active, LocalDateTime createdAt, LocalDateTime lastSeenAt) {}
