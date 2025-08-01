package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.dto.UserRegistrationEvent;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Conditional Kafka Consumer - only active when Kafka is enabled
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class UserRegistrationConsumer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(
            topics = "${app.kafka.topic.user-registration:user-registration-topic}",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserRegistration(
            @Payload UserRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("User registration event received: eventId={}, username={}, partition={}, offset={}",
                event.getEventId(), event.getUsername(), partition, offset);

        try {
            event.setStatus(UserRegistrationEvent.EventStatus.PROCESSING);
            log.info("Processing event: eventId={}, username={}", event.getEventId(), event.getUsername());

            if (!performFinalValidations(event)) {
                event.setStatus(UserRegistrationEvent.EventStatus.FAILED);
                return;
            }

            User savedUser = createAndSaveUser(event);

            event.setStatus(UserRegistrationEvent.EventStatus.COMPLETED);
            event.setMessage("User successfully registered: ID=" + savedUser.getId());

            log.info("User successfully registered: eventId={}, username={}, userId={}",
                    event.getEventId(), event.getUsername(), savedUser.getId());

            sendSuccessNotification(event, savedUser);

        } catch (Exception e) {
            log.error("User registration processing error: eventId={}, username={}, error={}",
                    event.getEventId(), event.getUsername(), e.getMessage(), e);

            handleRegistrationError(event, e);
        }
    }

    private boolean performFinalValidations(UserRegistrationEvent event) {
        if (userRepository.existsByUsername(event.getUsername())) {
            log.warn("Consumer level username conflict detected: username={}",
                    event.getUsername());
            event.setMessage("Username already exists (consumer level check)");
            return false;
        }

        if (event.getPassword() == null || event.getPassword().length() < 6) {
            log.warn("Consumer level password validation error: username={}", event.getUsername());
            event.setMessage("Password validation error");
            return false;
        }

        if (!event.getPassword().equals(event.getConfirmPassword())) {
            log.warn("Consumer level password confirmation error: username={}", event.getUsername());
            event.setMessage("Passwords do not match");
            return false;
        }

        return true;
    }

    private User createAndSaveUser(UserRegistrationEvent event) {
        log.info("Creating user: username={}", event.getUsername());

        User user = User.builder()
                .username(event.getUsername().trim())
                .password(passwordEncoder.encode(event.getPassword()))
                .roles(event.getRoles() != null ? event.getRoles() : Set.of("USER"))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User saved to database: username={}, id={}",
                savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    private void handleRegistrationError(UserRegistrationEvent event, Exception error) {
        event.incrementRetry();
        event.setMessage("Error: " + error.getMessage());

        if (event.canRetry()) {
            log.warn("Event will be retried: eventId={}, retryCount={}/{}, error={}",
                    event.getEventId(), event.getRetryCount(), event.getMaxRetries(), error.getMessage());

            kafkaProducerService.sendRetryEvent(event);
        } else {
            log.error("Event reached maximum retry count, sending to DLQ: eventId={}, error={}",
                    event.getEventId(), error.getMessage());

            kafkaProducerService.sendToDLQ(event, error.getMessage());
        }
    }

    private void sendSuccessNotification(UserRegistrationEvent event, User savedUser) {
        log.info("Sending success notification: username={}, userId={}",
                savedUser.getUsername(), savedUser.getId());
        // Success notification implementation
    }

    @KafkaListener(
            topics = "${app.kafka.topic.user-registration:user-registration-topic}.retry",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}.retry",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleRetryUserRegistration(
            @Payload UserRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Retry event received: eventId={}, username={}, retryCount={}, partition={}, offset={}",
                event.getEventId(), event.getUsername(), event.getRetryCount(), partition, offset);

        try {
            long waitTime = calculateBackoffTime(event.getRetryCount());
            Thread.sleep(waitTime);

            log.info("Starting retry process: eventId={}, waitTime={}ms", event.getEventId(), waitTime);

            handleUserRegistrationRetry(event);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry process interrupted: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("Retry process error: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            handleRegistrationError(event, e);
        }
    }

    private void handleUserRegistrationRetry(UserRegistrationEvent event) {
        event.setStatus(UserRegistrationEvent.EventStatus.PROCESSING);

        if (!performFinalValidations(event)) {
            event.setStatus(UserRegistrationEvent.EventStatus.FAILED);
            return;
        }

        User savedUser = createAndSaveUser(event);

        event.setStatus(UserRegistrationEvent.EventStatus.COMPLETED);
        event.setMessage("User successfully registered via retry: ID=" + savedUser.getId());

        log.info("User successfully registered via retry: eventId={}, username={}, userId={}",
                event.getEventId(), event.getUsername(), savedUser.getId());

        sendSuccessNotification(event, savedUser);
    }

    private long calculateBackoffTime(int retryCount) {
        long backoff = (long) Math.pow(2, retryCount) * 1000;
        return Math.min(backoff, 30000);
    }

    @KafkaListener(
            topics = "${app.kafka.topic.user-registration:user-registration-topic}.dlq",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}.dlq",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDLQMessages(
            @Payload UserRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("DLQ message received - manual intervention required: eventId={}, username={}, partition={}, offset={}, message={}",
                event.getEventId(), event.getUsername(), partition, offset, event.getMessage());

        // Log DLQ messages for manual intervention
    }
}