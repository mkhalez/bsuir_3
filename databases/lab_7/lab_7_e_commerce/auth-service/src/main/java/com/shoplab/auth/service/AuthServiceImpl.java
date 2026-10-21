package com.shoplab.auth.service;

import com.shoplab.auth.dto.*;
import com.shoplab.auth.repo.AuditLogRepository;
import com.shoplab.auth.repo.RegistrationEventRepository;
import com.shoplab.auth.repo.UserRepository;
import com.shoplab.auth.saga.RegistrationMessage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final RegistrationEventRepository events;
    private final AuditLogRepository audit;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    @Override
    @Transactional
    public UUID register(SignupRequest req, String ip) {
        if (users.existsByUsername(req.username())) {
            throw new IllegalStateException("User already exists: " + req.username());
        }
        long userId = users.insert(req.username(), encoder.encode(req.password()), false);
        users.assignRole(userId, "USER");
        UUID eventId = UUID.randomUUID();
        events.save(eventId, userId, new RegistrationMessage(eventId, userId, req.username(),
                req.firstName(), req.lastName(), req.email(), req.birthDate()));
        audit.log(userId, req.username(), "SIGNUP_INITIATED", "event=" + eventId, ip);
        return eventId;
    }

    @Override
    public AuthResponse login(LoginRequest req, String ip) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException | DisabledException e) {
            audit.log(null, req.username(), "LOGIN_FAILED", e.getMessage(), ip);
            throw new BadCredentialsException("Bad credentials");
        }
        var u = users.findByUsername(req.username()).orElseThrow();
        if (!u.active()) {
            audit.log(u.id(), u.username(), "LOGIN_BLOCKED", "inactive (saga pending or banned)", ip);
            throw new DisabledException("User is not active yet");
        }
        List<String> roles = u.roles();
        audit.log(u.id(), u.username(), "LOGIN", "ok", ip);
        return new AuthResponse(jwt.generateAccessToken(u.username(), u.id(), roles),
                jwt.generateRefreshToken(u.username()), u.username(), u.id(), roles);
    }

    @Override
    public AuthResponse refresh(RefreshRequest req, String ip) {
        jwt.assertRefresh(req.refreshToken());
        String username = jwt.getUsername(req.refreshToken());
        var u = users.findByUsername(username).orElseThrow();
        if (!u.active()) throw new DisabledException("User is not active");
        audit.log(u.id(), username, "TOKEN_REFRESH", "ok", ip);
        return new AuthResponse(jwt.generateAccessToken(username, u.id(), u.roles()),
                req.refreshToken(), username, u.id(), u.roles());
    }

    @Override
    public RegistrationStatusResponse status(UUID id) {
        var e = events.findById(id);
        if (e.isEmpty()) return new RegistrationStatusResponse(id, "NOT_FOUND", "no such registration");
        String msg = switch (e.get().status()) {
            case "SUCCESS" -> "Profile created, you can login";
            case "FAIL" -> "Profile creation failed (duplicate email?)";
            case "FAIL_TO_SEND" -> "Outbox failed, contact support";
            default -> "In progress";
        };
        return new RegistrationStatusResponse(id, e.get().status(), msg);
    }

    @Override
    @Transactional
    public void setActive(long userId, boolean active) {
        users.setActive(userId, active);
        var u = users.findById(userId);
        audit.log(userId, u.map(x -> x.username()).orElse("?"), active ? "ENABLE" : "DISABLE", "by internal api", null);
    }

    @Override
    @Transactional
    public void deleteUser(long userId) {
        var u = users.findById(userId);
        users.deleteById(userId);
        audit.log(null, u.map(x -> x.username()).orElse("?"), "DELETE", "id=" + userId, null);
    }
}
