package com.shoplab.order.client;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BuyerProfile(Long id, Long authUserId, String firstName, String lastName,
                           String email, String phone, LocalDate birthDate, boolean active,
                           OffsetDateTime createdAt) {
}
