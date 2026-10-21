package com.shoplab.user.repo;

import com.shoplab.user.domain.Profile;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileJdbcRepository {

    private final JdbcTemplate jdbc;

    private static final String BASE = "SELECT id, auth_user_id, first_name, last_name, email, phone, birth_date, active, created_at FROM profiles";

    private static Profile map(ResultSet rs, int n) throws java.sql.SQLException {
        return new Profile(rs.getLong("id"), rs.getLong("auth_user_id"), rs.getString("first_name"),
                rs.getString("last_name"), rs.getString("email"), rs.getString("phone"),
                rs.getObject("birth_date", java.time.LocalDate.class), rs.getBoolean("active"),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    public long insert(long authUserId, String firstName, String lastName, String email, String phone, java.time.LocalDate birthDate) {
        Long id = jdbc.queryForObject(
                "INSERT INTO profiles(auth_user_id, first_name, last_name, email, phone, birth_date) VALUES (?,?,?,?,?,?) RETURNING id",
                Long.class, authUserId, firstName, lastName, email, phone, Date.valueOf(birthDate));
        return Objects.requireNonNull(id);
    }

    public Optional<Profile> findById(long id) {
        try {
            return Optional.of(jdbc.queryForObject(BASE + " WHERE id = ?", ProfileJdbcRepository::map, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Profile> findByEmail(String email) {
        try {
            return Optional.of(jdbc.queryForObject(BASE + " WHERE email = ?", ProfileJdbcRepository::map, email));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Profile> findByAuthUserId(long authUserId) {
        try {
            return Optional.of(jdbc.queryForObject(BASE + " WHERE auth_user_id = ?", ProfileJdbcRepository::map, authUserId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM profiles WHERE email = ?)", Boolean.class, email));
    }

    public boolean existsByAuthUserId(long authUserId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM profiles WHERE auth_user_id = ?)", Boolean.class, authUserId));
    }

    public int update(long id, String firstName, String lastName, String email, String phone, java.time.LocalDate birthDate) {
        return jdbc.update("UPDATE profiles SET first_name = COALESCE(?, first_name), last_name = COALESCE(?, last_name), email = COALESCE(?, email), phone = COALESCE(?, phone), birth_date = COALESCE(?, birth_date) WHERE id = ?",
                firstName, lastName, email, phone, birthDate == null ? null : Date.valueOf(birthDate), id);
    }

    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE profiles SET active = ? WHERE id = ?", active, id);
    }

    public void deleteById(long id) {
        jdbc.update("DELETE FROM addresses WHERE profile_id = ?", id);
        jdbc.update("DELETE FROM profiles WHERE id = ?", id);
    }

    public List<Profile> search(String firstName, String lastName, int limit, Long cursorId) {
        return jdbc.query(BASE + " WHERE (?::TEXT IS NULL OR first_name ILIKE '%' || ? || '%') AND (?::TEXT IS NULL OR last_name ILIKE '%' || ? || '%') AND (?::BIGINT IS NULL OR id > ?) ORDER BY id LIMIT ?",
                ProfileJdbcRepository::map, firstName, firstName, lastName, lastName, cursorId, cursorId, limit);
    }

    public long countSearch(String firstName, String lastName) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM profiles WHERE (?::TEXT IS NULL OR first_name ILIKE '%' || ? || '%') AND (?::TEXT IS NULL OR last_name ILIKE '%' || ? || '%')",
                Long.class, firstName, firstName, lastName, lastName);
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> profilesWithAddressCount(int limit) {
        return jdbc.queryForList("SELECT p.id, p.email, p.first_name, p.last_name, COUNT(a.id) AS addresses FROM profiles p LEFT JOIN addresses a ON a.profile_id = p.id GROUP BY p.id ORDER BY p.id LIMIT ?", limit);
    }

    public List<Map<String, Object>> activeStats() {
        return jdbc.queryForList("SELECT active, COUNT(*) AS cnt FROM profiles GROUP BY active");
    }

    public Optional<Long> authIdByProfileId(long profileId) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT auth_user_id FROM profiles WHERE id = ?", Long.class, profileId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Long> authIdByEmail(String email) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT auth_user_id FROM profiles WHERE email = ?", Long.class, email));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
