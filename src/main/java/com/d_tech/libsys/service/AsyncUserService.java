package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.SignupRequest;
import com.d_tech.libsys.dto.SignupResponse;
import com.d_tech.libsys.dto.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asenkron Kullanıcı Servisi
 * Kullanıcı kayıt işlemlerini Kafka üzerinden asenkron olarak yapar
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncUserService {

    private final KafkaProducerService kafkaProducerService;
    private final UserService userService; // Senkron user service (username kontrolü için)

    /**
     * Asenkron kullanıcı kayıt işlemi
     * Validasyonları yapar, ardından Kafka'ya event gönderir ve hemen yanıt döner
     *
     * @param signupRequest Kayıt bilgileri
     * @return Anında dönen yanıt (kayıt işlemi devam ediyor mesajı ile)
     */
    public SignupResponse registerUserAsync(SignupRequest signupRequest) {
        log.info("Asenkron kullanıcı kaydı başlatılıyor: username={}", signupRequest.getUsername());

        try {
            // 1. Temel validasyonları hemen yap
            String validationError = validateSignupRequest(signupRequest);
            if (validationError != null) {
                log.warn("Kayıt validasyon hatası: username={}, error={}",
                        signupRequest.getUsername(), validationError);
                return new SignupResponse(validationError);
            }

            // 2. Kullanıcı adı benzersizlik kontrolü (hemen yapılabilir)
            if (userService.existsByUsername(signupRequest.getUsername())) {
                log.warn("Kullanıcı adı zaten mevcut: username={}", signupRequest.getUsername());
                return new SignupResponse("Bu kullanıcı adı zaten mevcut!");
            }

            // 3. Event oluştur
            UserRegistrationEvent event = UserRegistrationEvent.builder()
                    .eventId(generateEventId())
                    .username(signupRequest.getUsername().trim())
                    .password(signupRequest.getPassword())
                    .confirmPassword(signupRequest.getConfirmPassword())
                    .build();

            // 4. Event'i Kafka'ya gönder (asenkron)
            CompletableFuture<Boolean> sendResult = kafkaProducerService.sendUserRegistrationEvent(event);

            // 5. Gönderim sonucunu log'la ama kullanıcıyı bekletme
            sendResult.whenComplete((success, throwable) -> {
                if (success != null && success) {
                    log.info("Kullanıcı kayıt event'i başarıyla Kafka'ya gönderildi: eventId={}, username={}",
                            event.getEventId(), event.getUsername());
                } else {
                    log.error("Kullanıcı kayıt event'i Kafka'ya gönderilemedi: eventId={}, username={}",
                            event.getEventId(), event.getUsername());
                }
            });

            // 6. Kullanıcıya anında yanıt döndür
            log.info("Kullanıcı kayıt isteği kabul edildi: username={}, eventId={}",
                    signupRequest.getUsername(), event.getEventId());

            return new SignupResponse(
                    String.format("Kayıt isteğiniz alındı ve işleniyor. Event ID: %s. " +
                                    "Kayıt tamamlandığında bilgilendirileceksiniz.",
                            event.getEventId())
            );

        } catch (Exception e) {
            log.error("Asenkron kayıt işleminde beklenmeyen hata: username={}, error={}",
                    signupRequest.getUsername(), e.getMessage(), e);
            return new SignupResponse("Kayıt isteği alınamadı. Lütfen tekrar deneyin.");
        }
    }

    /**
     * Signup request validasyonu
     *
     * @param request Kayıt isteği
     * @return Hata mesajı (null ise valid)
     */
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

        return null; // Validation başarılı
    }

    /**
     * Benzersiz event ID oluşturur
     */
    private String generateEventId() {
        return "USER_REG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Event durumu sorgulama (isteğe bağlı)
     * Kullanıcı, event ID ile kayıt durumunu sorgulayabilir
     */
    public String getRegistrationStatus(String eventId) {
        // Bu metod, event tracking için bir veritabanı tablosu veya cache gerektirir
        // Şimdilik basit bir mesaj döndürüyoruz
        log.info("Kayıt durumu sorgulanıyor: eventId={}", eventId);
        return "Event durumu için ayrı bir tracking sistemi gereklidir.";
    }
}