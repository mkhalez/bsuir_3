package com.shoplab.auth.service;

import com.shoplab.auth.dto.LoginRequest;
import com.shoplab.auth.dto.SignupRequest;
import com.shoplab.auth.repo.AuditLogRepository;
import com.shoplab.auth.repo.RegistrationEventRepository;
import com.shoplab.auth.repo.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository users;
    @Mock RegistrationEventRepository events;
    @Mock AuditLogRepository audit;
    @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtService jwt;

    @InjectMocks AuthServiceImpl service;

    private SignupRequest signup() {
        return new SignupRequest("john 설명했다", "secret123", "John", "Doe", "john@shop.by", LocalDate.of(1999, 5, 1));
    }

    @Test
    void registerCreatesInactiveUserAndOutboxEvent() {
        Mockito.when(users.existsByUsername("john 설명했다")).thenReturn(false);
        Mockito.when(encoder.encode("secret123")).thenReturn("HASH");
        Mockito.when(users.insert(eq("john 설명했다"), eq("HASH"), eq(false))).thenReturn(42L);

        var eventId = service.register(signup(), "127.0.0.1");

        assertNotNull(eventId);
        Mockito.verify(users).insert("john 설명했다", "HASH", false);
        Mockito.verify(users).assignRole(42L, "USER");
        Mockito.verify(events).save(eq(eventId), eq(42L), any());
        Mockito.verify(audit).log(eq(42L), eq("john 설명했다"), eq("SIGNUP_INITIATED"), any(), eq("127.0.0.1"));
    }

    @Test
    void registerDuplicateThrows() {
        Mockito.when(users.existsByUsername("john 설명했다")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.register(signup(), null));
    }

    @Test
    void loginActiveUserReturnsTokens() {
        var u = new com.shoplab.auth.domain.AuthUser(7L, "john@example.com", "HASH", true, List.of("USER"), null);
        Mockito.when(users.findByUsername("john@example.com")).thenReturn(Optional.of(u));
        Mockito.when(jwt.generateAccessToken(any(), any(), any())).thenReturn("ACCESS");
        Mockito.when(jwt.generateRefreshToken(any())).thenReturn("REFRESH");

        var resp = service.login(new LoginRequest("john@example.com", "secret123"), "10.0.0.1");

        assertEquals("ACCESS", resp.accessToken());
        assertEquals("REFRESH", resp.refreshToken());
        assertEquals(7L, resp.userId());
    }

    @Test
    void loginInactiveUserThrows() {
        var u = new com.shoplab.auth.domain.AuthUser(7L, "john@example.com", "HASH", false, List.of("USER"), null);
        Mockito.when(users.findByUsername("john@example.com")).thenReturn(Optional.of(u));
        assertThrows(Exception.class, () -> service.login(new LoginRequest("john@example.com", "secret123"), null));
    }
}
