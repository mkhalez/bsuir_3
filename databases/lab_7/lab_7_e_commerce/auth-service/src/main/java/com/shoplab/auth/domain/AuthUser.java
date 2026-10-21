package com.shoplab.auth.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AuthUser(Long id, String username, String passwordHash, boolean active,
                       List<String> roles, OffsetDateTime createdAt) {
}
