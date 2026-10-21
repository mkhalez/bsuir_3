package com.shoplab.order.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record Order(Long id, Long buyerProfileId, String buyerEmail, String status,
                    BigDecimal total, boolean deleted, OffsetDateTime createdAt,
                    List<OrderItem> items) {
}
