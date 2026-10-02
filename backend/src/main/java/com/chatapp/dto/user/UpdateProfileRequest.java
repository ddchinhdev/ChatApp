package com.chatapp.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        @Pattern(
                regexp = "^$|^https?://.+$",
                message = "Avatar URL must start with http:// or https://"
        )
        String avatarUrl
) {}
