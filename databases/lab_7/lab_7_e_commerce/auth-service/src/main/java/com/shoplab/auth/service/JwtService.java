package com.shoplab.auth.service;

import java.util.List;

public interface JwtService {
    String generateAccessToken(String username, Long userId, List<String> roles);
    String generateRefreshToken(String username);
    String getUsername(String refreshToken);
    void assertRefresh(String refreshToken);
}
