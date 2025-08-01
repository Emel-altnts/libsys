package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.SignupRequest;
import com.d_tech.libsys.dto.SignupResponse;
import com.d_tech.libsys.dto.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Conditional Async User Service - fallback to sync when Kafka is disabled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncUserService {

    private final UserService userService;

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public SignupResponse registerUserAsync(SignupRequest signupRequest) {
        log.info("Async user registration request: username={}, kafkaEnabled={}",
                signupRequest.getUsername(), kafkaEnabled);

        if (!kafkaEnabled || kafkaProducerService == null) {
            log.info("Kafka disabled or producer not available, falling back to sync registration");
            return userService.registerUser(signupRequest);
        }

        try {
            // Basic validations
            String validationError = validateSignupRequest(signupRequest);
            if (validationError != null) {
                log.warn("Registration validation error: username={}, error={}",
                        signupRequest.getUsername(), validationError);
                return new SignupResponse(validationError);
            }

            // Check username uniqueness
            if (userService.existsByUsername(signupRequest.getUsername())) {
                log.warn("Username already exists: username={}", signupRequest.getUsername());
                return new SignupResponse("Bu kullanıcı adı zaten mevcut!");
            }

            // Create event
            UserRegistrationEvent event = UserRegistrationEvent.builder()
                    .eventId(generateEventId())
                    .username(signupRequest.getUsername().trim())
                    .password(signupRequest.getPassword())
                    .confirmPassword(signupRequest.getConfirmPassword())
                    .build();

            // Send to Kafka
            kafkaProducerService.sendUserRegistrationEvent(event)
                    .whenComplete((success, throwable) -> {
                        if (success != null && success) {
                            log.info("User registration event sent successfully: eventId={}, username={}",
                                    event.getEventId(), event.getUsername());
                        } else {
                            log.error("Failed to send user registration event: eventId={}, username={}",
                                    event.getEventId(), event.getUsername());
                        }
                    });

            log.info("User registration request accepted: username={}, eventId={}",
                    signupRequest.getUsername(), event.getEventId());

            return new SignupResponse(
                    String.format("Kayıt isteğiniz alındı ve işleniyor. Event ID: %s",
                            event.getEventId())
            );

        } catch (Exception e) {
            log.error("Async registration error: username={}, error={}",
                    signupRequest.getUsername(), e.getMessage(), e);

            // Fallback to sync registration
            log.info("Falling back to sync registration due to async error");
            return userService.registerUser(signupRequest);
        }
    }

    private String validateSignupRequest(SignupRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return "Kullanıcı adı boş olamaz!";
        }

        if (request.getUsername().length() < 3) {
            return "Kullanıcı adı en az 3 karakter olmalıdır!";
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return "Şifre en az 6 karakter olmalıdır!";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return "Şifreler eşleşmiyor!";
        }

        return null;
    }

    private String generateEventId() {
        return "USER_REG_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    public String getRegistrationStatus(String eventId) {
        log.info("Registration status queried: eventId={}", eventId);
        if (!kafkaEnabled) {
            return "Kafka disabled - check user directly in database";
        }
        return "Event tracking requires separate implementation";
    }
}