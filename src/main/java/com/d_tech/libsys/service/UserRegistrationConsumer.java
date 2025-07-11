package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.dto.UserRegistrationEvent;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Kafka Consumer Service
 * Kullanıcı kayıt event'lerini işler
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationConsumer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Ana consumer - kullanıcı kayıt event'lerini işler
     *
     * @param event Kafka'dan gelen event
     * @param partition Mesajın geldiği partition
     * @param offset Mesajın offset'i
     * @param acknowledgment Manual commit için
     */
    @KafkaListener(
            topics = "${app.kafka.topic.user-registration:user-registration-topic}",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserRegistration(
            @Payload UserRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Kullanıcı kayıt event'i alındı: eventId={}, username={}, partition={}, offset={}",
                event.getEventId(), event.getUsername(), partition, offset);

        try {
            // 1. Event'i processing durumuna getir
            event.setStatus(UserRegistrationEvent.EventStatus.PROCESSING);
            log.info("Event işleniyor: eventId={}, username={}", event.getEventId(), event.getUsername());

            // 2. Son kontroller (double-check)
            if (!performFinalValidations(event)) {
                // Validasyon hatası - tekrar deneme gerekmiyor
                event.setStatus(UserRegistrationEvent.EventStatus.FAILED);
                acknowledgment.acknowledge(); // Commit et, tekrar işleme
                return;
            }

            // 3. Kullanıcıyı veritabanına kaydet
            User savedUser = createAndSaveUser(event);

            // 4. Başarı durumu
            event.setStatus(UserRegistrationEvent.EventStatus.COMPLETED);
            event.setMessage("Kullanıcı başarıyla kaydedildi: ID=" + savedUser.getId());

            log.info("Kullanıcı başarıyla kaydedildi: eventId={}, username={}, userId={}",
                    event.getEventId(), event.getUsername(), savedUser.getId());

            // 5. Commit işlemi
            acknowledgment.acknowledge();

            // 6. Başarı bildirimi (isteğe bağlı - email, notification vs.)
            sendSuccessNotification(event, savedUser);

        } catch (Exception e) {
            log.error("Kullanıcı kayıt işleminde hata: eventId={}, username={}, error={}",
                    event.getEventId(), event.getUsername(), e.getMessage(), e);

            // Hata durumunu handle et
            handleRegistrationError(event, e, acknowledgment);
        }
    }

    /**
     * Son validasyonları yapar (consumer seviyesinde)
     */
    private boolean performFinalValidations(UserRegistrationEvent event) {
        // Kullanıcı adı tekrar kontrol et (race condition'a karşı)
        if (userRepository.existsByUsername(event.getUsername())) {
            log.warn("Consumer seviyesinde kullanıcı adı çakışması tespit edildi: username={}",
                    event.getUsername());
            event.setMessage("Kullanıcı adı zaten mevcut (consumer seviyesi kontrolü)");
            return false;
        }

        // Şifre validasyonu
        if (event.getPassword() == null || event.getPassword().length() < 6) {
            log.warn("Consumer seviyesinde şifre validasyon hatası: username={}", event.getUsername());
            event.setMessage("Şifre validasyon hatası");
            return false;
        }

        // Şifre onayı kontrolü
        if (!event.getPassword().equals(event.getConfirmPassword())) {
            log.warn("Consumer seviyesinde şifre onay hatası: username={}", event.getUsername());
            event.setMessage("Şifreler eşleşmiyor");
            return false;
        }

        return true;
    }

    /**
     * Kullanıcıyı oluşturur ve veritabanına kaydeder
     */
    private User createAndSaveUser(UserRegistrationEvent event) {
        log.info("Kullanıcı oluşturuluyor: username={}", event.getUsername());

        User user = User.builder()
                .username(event.getUsername().trim())
                .password(passwordEncoder.encode(event.getPassword()))
                .roles(event.getRoles() != null ? event.getRoles() : Set.of("USER"))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Kullanıcı veritabanına kaydedildi: username={}, id={}",
                savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    /**
     * Kayıt hatası durumunda retry veya DLQ işlemlerini handle eder
     */
    private void handleRegistrationError(UserRegistrationEvent event, Exception error, Acknowledgment acknowledgment) {
        event.incrementRetry();
        event.setMessage("Hata: " + error.getMessage());

        if (event.canRetry()) {
            log.warn("Event retry edilecek: eventId={}, retryCount={}/{}, error={}",
                    event.getEventId(), event.getRetryCount(), event.getMaxRetries(), error.getMessage());

            // Retry topic'e gönder
            kafkaProducerService.sendRetryEvent(event);
            acknowledgment.acknowledge(); // Original mesajı commit et
        } else {
            log.error("Event maximum retry sayısına ulaştı, DLQ'ya gönderiliyor: eventId={}, error={}",
                    event.getEventId(), error.getMessage());

            // Dead Letter Queue'ya gönder
            kafkaProducerService.sendToDLQ(event, error.getMessage());
            acknowledgment.acknowledge(); // Original mesajı commit et
        }
    }

    /**
     * Başarı bildirimi gönderir (email, SMS, notification vs.)
     */
    private void sendSuccessNotification(UserRegistrationEvent event, User savedUser) {
        log.info("Başarı bildirimi gönderiliyor: username={}, userId={}",
                savedUser.getUsername(), savedUser.getId());

        // Burada email, SMS, push notification vs. gönderilebilir
        // Şimdilik sadece log yazıyoruz

        // Örnek: Email servisi çağrısı
        // emailService.sendWelcomeEmail(savedUser.getUsername(), savedUser.getId());

        // Örnek: Notification servisi çağrısı
        // notificationService.sendRegistrationSuccess(event.getEventId(), savedUser.getId());
    }

    /**
     * Retry topic'ten gelen mesajları işler
     */
    @KafkaListener(
            topics = "${app.kafka.topic.user-registration:user-registration-topic}.retry",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}.retry",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleRetryUserRegistration(
            @Payload UserRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Retry event'i alındı: eventId={}, username={}, retryCount={}, partition={}, offset={}",
                event.getEventId(), event.getUsername(), event.getRetryCount(), partition, offset);

        try {
            // Kısa bir bekleme süresi (exponential backoff)
            long waitTime = calculateBackoffTime(event.getRetryCount());
            Thread.sleep(waitTime);

            log.info("Retry işlemi başlatılıyor: eventId={}, waitTime={}ms", event.getEventId(), waitTime);

            // Ana işlemi tekrar çalıştır
            handleUserRegistrationRetry(event, acknowledgment);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry işlemi kesildi: eventId={}", event.getEventId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Retry işleminde hata: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            handleRegistrationError(event, e, acknowledgment);
        }
    }

    /**
     * Retry için ana işlemi yeniden yapar
     */
    private void handleUserRegistrationRetry(UserRegistrationEvent event, Acknowledgment acknowledgment) {
        event.setStatus(UserRegistrationEvent.EventStatus.PROCESSING);

        if (!performFinalValidations(event)) {
            event.setStatus(UserRegistrationEvent.EventStatus.FAILED);
            acknowledgment.acknowledge();
            return;
        }

        User savedUser = createAndSaveUser(event);

        event.setStatus(UserRegistrationEvent.EventStatus.COMPLETED);
        event.setMessage("Kullanıcı retry sonucu başarıyla kaydedildi: ID=" + savedUser.getId());

        log.info("Retry ile kullanıcı başarıyla kaydedildi: eventId={}, username={}, userId={}",
                event.getEventId(), event.getUsername(), savedUser.getId());

        acknowledgment.acknowledge();
        sendSuccessNotification(event, savedUser);
    }

    /**
     * Exponential backoff hesaplar
     */
    private long calculateBackoffTime(int retryCount) {
        // Exponential backoff: 2^retryCount * 1000ms (max 30 saniye)
        long backoff = (long) Math.pow(2, retryCount) * 1000;
        return Math.min(backoff, 30000); // Maksimum 30 saniye
    }

    /**
     * Dead Letter Queue'dan gelen mesajları işler (manuel müdahale için)
     */
    @KafkaListener(
            topics = "${app.kafka.topic.user-registration:user-registration-topic}.dlq",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}.dlq",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDLQMessages(
            @Payload UserRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.error("DLQ mesajı alındı - manuel müdahale gerekli: eventId={}, username={}, partition={}, offset={}, message={}",
                event.getEventId(), event.getUsername(), partition, offset, event.getMessage());

        // Bu mesajları bir veritabanına kaydet, admin panelinden görüntülenebilir hale getir
        // Şimdilik sadece log yazıyoruz ve commit ediyoruz

        acknowledgment.acknowledge();

        // Opsiyonel: Admin bilgilendirme
        // alertService.sendDLQAlert(event);
    }
}