package com.shoplab.order.repo;

import com.shoplab.order.domain.Order;
import com.shoplab.order.domain.OrderItem;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderJdbcRepository {

    private final JdbcTemplate jdbc;

    public long insertOrder(long buyerProfileId, String buyerEmail, BigDecimal total) {
        Long id = jdbc.queryForObject(
                "INSERT INTO orders(buyer_profile_id, buyer_email, status, total) VALUES (?,?, 'CREATED', ?) RETURNING id",
                Long.class, buyerProfileId, buyerEmail, total);
        return Objects.requireNonNull(id);
    }

    public void insertItem(long orderId, long productId, int qty, BigDecimal unitPrice) {
        jdbc.update("INSERT INTO order_items(order_id, product_id, quantity, unit_price, line_total) VALUES (?,?,?,?,?)",
                orderId, productId, qty, unitPrice, unitPrice.multiply(BigDecimal.valueOf(qty)));
    }

    public Optional<Order> findDetailedById(long id) {
        try {
            var head = jdbc.queryForMap("SELECT id, buyer_profile_id, buyer_email, status, total, deleted, created_at FROM orders WHERE id = ? AND deleted = FALSE", id);
            List<OrderItem> items = jdbc.query("""
                    SELECT oi.id, oi.order_id, oi.product_id, p.name AS product_name, oi.quantity, oi.unit_price, oi.line_total
                    FROM order_items oi JOIN products p ON p.id = oi.product_id WHERE oi.order_id = ? ORDER BY oi.id""",
                    (ResultSet rs, int n) -> new OrderItem(rs.getLong("id"), rs.getLong("order_id"), rs.getLong("product_id"),
                            rs.getString("product_name"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"), rs.getBigDecimal("line_total")), id);
            Object created = head.get("created_at");
            OffsetDateTime createdAt = created instanceof OffsetDateTime odt ? odt
                    : ((java.sql.Timestamp) Objects.requireNonNull(created)).toInstant().atOffset(java.time.ZoneOffset.UTC);
            return Optional.of(new Order((Long) head.get("id"), (Long) head.get("buyer_profile_id"), (String) head.get("buyer_email"),
                    (String) head.get("status"), (BigDecimal) head.get("total"), (Boolean) head.get("deleted"),
                    createdAt, items));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int updateStatus(long orderId, String newStatus) {
        return jdbc.update("UPDATE orders SET status = ? WHERE id = ? AND deleted = FALSE", newStatus, orderId);
    }

    public Optional<String> statusOf(long orderId) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void softDelete(long orderId) {
        jdbc.update("UPDATE orders SET deleted = TRUE WHERE id = ?", orderId);
    }

    public List<Map<String, Object>> list(String status, OffsetDateTime from, OffsetDateTime to, int limit, OffsetDateTime cursorCreatedAt, Long cursorId) {
        java.sql.Timestamp cursorTs = cursorCreatedAt == null ? null : java.sql.Timestamp.from(cursorCreatedAt.toInstant());
        return jdbc.queryForList("""
                SELECT id, buyer_profile_id, buyer_email, status, total, created_at FROM orders
                WHERE deleted = FALSE AND (?::TEXT IS NULL OR status = ?)
                  AND (?::TIMESTAMPTZ IS NULL OR created_at >= ?) AND (?::TIMESTAMPTZ IS NULL OR created_at <= ?)
                  AND (?::TIMESTAMPTZ IS NULL OR ?::BIGINT IS NULL OR (created_at, id) < (?, ?))
                ORDER BY created_at DESC, id DESC LIMIT ?""",
                status, status,
                from == null ? null : java.sql.Timestamp.from(from.toInstant()), from == null ? null : java.sql.Timestamp.from(from.toInstant()),
                to == null ? null : java.sql.Timestamp.from(to.toInstant()), to == null ? null : java.sql.Timestamp.from(to.toInstant()),
                cursorTs, cursorId, cursorTs, cursorId,
                limit);
    }

    public List<Map<String, Object>> byBuyer(long buyerProfileId, int limit, OffsetDateTime cursorCreatedAt, Long cursorId) {
        java.sql.Timestamp cursorTs = cursorCreatedAt == null ? null : java.sql.Timestamp.from(cursorCreatedAt.toInstant());
        return jdbc.queryForList("SELECT id, status, total, created_at FROM orders WHERE buyer_profile_id = ? AND deleted = FALSE AND (?::TIMESTAMPTZ IS NULL OR ?::BIGINT IS NULL OR (created_at, id) < (?, ?)) ORDER BY created_at DESC, id DESC LIMIT ?",
                buyerProfileId, cursorTs, cursorId, cursorTs, cursorId, limit);
    }

    public List<Map<String, Object>> revenueByStatus() {
        return jdbc.queryForList("SELECT status, COUNT(*) AS orders, COALESCE(SUM(total),0) AS revenue FROM orders WHERE deleted = FALSE GROUP BY status ORDER BY status");
    }

    public List<Map<String, Object>> avgCheckByDay(int days) {
        return jdbc.queryForList("SELECT DATE_TRUNC('day', created_at)::date AS day, COUNT(*) AS orders, AVG(total) AS avg_check FROM orders WHERE created_at >= now() - (? || ' days')::interval AND deleted = FALSE GROUP BY 1 ORDER BY 1", String.valueOf(days));
    }

    public void logStatus(long orderId, String oldStatus, String newStatus, String actor) {
        jdbc.update("INSERT INTO order_status_log(order_id, old_status, new_status, changed_by) VALUES (?,?,?,?)", orderId, oldStatus, newStatus, actor);
    }

    public void audit(Long orderId, String action, String details, String actor) {
        jdbc.update("INSERT INTO order_audit_log(order_id, action, details, actor) VALUES (?,?,?,?)", orderId, action, details, actor);
    }

    public List<Map<String, Object>> statusHistory(long orderId) {
        return jdbc.queryForList("SELECT old_status, new_status, changed_by, created_at FROM order_status_log WHERE order_id = ? ORDER BY created_at", orderId);
    }
}
