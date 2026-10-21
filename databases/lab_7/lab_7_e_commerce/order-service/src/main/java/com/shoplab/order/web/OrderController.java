package com.shoplab.order.web;

import com.shoplab.order.domain.Order;
import com.shoplab.order.dto.CreateOrderRequest;
import com.shoplab.order.dto.UpdateOrderStatusRequest;
import com.shoplab.order.repo.OrderJdbcRepository;
import com.shoplab.order.service.OrderFacade;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade facade;
    private final OrderJdbcRepository orders;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest req, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(facade.create(req, jwt.getSubject()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Order> byId(@PathVariable long id) {
        return ResponseEntity.ok(orders.findDetailedById(id).orElseThrow(() -> new IllegalArgumentException("Order not found")));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorId) {
        return ResponseEntity.ok(orders.list(status, from, to, Math.min(limit, 100), cursorCreatedAt, cursorId));
    }

    @GetMapping("/by-buyer/{buyerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> byBuyer(@PathVariable long buyerId,
                                                             @RequestParam(defaultValue = "20") int limit,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime cursorCreatedAt,
                                                             @RequestParam(required = false) Long cursorId) {
        return ResponseEntity.ok(orders.byBuyer(buyerId, limit, cursorCreatedAt, cursorId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Order> status(@PathVariable long id, @Valid @RequestBody UpdateOrderStatusRequest req, @AuthenticationPrincipal Jwt jwt) {
        boolean admin = jwt.getClaimAsStringList("roles") != null && jwt.getClaimAsStringList("roles").contains("ADMIN");
        return ResponseEntity.ok(facade.changeStatus(id, req.status(), jwt.getSubject(), admin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        facade.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable long id) {
        return ResponseEntity.ok(orders.statusHistory(id));
    }

    @GetMapping("/stats/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> revenue() {
        return ResponseEntity.ok(orders.revenueByStatus());
    }
}
