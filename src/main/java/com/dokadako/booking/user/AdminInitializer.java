package com.dokadako.booking.user;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first administrator from environment variables, so no credentials live in code or migrations.
 */
@Component
class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    AdminInitializer(AdminProperties properties, UserRepository userRepository,
                     PasswordEncoder passwordEncoder, Clock clock) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set, skipping admin creation");
            return;
        }
        if (userRepository.existsByEmail(properties.email())) {
            return;
        }
        userRepository.save(new User(properties.email(), passwordEncoder.encode(properties.password()),
                Role.ADMIN, Instant.now(clock)));
        log.info("Created admin user {}", properties.email());
    }
}
