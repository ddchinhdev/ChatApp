package com.chatapp.config;

import com.chatapp.entity.Role;
import com.chatapp.entity.User;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String userPassword;
    private final String adminPassword;

    public DemoDataSeeder(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.demo-seed.user-password:}") String userPassword,
                          @Value("${app.demo-seed.admin-password:}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.userPassword = userPassword;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userPassword.length() < 8 || adminPassword.length() < 8) {
            throw new IllegalStateException("Demo seed passwords must contain at least 8 characters");
        }
        createIfMissing("demo", "demo@example.local", "Demo User", Role.USER, userPassword);
        createIfMissing("admin", "admin@example.local", "Demo Admin", Role.ADMIN, adminPassword);
    }

    private void createIfMissing(String username, String email, String displayName, Role role, String password) {
        if (users.existsByUsernameIgnoreCase(username)) return;
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setPassword(encoder.encode(password));
        users.save(user);
    }
}
