package com.shoplab.order.repo;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ProductJdbcRepositoryTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    private ProductJdbcRepository repo;

    @BeforeEach
    void init() {
        DataSource ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS order_items, products, categories");
        jdbc.execute("CREATE TABLE categories (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name VARCHAR(100) UNIQUE NOT NULL, description TEXT)");
        jdbc.execute("CREATE TABLE products (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, category_id BIGINT REFERENCES categories(id), sku VARCHAR(50) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, price NUMERIC(12,2) NOT NULL, stock INT DEFAULT 0, active BOOLEAN DEFAULT TRUE, created_at TIMESTAMPTZ DEFAULT now(), updated_at TIMESTAMPTZ DEFAULT now())");
        repo = new ProductJdbcRepository(jdbc);
    }

    @Test
    void insertCatalogAndStock() {
        long cat = repo.ensureCategory("Audio");
        long id = repo.insert(cat, "TEST-1", "Test speaker", new BigDecimal("100.00"), 10);
        assertTrue(id > 0);
        assertTrue(repo.findById(id).isPresent());
        assertEquals(1, repo.catalog(cat, null, null, 10, null, null).size());
        assertTrue(repo.decreaseStock(id, 3));
        assertEquals(7, repo.stockOf(id).orElseThrow());
        assertFalse(repo.decreaseStock(id, 100));
    }
}
