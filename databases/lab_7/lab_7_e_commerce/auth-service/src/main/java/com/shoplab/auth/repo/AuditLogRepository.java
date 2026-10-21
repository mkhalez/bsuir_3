package com.shoplab.auth.repo;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditLogRepository {

    private final JdbcTemplate jdbc;

    public void log(Long userId, String username, String action, String details, String ip) {
        jdbc.update("INSERT INTO auth_audit_log(user_id, username, action, details, ip_address) VALUES (?,?,?,?,?)",
                userId, username, action, details, ip);
    }

    public List<Map<String, Object>> lastByUser(Long userId, int limit) {
        return jdbc.queryForList("SELECT id, username, action, details, ip_address, created_at FROM auth_audit_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ?", userId, limit);
    }

    public List<Map<String, Object>> byActionSince(String action, java.time.OffsetDateTime since, int limit) {
        return jdbc.queryForList("SELECT id, username, action, details, created_at FROM auth_audit_log WHERE action = ? AND created_at >= ? ORDER BY created_at DESC LIMIT ?", action, since, limit);
    }

    public List<Map<String, Object>> topActiveUsers(int limit) {
        return jdbc.queryForList("SELECT username, COUNT(*) AS actions FROM auth_audit_log GROUP BY username ORDER BY actions DESC LIMIT ?", limit);
    }
}
