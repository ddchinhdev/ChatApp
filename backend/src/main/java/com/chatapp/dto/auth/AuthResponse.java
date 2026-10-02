package com.chatapp.dto.auth;

import com.chatapp.dto.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, UserResponse user) {
    public AuthResponse(String accessToken, UserResponse user) {
        this(accessToken, "Bearer", user);
    }
}
