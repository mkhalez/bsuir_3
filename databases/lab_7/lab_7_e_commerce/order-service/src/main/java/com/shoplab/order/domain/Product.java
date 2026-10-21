package com.shoplab.order.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Product(Long id, Long categoryId, String categoryName, String sku,
                      String name, BigDecimal price, int stock, boolean active,
                      OffsetDateTime createdAt) {
}
