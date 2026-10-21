package com.shoplab.user.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Profile(Long id, Long authUserId, String firstName, String lastName,
                      String email, String phone, LocalDate birthDate, boolean active,
                      OffsetDateTime createdAt) implements Serializable {
}
