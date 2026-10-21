package com.shoplab.auth.saga;

import com.shoplab.auth.config.KafkaTopicsProperties;
import com.shoplab.auth.repo.RegistrationEventRepository;
import com.shoplab.contracts.avro.UserRegistrationRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationSender {

    private final RegistrationEventRepository events;
    private final KafkaTemplate<String, Object> kafka;
    private final KafkaTopicsProperties topics;

    @Scheduled(fixedDelayString = "${kafka.sender.fixed-delay-ms:2000}")
    @Transactional
    public void sendPending() {
        var batch = events.findEventsToProcess();
        for (var e : batch) {
            if (e.attemptCount() >= 3) {
                events.markFailed(e.id());
                continue;
            }
            events.markPreSending(e.id());
            try {
                UserRegistrationRequest msg = toAvro(e.id(), events.fromJson(e.payload()));
                kafka.send(topics.registration(), e.id().toString(), msg).whenComplete((r, ex) -> {
                    if (ex == null) events.markSending(e.id());
                    else {
                        log.warn("Send failed {}: {}", e.id(), ex.toString());
                        events.markRetriable(e.id(), e.attemptCount());
                    }
                });
            } catch (Exception ex) {
                events.markFailed(e.id());
            }
        }
    }

    private UserRegistrationRequest toAvro(UUID eventId, RegistrationMessage msg) {
        return UserRegistrationRequest.newBuilder()
                .setEventId(eventId.toString())
                .setAuthUserId(msg.authUserId())
                .setUsername(msg.username())
                .setFirstName(msg.firstName())
                .setLastName(msg.lastName())
                .setEmail(msg.email())
                .setBirthDate(msg.birthDate())
                .build();
    }
}
