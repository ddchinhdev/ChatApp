package com.chatapp.dto.message;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RealtimeReceiptRequest(
        @NotNull @Positive Long conversationId,
        @NotNull @Positive Long messageId
) {}
