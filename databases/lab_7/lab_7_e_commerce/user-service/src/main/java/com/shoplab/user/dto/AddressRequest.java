package com.shoplab.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(@NotBlank String city, @NotBlank String street,
                             @NotBlank String zip, boolean isDefault) {
}
