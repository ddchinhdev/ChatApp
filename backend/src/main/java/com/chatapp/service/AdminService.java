package com.chatapp.service;

import com.chatapp.dto.admin.*;
import com.chatapp.dto.common.PageResponse;
import com.chatapp.entity.AdminAuditLog;
import com.chatapp.entity.Role;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final AdminAuditLogRepository auditLogs;

    public AdminService(UserRepository users, ConversationRepository conversations,
                        MessageRepository messages, AdminAuditLogRepository auditLogs) {
        this.users = users;
        this.conversations = conversations;
        this.messages = messages;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats(String adminUsername) {
        requireAdmin(adminUsername);
        return new AdminStatsResponse(users.count(), users.countByActiveTrue(), conversations.count(), messages.count());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(String adminUsername, String query, int page, int size) {
        requireAdmin(adminUsername);
        Page<User> result = users.searchForAdmin(query == null ? "" : query.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(result.getContent().stream().map(this::toUser).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse user(String adminUsername, Long userId) {
        requireAdmin(adminUsername);
        return toUser(requireUser(userId));
    }

    @Transactional
    public AdminUserResponse setActive(String adminUsername, Long userId, boolean active) {
        User admin = requireAdmin(adminUsername);
        User target = requireUser(userId);
        if (admin.getId().equals(target.getId()) && !active) {
            throw new BusinessException("Administrators cannot lock their own account", HttpStatus.BAD_REQUEST);
        }
        if (Boolean.TRUE.equals(target.getActive()) == active) return toUser(target);
        target.setActive(active);
        users.save(target);
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(admin);
        log.setTargetUser(target);
        log.setAction(active ? "USER_UNBANNED" : "USER_BANNED");
        log.setDetails((active ? "Unlocked account " : "Locked account ") + target.getUsername());
        auditLogs.save(log);
        return toUser(target);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> audit(String adminUsername, int page, int size) {
        requireAdmin(adminUsername);
        Page<AdminAuditLog> result = auditLogs.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new PageResponse<>(result.getContent().stream().map(this::toAudit).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    private User requireAdmin(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        if (user.getRole() != Role.ADMIN) throw new BusinessException("Administrator permission is required", HttpStatus.FORBIDDEN);
        return user;
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private AdminUserResponse toUser(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getAvatarUrl(), user.getRole().name(), Boolean.TRUE.equals(user.getActive()), user.getCreatedAt(),
                user.getLastSeenAt());
    }

    private AuditLogResponse toAudit(AdminAuditLog log) {
        User target = log.getTargetUser();
        return new AuditLogResponse(log.getId(), log.getAdmin().getId(), log.getAdmin().getUsername(), log.getAction(),
                target == null ? null : target.getId(), target == null ? null : target.getUsername(),
                log.getDetails(), log.getCreatedAt());
    }
}
