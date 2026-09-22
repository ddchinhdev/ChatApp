package com.chatapp.dto.user;

import com.chatapp.entity.Role;
import com.chatapp.entity.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}
