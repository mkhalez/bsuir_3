package com.shoplab.user.repo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SagaAuditRepository {

    private final JdbcTemplate jdbc;

    public boolean inboxExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM registration_inbox WHERE id = ?)", Boolean.class, id));
    }

    public void inboxSave(UUID id, String payload) {
        jdbc.update("INSERT INTO registration_inbox(id, payload, status) VALUES (?,?, 'RECEIVED') ON CONFLICT (id) DO NOTHING", id, payload);
    }

    public void outboxSave(UUID id, String status, String message) {
        jdbc.update("INSERT INTO registration_outbox(id, status, message, sent) VALUES (?,?,?, FALSE) ON CONFLICT (id) DO NOTHING", id, status, message);
    }

    public List<Map<String, Object>> pendingResults(int limit) {
        return jdbc.queryForList("SELECT id, status, message, attempt_count FROM registration_outbox WHERE sent = FALSE AND next_attempt_at < now() ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED", limit);
    }

    public void markSent(UUID id) {
        jdbc.update("UPDATE registration_outbox SET sent = TRUE WHERE id = ?", id);
    }

    public void markRetriable(UUID id) {
        jdbc.update("UPDATE registration_outbox SET attempt_count = attempt_count + 1, next_attempt_at = now() + INTERVAL '5 seconds' WHERE id = ?", id);
    }

    public void audit(Long profileId, Long authUserId, String action, String details) {
        jdbc.update("INSERT INTO user_audit_log(profile_id, auth_user_id, action, details) VALUES (?,?,?,?)", profileId, authUserId, action, details);
    }

    public List<Map<String, Object>> lastAudit(Long profileId, int limit) {
        return jdbc.queryForList("SELECT id, action, details, created_at FROM user_audit_log WHERE profile_id = ? ORDER BY created_at DESC LIMIT ?", profileId, limit);
    }
}
