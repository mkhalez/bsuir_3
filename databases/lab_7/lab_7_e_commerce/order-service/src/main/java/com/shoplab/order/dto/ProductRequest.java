package com.shoplab.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(@NotNull Long categoryId, @NotBlank String sku,
                             @NotBlank String name, @DecimalMin("0.0") BigDecimal price,
                             @Min(0) int stock) {
}
