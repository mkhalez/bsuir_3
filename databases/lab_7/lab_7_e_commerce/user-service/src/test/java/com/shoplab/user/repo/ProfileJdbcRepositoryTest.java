package com.shoplab.user.repo;

import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ProfileJdbcRepositoryTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    private ProfileJdbcRepository repo;

    @BeforeEach
    void init() {
        DataSource ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS addresses, profiles");
        jdbc.execute("CREATE TABLE profiles (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, auth_user_id BIGINT UNIQUE NOT NULL, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL, email VARCHAR(200) UNIQUE NOT NULL, phone VARCHAR(30), birth_date DATE NOT NULL, active BOOLEAN DEFAULT TRUE, created_at TIMESTAMPTZ DEFAULT now(), updated_at TIMESTAMPTZ DEFAULT now())");
        jdbc.execute("CREATE TABLE addresses (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE, city VARCHAR(100) NOT NULL, street VARCHAR(200) NOT NULL, zip VARCHAR(20) NOT NULL, is_default BOOLEAN DEFAULT FALSE, created_at TIMESTAMPTZ DEFAULT now())");
        repo = new ProfileJdbcRepository(jdbc);
    }

    @Test
    void crudAndSearch() {
        long id = repo.insert(100L, "Anna", "Sidorova", "anna@shop.by", "+37529", LocalDate.of(2000, 9, 3));
        assertTrue(id > 0);
        assertTrue(repo.findByEmail("anna@shop.by").isPresent());
        assertTrue(repo.findByAuthUserId(100L).isPresent());
        assertEquals(1, repo.search("Ann", null, 10, null).size());
        assertEquals(1, repo.countSearch("Ann", null));

        repo.setActive(id, false);
        assertFalse(repo.findById(id).orElseThrow().active());

        repo.deleteById(id);
        assertTrue(repo.findById(id).isEmpty());
    }
}
