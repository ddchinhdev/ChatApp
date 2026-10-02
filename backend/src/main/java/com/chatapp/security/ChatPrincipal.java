package com.chatapp.security;

import java.security.Principal;

public record ChatPrincipal(Long userId, String username) implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
