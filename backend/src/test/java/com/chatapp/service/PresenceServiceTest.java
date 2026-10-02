package com.chatapp.service;

import com.chatapp.entity.User;
import com.chatapp.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PresenceServiceTest {
    @Test
    void userOnlyGoesOfflineAfterLastTabDisconnects() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User();
        user.setUsername("two_tabs");
        user.setEmail("two.tabs@example.com");
        user.setPassword("unused");
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        PresenceService presence = new PresenceService(repository);

        presence.connected(7L, "tab-one");
        presence.connected(7L, "tab-two");
        assertThat(presence.activeSessionCount(7L)).isEqualTo(2);

        assertThat(presence.disconnected(7L, "tab-one")).isFalse();
        assertThat(presence.isOnline(7L)).isTrue();
        assertThat(user.getLastSeenAt()).isNull();
        verify(repository, never()).save(any());

        LocalDateTime beforeUtc = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        assertThat(presence.disconnected(7L, "tab-two")).isTrue();
        assertThat(presence.isOnline(7L)).isFalse();
        assertThat(user.getLastSeenAt()).isAfter(beforeUtc)
                .isBefore(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1));
        verify(repository).save(user);
    }
}
