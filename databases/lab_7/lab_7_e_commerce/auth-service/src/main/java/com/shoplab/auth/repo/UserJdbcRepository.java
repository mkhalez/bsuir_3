package com.shoplab.auth.repo;

import com.shoplab.auth.domain.AuthUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepository implements UserRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<AuthUser> USER_MAPPER = (ResultSet rs, int n) -> new AuthUser(
            rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
            rs.getBoolean("active"), List.of(), rs.getObject("created_at", java.time.OffsetDateTime.class));

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        try {
            AuthUser u = jdbc.queryForObject("SELECT id, username, password_hash, active, created_at FROM users WHERE username = ?", USER_MAPPER, username);
            return Optional.of(withRoles(u));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthUser> findById(long id) {
        try {
            AuthUser u = jdbc.queryForObject("SELECT id, username, password_hash, active, created_at FROM users WHERE id = ?", USER_MAPPER, id);
            return Optional.of(withRoles(u));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", Boolean.class, username);
        return Boolean.TRUE.equals(b);
    }

    @Override
    public long insert(String username, String passwordHash, boolean active) {
        Long id = jdbc.queryForObject(
                "INSERT INTO users(username, password_hash, active) VALUES (?,?,?) RETURNING id",
                Long.class, username, passwordHash, active);
        return Objects.requireNonNull(id);
    }

    @Override
    public void assignRole(long userId, String roleName) {
        jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE name = ? ON CONFLICT DO NOTHING", userId, roleName);
    }

    @Override
    public List<String> findRolesByUserId(long userId) {
        return jdbc.queryForList("SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = ? ORDER BY r.name", String.class, userId);
    }

    @Override
    public void setActive(long userId, boolean active) {
        jdbc.update("UPDATE users SET active = ? WHERE id = ?", active, userId);
    }

    @Override
    public void deleteById(long userId) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Override
    public List<AuthUser> findAll(int limit, Long cursorId) {
        return jdbc.query("SELECT id, username, password_hash, active, created_at FROM users WHERE (?::BIGINT IS NULL OR id > ?) ORDER BY id LIMIT ?", USER_MAPPER, cursorId, cursorId, limit);
    }

    @Override
    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return c == null ? 0 : c;
    }

    @Override
    public Optional<Long> findRoleIdByName(String name) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, name));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private AuthUser withRoles(AuthUser u) {
        return new AuthUser(u.id(), u.username(), u.passwordHash(), u.active(), findRolesByUserId(u.id()), u.createdAt());
    }
}
