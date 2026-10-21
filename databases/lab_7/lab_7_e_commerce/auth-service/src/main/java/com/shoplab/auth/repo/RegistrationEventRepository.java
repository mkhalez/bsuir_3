package com.shoplab.auth.repo;

import tools.jackson.databind.ObjectMapper;
import com.shoplab.auth.domain.RegistrationEvent;
import com.shoplab.auth.saga.RegistrationMessage;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegistrationEventRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Value("${kafka.sender.max-attempts:3}")
    private int maxAttempts = 3;
    @Value("${kafka.sender.batch-limit:10}")
    private int batchLimit = 10;

    public void save(UUID id, Long userId, RegistrationMessage payload) {
        jdbc.update("INSERT INTO registration_event(id, user_id, payload, status, attempt_count, next_attempt_at) VALUES (?,?,?::text,'CREATED',0,now())",
                id, userId, toJson(payload));
    }

    public List<RegistrationEvent> findEventsToProcess() {
        return jdbc.query("""
                SELECT id, user_id, payload, status, attempt_count, next_attempt_at, sending_started_at
                FROM registration_event
                WHERE attempt_count <= ? AND ((status='CREATED' AND next_attempt_at < now())
                   OR (status='PRE_SENDING' AND (sending_started_at IS NULL OR sending_started_at <= now() - INTERVAL '2 minutes')))
                ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED
                """, (ResultSet rs, int n) -> new RegistrationEvent(
                        (UUID) rs.getObject("id"), (Long) rs.getObject("user_id"), rs.getString("payload"),
                        rs.getString("status"), rs.getInt("attempt_count"),
                        rs.getObject("next_attempt_at", OffsetDateTime.class),
                        rs.getObject("sending_started_at", OffsetDateTime.class)),
                maxAttempts, batchLimit);
    }

    public void markPreSending(UUID id) {
        jdbc.update("UPDATE registration_event SET status='PRE_SENDING', attempt_count=attempt_count+1, sending_started_at=now(), updated_at=now() WHERE id=?", id);
    }

    public void markSending(UUID id) {
        jdbc.update("UPDATE registration_event SET status='SENDING', updated_at=now() WHERE id=?", id);
    }

    public void markRetriable(UUID id, int attempt) {
        int backoffSec = (int) Math.pow(2, attempt);
        jdbc.update("UPDATE registration_event SET status='CREATED', next_attempt_at=now() + (? || ' seconds')::interval, updated_at=now() WHERE id=?", String.valueOf(backoffSec), id);
    }

    public void markFailed(UUID id) {
        jdbc.update("UPDATE registration_event SET status='FAIL_TO_SEND', updated_at=now() WHERE id=?", id);
    }

    public void markSuccess(UUID id) {
        jdbc.update("UPDATE registration_event SET status='SUCCESS', updated_at=now() WHERE id=?", id);
    }

    public void markFail(UUID id) {
        jdbc.update("UPDATE registration_event SET status='FAIL', user_id=NULL, updated_at=now() WHERE id=?", id);
    }

    public Optional<RegistrationEvent> findById(UUID id) {
        try {
            RegistrationEvent e = jdbc.queryForObject("SELECT id, user_id, payload, status, attempt_count, next_attempt_at, sending_started_at FROM registration_event WHERE id=?",
                    (ResultSet rs, int n) -> new RegistrationEvent((UUID) rs.getObject("id"), (Long) rs.getObject("user_id"),
                            rs.getString("payload"), rs.getString("status"), rs.getInt("attempt_count"),
                            rs.getObject("next_attempt_at", OffsetDateTime.class), rs.getObject("sending_started_at", OffsetDateTime.class)), id);
            return Optional.of(e);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<java.util.Map<String, Object>> statusStats() {
        return jdbc.queryForList("SELECT status, COUNT(*) AS cnt FROM registration_event GROUP BY status ORDER BY status");
    }

    @SneakyThrows
    private String toJson(RegistrationMessage m) {
        return objectMapper.writeValueAsString(m);
    }

    public RegistrationMessage fromJson(String json) {
        try {
            return objectMapper.readValue(json, RegistrationMessage.class);
        } catch (Exception e) {
            throw new IllegalStateException("Bad registration payload", e);
        }
    }
}
