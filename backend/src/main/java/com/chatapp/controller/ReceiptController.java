package com.chatapp.controller;

import com.chatapp.dto.message.ReceiptRequest;
import com.chatapp.dto.message.ReceiptResponse;
import com.chatapp.service.ReceiptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations/{conversationId}/receipts")
@Tag(name = "Messages")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) { this.receiptService = receiptService; }

    @PostMapping("/delivered")
    public ReceiptResponse delivered(@PathVariable Long conversationId, @Valid @RequestBody ReceiptRequest request,
                                     Authentication authentication) {
        return receiptService.markDelivered(authentication.getName(), conversationId, request.messageId());
    }

    @PostMapping("/read")
    public ReceiptResponse read(@PathVariable Long conversationId, @Valid @RequestBody ReceiptRequest request,
                                Authentication authentication) {
        return receiptService.markRead(authentication.getName(), conversationId, request.messageId());
    }
}
