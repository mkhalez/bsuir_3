package com.shoplab.auth.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistrationEvent(UUID id, Long userId, String payload, String status,
                                int attemptCount, OffsetDateTime nextAttemptAt,
                                OffsetDateTime sendingStartedAt) {
}
