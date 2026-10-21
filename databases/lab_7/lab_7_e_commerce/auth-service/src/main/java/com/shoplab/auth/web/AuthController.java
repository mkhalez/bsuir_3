package com.shoplab.auth.web;

import com.shoplab.auth.dto.*;
import com.shoplab.auth.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/signup")
    public ResponseEntity<RegistrationStatusResponse> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
        UUID id = auth.register(req, http.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/auth/status/" + id))
                .body(new RegistrationStatusResponse(id, "CREATED", "Registration accepted, poll status"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return ResponseEntity.ok(auth.login(req, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        return ResponseEntity.ok(auth.refresh(req, http.getRemoteAddr()));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<RegistrationStatusResponse> status(@PathVariable UUID id) {
        return ResponseEntity.ok(auth.status(id));
    }
}
