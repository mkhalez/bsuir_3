package com.shoplab.auth.service;

import com.shoplab.auth.config.RsaKeyProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final RsaKeyProperties keys;

    @Value("${security.issuer:auth-service}")
    private String issuer = "auth-service";
    @Value("${security.access-token-minutes:10}")
    private long accessMinutes = 10;
    @Value("${security.refresh-token-minutes:60}")
    private long refreshMinutes = 60;

    @Override
    public String generateAccessToken(String username, Long userId, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(username)
                .claim("roles", roles).claim("type", "access").claim("userId", userId)
                .issuedAt(now).expiresAt(now.plus(accessMinutes, ChronoUnit.MINUTES)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(keys.kid()).build(), claims)).getTokenValue();
    }

    @Override
    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(username)
                .claim("type", "refresh")
                .issuedAt(now).expiresAt(now.plus(refreshMinutes, ChronoUnit.MINUTES)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(keys.kid()).build(), claims)).getTokenValue();
    }

    @Override
    public String getUsername(String refreshToken) {
        return decoder.decode(refreshToken).getSubject();
    }

    @Override
    public void assertRefresh(String refreshToken) {
        Jwt jwt = decoder.decode(refreshToken);
        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw new IllegalArgumentException("Not a refresh token");
        }
    }
}
