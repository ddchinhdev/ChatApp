package com.chatapp.dto.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String avatarUrl
) {}
