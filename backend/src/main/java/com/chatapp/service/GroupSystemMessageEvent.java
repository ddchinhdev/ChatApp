package com.chatapp.service;

public record GroupSystemMessageEvent(Long conversationId, String actorUsername, String content) {}
