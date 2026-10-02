package com.chatapp.service;

import com.chatapp.dto.message.MessageResponse;
import java.util.List;

public record ChatMessageCreatedEvent(MessageResponse message, List<String> recipientUsernames) {}
