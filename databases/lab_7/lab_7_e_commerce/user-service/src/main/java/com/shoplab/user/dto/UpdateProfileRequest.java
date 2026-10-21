package com.shoplab.user.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UpdateProfileRequest(String firstName, String lastName,
                                   @Email String email, String phone, LocalDate birthDate) {
}
