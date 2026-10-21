package com.shoplab.user.service;

import com.shoplab.user.client.AuthInternalClient;
import com.shoplab.user.domain.Profile;
import com.shoplab.user.dto.UpdateProfileRequest;
import com.shoplab.user.repo.ProfileJdbcRepository;
import com.shoplab.user.repo.SagaAuditRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileJdbcRepository profiles;
    private final SagaAuditRepository sagaAudit;
    private final AuthInternalClient authClient;

    @Cacheable(value = "profiles", key = "#id")
    public Profile getById(long id) {
        return profiles.findById(id).orElseThrow(() -> new IllegalArgumentException("Profile not found: " + id));
    }

    @Cacheable(value = "profiles", key = "'email:' + #email")
    public Profile getByEmail(String email) {
        return profiles.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Profile not found: " + email));
    }

    public List<Profile> search(String firstName, String lastName, int limit, Long cursorId) {
        return profiles.search(firstName, lastName, limit, cursorId);
    }

    @CacheEvict(value = "profiles", allEntries = true)
    @Transactional
    public Profile update(long id, UpdateProfileRequest req) {
        profiles.update(id, req.firstName(), req.lastName(), req.email(), req.phone(), req.birthDate());
        var p = getByIdFresh(id);
        sagaAudit.audit(id, p.authUserId(), "PROFILE_UPDATE", req.toString());
        return p;
    }

    @CacheEvict(value = "profiles", allEntries = true)
    @Transactional
    public void deactivate(long id) {
        var p = profiles.findById(id).orElseThrow();
        profiles.setActive(id, false);
        sagaAudit.audit(id, p.authUserId(), "DEACTIVATE", "admin action");
        try {
            authClient.disable(p.authUserId());
        } catch (Exception ignored) {
        }
    }

    @CacheEvict(value = "profiles", allEntries = true)
    @Transactional
    public void activate(long id) {
        var p = profiles.findById(id).orElseThrow();
        profiles.setActive(id, true);
        sagaAudit.audit(id, p.authUserId(), "ACTIVATE", "admin action");
        try {
            authClient.enable(p.authUserId());
        } catch (Exception ignored) {
        }
    }

    @CacheEvict(value = "profiles", allEntries = true)
    @Transactional
    public void delete(long id) {
        var p = profiles.findById(id).orElseThrow();
        profiles.deleteById(id);
        sagaAudit.audit(null, p.authUserId(), "PROFILE_DELETE", "id=" + id);
        try {
            authClient.delete(p.authUserId());
        } catch (Exception ignored) {
        }
    }

    private Profile getByIdFresh(long id) {
        return profiles.findById(id).orElseThrow();
    }
}
