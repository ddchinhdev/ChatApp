package com.chatapp.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record SendMessageRequest(
        @NotBlank(message = "Message content must not be blank")
        @Size(max = 4000, message = "Message content must not exceed 4000 characters")
        String content,
        @NotBlank(message = "clientMessageId must not be blank")
        @Size(max = 64, message = "clientMessageId must not exceed 64 characters")
        @Pattern(regexp = "[A-Za-z0-9._:-]+", message = "clientMessageId contains invalid characters")
        String clientMessageId
) {}
