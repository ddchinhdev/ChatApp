package com.chatapp.service;

import com.chatapp.dto.common.PageResponse;
import com.chatapp.dto.user.UpdateProfileRequest;
import com.chatapp.dto.user.UserResponse;
import com.chatapp.dto.user.UserSearchResponse;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.mapper.UserMapper;
import com.chatapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        return userMapper.toCurrentUserResponse(findByUsername(username));
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setBio(normalizeOptional(request.bio()));
        user.setAvatarUrl(normalizeOptional(request.avatarUrl()));
        return userMapper.toCurrentUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSearchResponse> search(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("id"))
        );
        Page<UserSearchResponse> results = userRepository
                .searchActiveUsers(query.trim(), pageable)
                .map(userMapper::toSearchResponse);
        return PageResponse.from(results);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
