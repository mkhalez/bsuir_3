package com.shoplab.user.repo;

import com.shoplab.user.domain.Address;
import java.sql.ResultSet;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AddressJdbcRepository {

    private final JdbcTemplate jdbc;

    private static Address map(ResultSet rs, int n) throws java.sql.SQLException {
        return new Address(rs.getLong("id"), rs.getLong("profile_id"), rs.getString("city"),
                rs.getString("street"), rs.getString("zip"), rs.getBoolean("is_default"));
    }

    public long insert(long profileId, String city, String street, String zip, boolean isDefault) {
        Long id = jdbc.queryForObject(
                "INSERT INTO addresses(profile_id, city, street, zip, is_default) VALUES (?,?,?,?,?) RETURNING id",
                Long.class, profileId, city, street, zip, isDefault);
        return Objects.requireNonNull(id);
    }

    public Optional<Address> findById(long id) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT id, profile_id, city, street, zip, is_default FROM addresses WHERE id = ?", AddressJdbcRepository::map, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Address> findByProfileId(long profileId) {
        return jdbc.query("SELECT id, profile_id, city, street, zip, is_default FROM addresses WHERE profile_id = ? ORDER BY id", AddressJdbcRepository::map, profileId);
    }

    public int update(long id, String city, String street, String zip, Boolean isDefault) {
        return jdbc.update("UPDATE addresses SET city = COALESCE(?, city), street = COALESCE(?, street), zip = COALESCE(?, zip), is_default = COALESCE(?, is_default) WHERE id = ?",
                city, street, zip, isDefault, id);
    }

    public void deleteById(long id) {
        jdbc.update("DELETE FROM addresses WHERE id = ?", id);
    }

    public Optional<Long> ownerProfileId(long addressId) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT profile_id FROM addresses WHERE id = ?", Long.class, addressId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
