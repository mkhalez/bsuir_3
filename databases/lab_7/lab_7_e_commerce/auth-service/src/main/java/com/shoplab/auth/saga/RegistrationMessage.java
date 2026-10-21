package com.shoplab.auth.saga;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrationMessage(UUID eventId, Long authUserId, String username,
                                  String firstName, String lastName, String email,
                                  LocalDate birthDate) {
}
