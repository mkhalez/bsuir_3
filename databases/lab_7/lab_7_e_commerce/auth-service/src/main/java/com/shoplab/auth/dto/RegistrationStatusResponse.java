package com.shoplab.auth.dto;

import java.util.UUID;

public record RegistrationStatusResponse(UUID registrationId, String status, String message) {
}
