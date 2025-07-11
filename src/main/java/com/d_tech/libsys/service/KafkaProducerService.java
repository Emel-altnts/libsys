package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer Service
 * Mesajları Kafka topic'lerine gönderir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.user-registration:user-registration-topic}")
    private String userRegistrationTopic;

    /**
     * Kullanıcı kayıt event'ini Kafka'ya gönderir
     *
     * @param event Kullanıcı kayıt event'i
     * @return Gönderim işleminin tamamlanıp tamamlanmadığını belirten CompletableFuture
     */
    public CompletableFuture<Boolean> sendUserRegistrationEvent(UserRegistrationEvent event) {
        log.info("Kafka'ya kullanıcı kayıt event'i gönderiliyor: eventId={}, username={}",
                event.getEventId(), event.getUsername());

        // Event'i processing durumuna getir
        event.setStatus(UserRegistrationEvent.EventStatus.PENDING);

        // Kafka'ya gönder
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(userRegistrationTopic, event.getEventId(), event);

        // Sonucu handle et
        return future.handle((result, throwable) -> {
            if (throwable != null) {
                log.error("Kafka'ya mesaj gönderimi başarısız: eventId={}, error={}",
                        event.getEventId(), throwable.getMessage(), throwable);
                return false;
            } else {
                log.info("Kafka'ya mesaj başarıyla gönderildi: eventId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return true;
            }
        });
    }

    /**
     * Genel amaçlı mesaj gönderme metodu
     *
     * @param topic Hedef topic
     * @param key Mesaj anahtarı
     * @param message Mesaj içeriği
     */
    public void sendMessage(String topic, String key, Object message) {
        log.info("Kafka'ya mesaj gönderiliyor: topic={}, key={}", topic, key);

        kafkaTemplate.send(topic, key, message)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Mesaj gönderimi başarısız: topic={}, key={}, error={}",
                                topic, key, throwable.getMessage());
                    } else {
                        log.info("Mesaj başarıyla gönderildi: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Retry event'i gönderir (DLQ veya retry topic'e)
     */
    public void sendRetryEvent(UserRegistrationEvent event) {
        String retryTopic = userRegistrationTopic + ".retry";
        log.info("Retry event'i gönderiliyor: eventId={}, retryCount={}",
                event.getEventId(), event.getRetryCount());

        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    /**
     * Dead Letter Queue'ya mesaj gönderir
     */
    public void sendToDLQ(UserRegistrationEvent event, String errorReason) {
        String dlqTopic = userRegistrationTopic + ".dlq";
        event.setMessage("DLQ'ya gönderildi: " + errorReason);
        event.setStatus(UserRegistrationEvent.EventStatus.FAILED);

        log.error("Event DLQ'ya gönderiliyor: eventId={}, reason={}",
                event.getEventId(), errorReason);

        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }
}