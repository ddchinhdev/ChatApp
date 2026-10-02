package com.chatapp.controller;

import com.chatapp.dto.message.MessagePageResponse;
import com.chatapp.dto.message.MessageResponse;
import com.chatapp.dto.message.SendMessageRequest;
import com.chatapp.service.ChatService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@Validated
@Tag(name = "Messages", description = "REST message history and fallback sending")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {
    private final ChatService chatService;

    public MessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable Long conversationId,
                                @Valid @RequestBody SendMessageRequest request,
                                Authentication authentication) {
        return chatService.send(authentication.getName(), conversationId, request);
    }

    @GetMapping
    public MessagePageResponse history(@PathVariable Long conversationId,
                                       @RequestParam(required = false) String cursor,
                                       @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
                                       Authentication authentication) {
        return chatService.history(authentication.getName(), conversationId, cursor, size);
    }
}
