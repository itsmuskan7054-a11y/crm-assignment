package com.palmonas.crm.infrastructure.lifecycle;

import com.palmonas.crm.config.AppProperties;
import com.palmonas.crm.module.user.model.Role;
import com.palmonas.crm.module.user.model.User;
import com.palmonas.crm.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        String email = appProperties.getAdmin().getEmail();
        String encodedPassword = passwordEncoder.encode(appProperties.getAdmin().getPassword());

        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    if (!passwordEncoder.matches(appProperties.getAdmin().getPassword(), existing.getPasswordHash())) {
                        existing.setPasswordHash(encodedPassword);
                        userRepository.save(existing);
                        log.info("Admin password updated for: {}", email);
                    } else {
                        log.info("Admin user already exists: {}", email);
                    }
                },
                () -> {
                    User admin = User.builder()
                            .email(email)
                            .passwordHash(encodedPassword)
                            .fullName("System Admin")
                            .role(Role.SUPER_ADMIN)
                            .build();
                    userRepository.save(admin);
                    log.info("Default admin user created: {}", email);
                }
        );
    }
}
