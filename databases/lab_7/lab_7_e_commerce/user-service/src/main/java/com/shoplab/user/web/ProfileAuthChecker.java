package com.shoplab.user.web;

import com.shoplab.user.repo.ProfileJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("profileAuthChecker")
@RequiredArgsConstructor
public class ProfileAuthChecker {

    private final ProfileJdbcRepository profiles;

    public boolean isOwner(long profileId, Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt)) return false;
        Object claim = jwt.getToken().getClaim("userId");
        if (claim == null) return false;
        long authUserId = Long.parseLong(claim.toString());
        return profiles.authIdByProfileId(profileId).map(id -> id == authUserId).orElse(false);
    }
}
