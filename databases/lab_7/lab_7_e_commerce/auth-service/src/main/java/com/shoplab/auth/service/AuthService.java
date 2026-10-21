package com.shoplab.auth.service;

import com.shoplab.auth.dto.*;
import java.util.UUID;

public interface AuthService {
    UUID register(SignupRequest req, String ip);
    AuthResponse login(LoginRequest req, String ip);
    AuthResponse refresh(RefreshRequest req, String ip);
    RegistrationStatusResponse status(UUID id);
    void setActive(long userId, boolean active);
    void deleteUser(long userId);
}
