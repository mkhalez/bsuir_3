package com.shoplab.order.domain;

import java.math.BigDecimal;

public record OrderItem(Long id, Long orderId, Long productId, String productName,
                        int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}
