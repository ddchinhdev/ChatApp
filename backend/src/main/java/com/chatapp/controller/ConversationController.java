package com.chatapp.controller;

import com.chatapp.dto.conversation.ConversationResponse;
import com.chatapp.dto.conversation.UnreadTotalResponse;
import com.chatapp.service.ConversationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Direct conversations")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct/{userId}")
    public ResponseEntity<ConversationResponse> createDirect(@PathVariable Long userId, Authentication authentication) {
        ConversationService.CreateResult result = conversationService.createOrGetDirect(authentication.getName(), userId);
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/conversations/" + result.conversation().id()))
                    .body(result.conversation());
        }
        return ResponseEntity.ok(result.conversation());
    }

    @GetMapping
    public List<ConversationResponse> list(Authentication authentication) {
        return conversationService.list(authentication.getName());
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse get(@PathVariable Long conversationId, Authentication authentication) {
        return conversationService.get(authentication.getName(), conversationId);
    }

    @GetMapping("/unread-count")
    public UnreadTotalResponse unreadTotal(Authentication authentication) {
        return new UnreadTotalResponse(conversationService.totalUnread(authentication.getName()));
    }
}
