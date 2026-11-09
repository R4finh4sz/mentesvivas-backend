package com.mentesvivas.backend.config;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.util.Normalization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final int PASSWORD_MIN_LENGTH = 12;
    private static final int PASSWORD_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalizedEmail = Normalization.email(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            return;
        }
        validatePassword();

        User admin = new User();
        admin.setFullName("Administrador");
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMINISTRATOR);
        admin.setEmailConfirmed(true);
        userRepository.save(admin);
    }

    private void validatePassword() {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new InvalidConfigurationException(
                    "BOOTSTRAP_ADMIN_PASSWORD must have at least 12 characters");
        }
    }
}
