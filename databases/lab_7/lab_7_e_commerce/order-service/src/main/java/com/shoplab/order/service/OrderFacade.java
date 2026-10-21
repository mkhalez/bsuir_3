package com.shoplab.order.service;

import com.shoplab.order.client.BuyerProfile;
import com.shoplab.order.client.UserServiceClient;
import com.shoplab.order.domain.Order;
import com.shoplab.order.dto.CreateOrderRequest;
import com.shoplab.order.repo.OrderJdbcRepository;
import com.shoplab.order.repo.ProductJdbcRepository;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final UserServiceClient userClient;
    private final OrderJdbcRepository orders;
    private final ProductJdbcRepository products;

    @Transactional
    public Order create(CreateOrderRequest req, String actor) {
        BuyerProfile buyer = userClient.findByEmail(req.buyerEmail());
        if (!buyer.active()) throw new IllegalStateException("Buyer profile is inactive");

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, BigDecimal> prices = new HashMap<>();
        for (var line : req.items()) {
            var p = products.findById(line.productId()).orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.productId()));
            if (!p.active()) throw new IllegalStateException("Product inactive: " + p.sku());
            prices.put(p.id(), p.price());
            total = total.add(p.price().multiply(BigDecimal.valueOf(line.quantity())));
        }

        long orderId = orders.insertOrder(buyer.id(), buyer.email(), total);
        for (var line : req.items()) {
            boolean ok = products.decreaseStock(line.productId(), line.quantity());
            if (!ok) throw new IllegalStateException("Not enough stock for product " + line.productId());
            orders.insertItem(orderId, line.productId(), line.quantity(), prices.get(line.productId()));
        }
        orders.logStatus(orderId, null, "CREATED", actor);
        orders.audit(orderId, "ORDER_CREATED", "buyer=" + buyer.email() + " total=" + total, actor);
        return orders.findDetailedById(orderId).orElseThrow();
    }

    @Transactional
    public Order changeStatus(long orderId, String newStatus, String actor, boolean admin) {
        List<String> allowed = List.of("CREATED", "PAID", "SHIPPED", "DELIVERED", "CANCELLED");
        if (!allowed.contains(newStatus)) throw new IllegalArgumentException("Bad status: " + newStatus);
        if (!admin && !(newStatus.equals("CANCELLED"))) throw new SecurityException("Only ADMIN can set " + newStatus);
        String old = orders.statusOf(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (newStatus.equals("CANCELLED") && !old.equals("CANCELLED")) {
            var full = orders.findDetailedById(orderId).orElseThrow();
            for (var item : full.items()) products.increaseStock(item.productId(), item.quantity());
        }
        orders.updateStatus(orderId, newStatus);
        orders.logStatus(orderId, old, newStatus, actor);
        orders.audit(orderId, "STATUS_" + newStatus, old + "->" + newStatus, actor);
        return orders.findDetailedById(orderId).orElseThrow();
    }

    @Transactional
    public void delete(long orderId, String actor) {
        orders.softDelete(orderId);
        orders.audit(orderId, "ORDER_DELETED", "soft delete", actor);
    }
}
