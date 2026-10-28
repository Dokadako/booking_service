package com.dokadako.booking.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dokadako.booking.common.ConflictException;
import com.dokadako.booking.user.Role;
import com.dokadako.booking.user.User;
import com.dokadako.booking.user.UserRepository;

@Service
class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                TokenService tokenService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Transactional
    AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()),
                Role.USER, Instant.now(clock)));
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return tokenService.issue(user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
