package com.chatapp.controller;

import com.chatapp.dto.common.PageResponse;
import com.chatapp.dto.user.*;
import com.chatapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Users", description = "Current user profile and user search")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update allowed profile fields")
    public ResponseEntity<UserResponse> update(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                userService.updateProfile(authentication.getName(), request)
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search active users by username or display name")
    public ResponseEntity<PageResponse<UserSearchResponse>> search(
            @RequestParam(defaultValue = "")
            @Size(max = 100, message = "Search query must not exceed 100 characters") String q,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 50, message = "Size must not exceed 50") int size) {
        return ResponseEntity.ok(userService.search(q, page, size));
    }
}
