package com.chatapp.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RealtimeSendMessageRequest(
        @NotNull(message = "conversationId is required") Long conversationId,
        @NotBlank(message = "Message content must not be blank")
        @Size(max = 4000, message = "Message content must not exceed 4000 characters") String content,
        @NotBlank(message = "clientMessageId must not be blank")
        @Size(max = 64, message = "clientMessageId must not exceed 64 characters")
        @Pattern(regexp = "[A-Za-z0-9._:-]+", message = "clientMessageId contains invalid characters") String clientMessageId
) {
    public SendMessageRequest toSendRequest() {
        return new SendMessageRequest(content, clientMessageId);
    }
}
