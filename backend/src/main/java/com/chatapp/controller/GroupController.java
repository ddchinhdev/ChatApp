package com.chatapp.controller;

import com.chatapp.dto.conversation.*;
import com.chatapp.service.GroupService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Conversations", description = "Direct and group conversation management")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(@Valid @RequestBody CreateGroupRequest request, Authentication auth) {
        return groupService.create(auth.getName(), request);
    }

    @PatchMapping("/{id}/group")
    public ConversationResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGroupRequest request,
                                       Authentication auth) {
        return groupService.update(auth.getName(), id, request);
    }

    @GetMapping("/{id}/members")
    public List<GroupMemberResponse> members(@PathVariable Long id, Authentication auth) {
        return groupService.listMembers(auth.getName(), id);
    }

    @PostMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse add(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
        return groupService.addMember(auth.getName(), id, userId);
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public GroupMemberResponse role(@PathVariable Long id, @PathVariable Long userId,
                                    @Valid @RequestBody UpdateGroupRoleRequest request, Authentication auth) {
        return groupService.updateRole(auth.getName(), id, userId, request.role());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
        groupService.removeMember(auth.getName(), id, userId);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, Authentication auth) {
        groupService.leave(auth.getName(), id);
    }

    @PostMapping("/{id}/transfer-owner/{userId}")
    public GroupMemberResponse transfer(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
        return groupService.transferOwner(auth.getName(), id, userId);
    }
}
