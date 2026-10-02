package com.chatapp.dto.admin;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, Long adminUserId, String adminUsername, String action,
                               Long targetUserId, String targetUsername, String details, LocalDateTime createdAt) {}
