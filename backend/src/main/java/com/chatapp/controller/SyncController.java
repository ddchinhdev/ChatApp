package com.chatapp.controller;

import com.chatapp.dto.message.MessageSyncResponse;
import com.chatapp.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@Validated
@Tag(name = "Messages")
public class SyncController {
    private final ChatService chatService;

    public SyncController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/messages")
    public MessageSyncResponse messages(
            @RequestParam(defaultValue = "0") @Min(0) Long afterMessageId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit,
            Authentication authentication) {
        return chatService.sync(authentication.getName(), afterMessageId, limit);
    }
}
