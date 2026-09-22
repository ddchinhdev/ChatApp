package com.chatapp.dto.auth;

import com.chatapp.dto.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {}
