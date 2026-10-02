package com.chatapp.service;

import com.chatapp.dto.message.ReceiptResponse;

public record ReceiptUpdatedEvent(String senderUsername, ReceiptResponse receipt) {}
