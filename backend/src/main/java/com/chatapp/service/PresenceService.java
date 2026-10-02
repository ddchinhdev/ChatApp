package com.chatapp.service;

import com.chatapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private final UserRepository userRepository;
    private final ConcurrentHashMap<Long, Set<String>> sessions = new ConcurrentHashMap<>();

    public PresenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void connected(Long userId, String sessionId) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    @Transactional
    public boolean disconnected(Long userId, String sessionId) {
        Set<String> active = sessions.get(userId);
        if (active == null) return false;
        active.remove(sessionId);
        if (!active.isEmpty()) return false;
        if (!sessions.remove(userId, active)) return false;
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(LocalDateTime.now(Clock.systemUTC()));
            userRepository.save(user);
        });
        return true;
    }

    public boolean isOnline(Long userId) {
        Set<String> active = sessions.get(userId);
        return active != null && !active.isEmpty();
    }

    public int activeSessionCount(Long userId) {
        Set<String> active = sessions.get(userId);
        return active == null ? 0 : active.size();
    }
}
