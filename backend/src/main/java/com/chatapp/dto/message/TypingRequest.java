package com.chatapp.dto.message;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TypingRequest(
        @NotNull @Positive Long conversationId,
        boolean typing
) {}
