package com.chatapp.service;

import com.chatapp.dto.auth.*;
import com.chatapp.dto.user.UserResponse;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
import com.chatapp.mapper.UserMapper;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(username);

        userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(user.getUsername()),
                userMapper.toCurrentUserResponse(user)
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new BusinessException(
                        "Invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(
                    "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("Account is inactive", HttpStatus.FORBIDDEN);
        }

        return new AuthResponse(
                jwtService.generateToken(user.getUsername()),
                userMapper.toCurrentUserResponse(user)
        );
    }
}
