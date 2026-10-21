package com.shoplab.auth.saga;

import com.shoplab.auth.repo.RegistrationEventRepository;
import com.shoplab.auth.repo.RegistrationResultRepository;
import com.shoplab.auth.repo.UserRepository;
import com.shoplab.contracts.avro.RegistrationStatus;
import com.shoplab.contracts.avro.UserRegistrationResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationResultListener {

    private final RegistrationEventRepository events;
    private final RegistrationResultRepository inbox;
    private final UserRepository users;

    @KafkaListener(topics = "${kafka.topics.registration-result}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onResult(UserRegistrationResult result, Acknowledgment ack) {
        UUID eventId = UUID.fromString(result.getEventId().toString());
        String status = result.getStatus().name();
        String message = result.getMessage() == null ? null : result.getMessage().toString();
        try {
            if (inbox.existsById(eventId)) {
                ack.acknowledge();
                return;
            }
            var event = events.findById(eventId);
            if (event.isEmpty()) {
                inbox.save(eventId, status, message);
                ack.acknowledge();
                return;
            }
            Long userId = event.get().userId();
            if (result.getStatus() == RegistrationStatus.SUCCESS) {
                if (userId != null) users.setActive(userId, true);
                events.markSuccess(eventId);
            } else {
                events.markFail(eventId);
                if (userId != null) users.deleteById(userId);
            }
            inbox.save(eventId, status, message);
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Result processing failed, will retry: {}", e.toString());
            throw e;
        }
    }
}
