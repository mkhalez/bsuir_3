package com.shoplab.order.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(@NotBlank @Email String buyerEmail,
                                 @NotEmpty List<OrderLineRequest> items) {
    public record OrderLineRequest(@NotNull Long productId, @Min(1) int quantity) {
    }
}
