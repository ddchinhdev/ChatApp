package com.chatapp.service;

import com.chatapp.dto.message.TypingEventResponse;
import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TypingServiceTest {
    @Test
    void publishesTypingStartAndStopOnlyToOtherConversationMembers() {
        User alice = user(1L, "alice", "Alice");
        User bob = user(2L, "bob", "Bob");
        UserRepository users = mock(UserRepository.class);
        ConversationMemberRepository members = mock(ConversationMemberRepository.class);
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(members.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);
        ConversationMember aliceMember = member(alice);
        ConversationMember bobMember = member(bob);
        when(members.findByConversationIdOrderByIdAsc(10L)).thenReturn(List.of(aliceMember, bobMember));
        TypingService service = new TypingService(users, members, template);

        service.publish("alice", 10L, true);
        service.publish("alice", 10L, false);

        verify(template, times(2)).convertAndSendToUser(eq("bob"), eq("/queue/typing"),
                any(TypingEventResponse.class));
        verify(template, never()).convertAndSendToUser(eq("alice"), anyString(), any());
    }

    @Test
    void rejectsTypingForConversationOutsideUserMembershipWithoutLeakingEvent() {
        User alice = user(1L, "alice", "Alice");
        UserRepository users = mock(UserRepository.class);
        ConversationMemberRepository members = mock(ConversationMemberRepository.class);
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(members.existsByConversationIdAndUserId(99L, 1L)).thenReturn(false);
        TypingService service = new TypingService(users, members, template);

        assertThatThrownBy(() -> service.publish("alice", 99L, true)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(template);
    }

    private User user(Long id, String username, String displayName) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.getDisplayName()).thenReturn(displayName);
        when(user.getActive()).thenReturn(true);
        return user;
    }

    private ConversationMember member(User user) {
        ConversationMember member = mock(ConversationMember.class);
        when(member.getUser()).thenReturn(user);
        return member;
    }
}
