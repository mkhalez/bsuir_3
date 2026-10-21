package com.shoplab.auth.repo;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class UserJdbcRepositoryTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    private UserJdbcRepository repo;
    private JdbcTemplate jdbc;

    @BeforeEach
    void init() {
        DataSource ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS user_roles, users, roles");
        jdbc.execute("CREATE TABLE roles (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name VARCHAR(50) UNIQUE NOT NULL)");
        jdbc.execute("CREATE TABLE users (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, username VARCHAR(100) UNIQUE NOT NULL, password_hash TEXT NOT NULL, active BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ DEFAULT now(), updated_at TIMESTAMPTZ DEFAULT now())");
        jdbc.execute("CREATE TABLE user_roles (user_id BIGINT REFERENCES users(id) ON DELETE CASCADE, role_id BIGINT REFERENCES roles(id), PRIMARY KEY (user_id, role_id))");
        jdbc.execute("INSERT INTO roles(name) VALUES ('ADMIN'), ('USER')");
        repo = new UserJdbcRepository(jdbc);
    }

    @Test
    void insertFindUpdateDelete() {
        long id = repo.insert("alice@shop.by", "HASH", false);
        assertTrue(id > 0);
        assertTrue(repo.existsByUsername("alice@shop.by"));

        repo.assignRole(id, "USER");
        List<String> roles = repo.findRolesByUserId(id);
        assertEquals(List.of("USER"), roles);

        var u = repo.findByUsername("alice@shop.by").orElseThrow();
        assertFalse(u.active());

        repo.setActive(id, true);
        assertTrue(repo.findById(id).orElseThrow().active());

        assertEquals(1, repo.count());
        assertEquals(1, repo.findAll(10, null).size());

        repo.deleteById(id);
        assertTrue(repo.findById(id).isEmpty());
        assertEquals(0, repo.count());
    }
}
