package com.shoplab.auth.repo;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegistrationResultRepository {

    private final JdbcTemplate jdbc;

    public boolean existsById(UUID id) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM registration_result WHERE id = ?)", Boolean.class, id);
        return Boolean.TRUE.equals(b);
    }

    public void save(UUID id, String status, String message) {
        jdbc.update("INSERT INTO registration_result(id, status, message) VALUES (?,?,?) ON CONFLICT (id) DO NOTHING", id, status, message);
    }
}
