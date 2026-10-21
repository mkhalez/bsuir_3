package com.shoplab.user.saga;

import com.shoplab.contracts.avro.RegistrationStatus;
import com.shoplab.contracts.avro.UserRegistrationRequest;
import com.shoplab.contracts.avro.UserRegistrationResult;
import com.shoplab.user.config.KafkaTopicsProperties;
import com.shoplab.user.repo.ProfileJdbcRepository;
import com.shoplab.user.repo.SagaAuditRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationSagaParticipant {

    private final ProfileJdbcRepository profiles;
    private final SagaAuditRepository saga;
    private final KafkaTemplate<String, Object> kafka;
    private final KafkaTopicsProperties topics;

    @KafkaListener(topics = "${kafka.topics.registration}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onRegistration(UserRegistrationRequest msg, Acknowledgment ack) {
        UUID eventId = UUID.fromString(msg.getEventId().toString());
        String email = msg.getEmail().toString();
        try {
            if (saga.inboxExists(eventId)) {
                ack.acknowledge();
                return;
            }
            saga.inboxSave(eventId, msg.toString());
            if (profiles.existsByEmail(email)) {
                saga.outboxSave(eventId, "FAIL", "Email already used: " + email);
            } else {
                long pid = profiles.insert(msg.getAuthUserId(), msg.getFirstName().toString(),
                        msg.getLastName().toString(), email, null, msg.getBirthDate());
                saga.audit(pid, msg.getAuthUserId(), "SAGA_PROFILE_CREATED", "event=" + eventId);
                saga.outboxSave(eventId, "SUCCESS", "profile=" + pid);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Saga processing failed, retry: {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelayString = "${kafka.sender.fixed-delay-ms:2000}")
    public void sendResults() {
        for (var row : saga.pendingResults(10)) {
            UUID id = (UUID) row.get("id");
            try {
                UserRegistrationResult result = UserRegistrationResult.newBuilder()
                        .setEventId(id.toString())
                        .setStatus(RegistrationStatus.valueOf(((String) row.get("status")).toUpperCase()))
                        .setMessage((String) row.get("message"))
                        .build();
                kafka.send(topics.registrationResult(), id.toString(), result).get();
                saga.markSent(id);
            } catch (Exception e) {
                saga.markRetriable(id);
            }
        }
    }
}
