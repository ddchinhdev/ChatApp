package com.chatapp.service;

import com.chatapp.dto.conversation.*;
import com.chatapp.entity.*;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final UserRepository users;
    private final ConversationService conversationService;
    private final ApplicationEventPublisher eventPublisher;

    public GroupService(ConversationRepository conversations, ConversationMemberRepository members,
                        UserRepository users, ConversationService conversationService,
                        ApplicationEventPublisher eventPublisher) {
        this.conversations = conversations;
        this.members = members;
        this.users = users;
        this.conversationService = conversationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ConversationResponse create(String username, CreateGroupRequest request) {
        User owner = requireUser(username);
        Set<Long> requestedIds = new LinkedHashSet<>(request.memberIds() == null ? List.of() : request.memberIds());
        requestedIds.remove(owner.getId());
        List<User> invited = users.findAllById(requestedIds);
        if (invited.size() != requestedIds.size()) {
            throw new BusinessException("One or more users do not exist", HttpStatus.NOT_FOUND);
        }
        Conversation group = new Conversation();
        group.setGroup(true);
        group.setName(request.name().trim());
        group.setAvatarUrl(normalize(request.avatarUrl()));
        Conversation savedGroup = conversations.saveAndFlush(group);
        addMembership(savedGroup, owner, GroupRole.OWNER);
        invited.forEach(user -> addMembership(savedGroup, user, GroupRole.MEMBER));
        members.flush();
        return conversationService.get(username, savedGroup.getId());
    }

    @Transactional
    public ConversationResponse update(String username, Long conversationId, UpdateGroupRequest request) {
        User actor = requireUser(username);
        Conversation group = requireGroup(conversationId);
        requireManager(conversationId, actor.getId());
        group.setName(request.name().trim());
        group.setAvatarUrl(normalize(request.avatarUrl()));
        group.setUpdatedAt(LocalDateTime.now());
        conversations.save(group);
        return conversationService.get(username, conversationId);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> listMembers(String username, Long conversationId) {
        User actor = requireUser(username);
        requireGroup(conversationId);
        requireMembership(conversationId, actor.getId());
        return members.findByConversationIdOrderByIdAsc(conversationId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GroupMemberResponse addMember(String username, Long conversationId, Long userId) {
        User actor = requireUser(username);
        Conversation group = requireGroup(conversationId);
        requireManager(conversationId, actor.getId());
        if (members.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BusinessException("User is already a group member", HttpStatus.CONFLICT);
        }
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        try {
            ConversationMember member = addMembership(group, user, GroupRole.MEMBER);
            members.flush();
            systemMessage(conversationId, username, actor.getDisplayName() + " đã thêm " + user.getDisplayName());
            return toResponse(member);
        } catch (DataIntegrityViolationException duplicate) {
            throw new BusinessException("User is already a group member", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public GroupMemberResponse updateRole(String username, Long conversationId, Long userId, GroupRole role) {
        User actor = requireUser(username);
        requireGroup(conversationId);
        requireRole(conversationId, actor.getId(), GroupRole.OWNER);
        if (role == GroupRole.OWNER) {
            throw new BusinessException("Use ownership transfer to assign the owner role", HttpStatus.BAD_REQUEST);
        }
        ConversationMember target = requireMembership(conversationId, userId);
        if (target.getGroupRole() == GroupRole.OWNER) {
            throw new BusinessException("The owner role cannot be changed directly", HttpStatus.BAD_REQUEST);
        }
        target.setGroupRole(role);
        systemMessage(conversationId, username, target.getUser().getDisplayName() + " hiện là " + role.name());
        return toResponse(members.save(target));
    }

    @Transactional
    public void removeMember(String username, Long conversationId, Long userId) {
        User actor = requireUser(username);
        requireGroup(conversationId);
        ConversationMember actorMember = requireManager(conversationId, actor.getId());
        ConversationMember target = requireMembership(conversationId, userId);
        if (actor.getId().equals(userId)) {
            throw new BusinessException("Use leave group to remove yourself", HttpStatus.BAD_REQUEST);
        }
        if (target.getGroupRole() == GroupRole.OWNER
                || (actorMember.getGroupRole() == GroupRole.ADMIN && target.getGroupRole() != GroupRole.MEMBER)) {
            throw new BusinessException("You cannot remove this group member", HttpStatus.FORBIDDEN);
        }
        members.delete(target);
        systemMessage(conversationId, username, actor.getDisplayName() + " đã xóa " + target.getUser().getDisplayName());
    }

    @Transactional
    public void leave(String username, Long conversationId) {
        User actor = requireUser(username);
        Conversation group = requireGroup(conversationId);
        ConversationMember membership = requireMembership(conversationId, actor.getId());
        if (membership.getGroupRole() == GroupRole.OWNER) {
            if (members.findByConversationIdOrderByIdAsc(conversationId).size() > 1) {
                throw new BusinessException("Transfer ownership before leaving the group", HttpStatus.CONFLICT);
            }
            group.setLastMessage(null);
            conversations.saveAndFlush(group);
            conversations.delete(group);
            return;
        }
        members.delete(membership);
    }

    @Transactional
    public GroupMemberResponse transferOwner(String username, Long conversationId, Long targetUserId) {
        User actor = requireUser(username);
        requireGroup(conversationId);
        ConversationMember owner = requireRole(conversationId, actor.getId(), GroupRole.OWNER);
        ConversationMember target = requireMembership(conversationId, targetUserId);
        if (owner.getId().equals(target.getId())) {
            throw new BusinessException("Select another member as owner", HttpStatus.BAD_REQUEST);
        }
        owner.setGroupRole(GroupRole.ADMIN);
        target.setGroupRole(GroupRole.OWNER);
        members.save(owner);
        systemMessage(conversationId, username, target.getUser().getDisplayName() + " đã trở thành owner");
        return toResponse(members.save(target));
    }

    private ConversationMember addMembership(Conversation conversation, User user, GroupRole role) {
        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        member.setGroupRole(role);
        return members.save(member);
    }

    private Conversation requireGroup(Long id) {
        Conversation conversation = conversations.findById(id)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(conversation.getGroup())) {
            throw new BusinessException("Conversation is not a group", HttpStatus.BAD_REQUEST);
        }
        return conversation;
    }

    private ConversationMember requireManager(Long conversationId, Long userId) {
        ConversationMember member = requireMembership(conversationId, userId);
        if (member.getGroupRole() != GroupRole.OWNER && member.getGroupRole() != GroupRole.ADMIN) {
            throw new BusinessException("Group management permission is required", HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private ConversationMember requireRole(Long conversationId, Long userId, GroupRole role) {
        ConversationMember member = requireMembership(conversationId, userId);
        if (member.getGroupRole() != role) {
            throw new BusinessException(role + " permission is required", HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private ConversationMember requireMembership(Long conversationId, Long userId) {
        return members.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException("You are not a member of this conversation", HttpStatus.FORBIDDEN));
    }

    private User requireUser(String username) {
        return users.findByUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private GroupMemberResponse toResponse(ConversationMember member) {
        User user = member.getUser();
        return new GroupMemberResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl(),
                member.getGroupRole().name(), member.getJoinedAt());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void systemMessage(Long conversationId, String username, String content) {
        eventPublisher.publishEvent(new GroupSystemMessageEvent(conversationId, username, content));
    }
}
