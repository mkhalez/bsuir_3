package com.shoplab.auth.repo;

import com.shoplab.auth.domain.AuthUser;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findById(long id);
    boolean existsByUsername(String username);
    long insert(String username, String passwordHash, boolean active);
    void assignRole(long userId, String roleName);
    List<String> findRolesByUserId(long userId);
    void setActive(long userId, boolean active);
    void deleteById(long userId);
    List<AuthUser> findAll(int limit, Long cursorId);
    long count();
    Optional<Long> findRoleIdByName(String name);
}
