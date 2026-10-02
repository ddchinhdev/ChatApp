package com.chatapp.controller;

import com.chatapp.dto.admin.*;
import com.chatapp.dto.common.PageResponse;
import com.chatapp.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@Validated
@Tag(name = "Administration", description = "ADMIN-only user metadata, statistics and audit logs")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping("/stats")
    public AdminStatsResponse stats(Authentication auth) { return adminService.stats(auth.getName()); }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(Authentication auth,
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.users(auth.getName(), q, page, size);
    }

    @GetMapping("/users/{userId}")
    public AdminUserResponse user(Authentication auth, @PathVariable Long userId) {
        return adminService.user(auth.getName(), userId);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserResponse status(Authentication auth, @PathVariable Long userId,
                                    @Valid @RequestBody AccountStatusRequest request) {
        return adminService.setActive(auth.getName(), userId, request.active());
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> audit(Authentication auth,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.audit(auth.getName(), page, size);
    }
}
