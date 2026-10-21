package com.shoplab.order.repo;

import com.shoplab.order.domain.Product;
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
public class ProductJdbcRepository {

    private final JdbcTemplate jdbc;

    private static final String BASE = """
            SELECT p.id, p.category_id, c.name AS category_name, p.sku, p.name, p.price, p.stock, p.active, p.created_at
            FROM products p JOIN categories c ON c.id = p.category_id""";

    private static Product map(ResultSet rs, int n) throws java.sql.SQLException {
        return new Product(rs.getLong("id"), rs.getLong("category_id"), rs.getString("category_name"),
                rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price"),
                rs.getInt("stock"), rs.getBoolean("active"), rs.getObject("created_at", OffsetDateTime.class));
    }

    public long insert(long categoryId, String sku, String name, BigDecimal price, int stock) {
        Long id = jdbc.queryForObject(
                "INSERT INTO products(category_id, sku, name, price, stock) VALUES (?,?,?,?,?) RETURNING id",
                Long.class, categoryId, sku, name, price, stock);
        return Objects.requireNonNull(id);
    }

    public Optional<Product> findById(long id) {
        try {
            return Optional.of(jdbc.queryForObject(BASE + " WHERE p.id = ?", ProductJdbcRepository::map, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int update(long id, String name, BigDecimal price, Integer stock, Boolean active) {
        return jdbc.update("UPDATE products SET name = COALESCE(?, name), price = COALESCE(?, price), stock = COALESCE(?, stock), active = COALESCE(?, active) WHERE id = ?",
                name, price, stock, active, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM products WHERE id = ?", id);
    }

    public List<Product> catalog(Long categoryId, BigDecimal maxPrice, String nameLike, int limit, BigDecimal cursorPrice, Long cursorId) {
        return jdbc.query(BASE + " WHERE p.active = TRUE AND (?::BIGINT IS NULL OR p.category_id = ?) AND (?::NUMERIC IS NULL OR p.price <= ?) AND (?::TEXT IS NULL OR p.name ILIKE '%' || ? || '%') AND (?::NUMERIC IS NULL OR ?::BIGINT IS NULL OR (p.price, p.id) > (?, ?)) ORDER BY p.price, p.id LIMIT ?",
                ProductJdbcRepository::map, categoryId, categoryId, maxPrice, maxPrice, nameLike, nameLike, cursorPrice, cursorId, cursorPrice, cursorId, limit);
    }

    public Optional<Integer> stockOf(long productId) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, productId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean decreaseStock(long productId, int qty) {
        return jdbc.update("UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?", qty, productId, qty) == 1;
    }

    public void increaseStock(long productId, int qty) {
        jdbc.update("UPDATE products SET stock = stock + ? WHERE id = ?", qty, productId);
    }

    public List<Map<String, Object>> topSelling(int limit) {
        return jdbc.queryForList("""
                SELECT p.id, p.name, p.sku, SUM(oi.quantity) AS sold_qty, SUM(oi.line_total) AS revenue
                FROM order_items oi JOIN products p ON p.id = oi.product_id
                GROUP BY p.id, p.name, p.sku ORDER BY sold_qty DESC LIMIT ?""", limit);
    }

    public List<Map<String, Object>> categoriesWithCounts() {
        return jdbc.queryForList("SELECT c.id, c.name, COUNT(p.id) AS products FROM categories c LEFT JOIN products p ON p.category_id = c.id GROUP BY c.id, c.name ORDER BY c.name");
    }

    public long ensureCategory(String name) {
        jdbc.update("INSERT INTO categories(name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
        return jdbc.queryForObject("SELECT id FROM categories WHERE name = ?", Long.class, name);
    }
}
