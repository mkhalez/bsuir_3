package com.shoplab.user.service;

import com.shoplab.user.client.AuthInternalClient;
import com.shoplab.user.domain.Profile;
import com.shoplab.user.dto.UpdateProfileRequest;
import com.shoplab.user.repo.ProfileJdbcRepository;
import com.shoplab.user.repo.SagaAuditRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock ProfileJdbcRepository profiles;
    @Mock SagaAuditRepository sagaAudit;
    @Mock AuthInternalClient authClient;

    @InjectMocks ProfileService service;

    private Profile profile() {
        return new Profile(1L, 10L, "Ivan", "Petrov", "ivan@shop.by", "+37529", LocalDate.of(1998, 4, 12), true, OffsetDateTime.now());
    }

    @Test
    void updateWritesAudit() {
        Mockito.when(profiles.findById(1L)).thenReturn(Optional.of(profile()));
        var updated = service.update(1L, new UpdateProfileRequest("Ivan", "Petrov", null, null, null));
        assertEquals(1L, updated.id());
        Mockito.verify(profiles).update(eq(1L), eq("Ivan"), eq("Petrov"), isNull(), isNull(), isNull());
        Mockito.verify(sagaAudit).audit(eq(1L), eq(10L), eq("PROFILE_UPDATE"), any());
    }

    @Test
    void deactivateCallsAuthDisable() {
        Mockito.when(profiles.findById(1L)).thenReturn(Optional.of(profile()));
        service.deactivate(1L);
        Mockito.verify(profiles).setActive(1L, false);
        Mockito.verify(authClient).disable(10L);
    }
}
