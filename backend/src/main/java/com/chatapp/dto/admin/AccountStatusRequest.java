package com.chatapp.dto.admin;

import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull Boolean active) {}
