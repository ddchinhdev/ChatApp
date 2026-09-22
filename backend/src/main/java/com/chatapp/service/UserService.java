package com.chatapp.service;

import com.chatapp.dto.user.UpdateProfileRequest;
import com.chatapp.dto.user.UserResponse;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getCurrentUser(String username) {
        return UserResponse.from(findByUsername(username));
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);
        user.setDisplayName(request.displayName().trim());
        return UserResponse.from(userRepository.save(user));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }
}
