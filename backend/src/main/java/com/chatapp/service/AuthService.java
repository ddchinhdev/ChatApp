package com.chatapp.service;

import com.chatapp.dto.auth.*;
import com.chatapp.dto.user.UserResponse;
import com.chatapp.entity.User;
import com.chatapp.exception.BusinessException;
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

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.username().trim());

        userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(user.getUsername()),
                UserResponse.from(user)
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
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
                UserResponse.from(user)
        );
    }
}
