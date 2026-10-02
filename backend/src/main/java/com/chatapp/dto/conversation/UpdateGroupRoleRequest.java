package com.chatapp.dto.conversation;

import com.chatapp.entity.GroupRole;
import jakarta.validation.constraints.NotNull;

public record UpdateGroupRoleRequest(@NotNull GroupRole role) {}
