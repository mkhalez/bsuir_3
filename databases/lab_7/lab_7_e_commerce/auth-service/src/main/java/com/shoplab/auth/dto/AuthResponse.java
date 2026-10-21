package com.shoplab.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(String accessToken, String refreshToken, String username,
                           Long userId, List<String> roles) {
}
