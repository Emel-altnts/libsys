package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.RegistrationEvent;
import com.d_tech.libsys.dto.UserRegistrationEvent;
import com.d_tech.libsys.repository.RegistrationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Event tracking servisi
 * Kullanıcı kayıt event'lerinin durumunu takip eder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventTrackingService {

    private final RegistrationEventRepository eventRepository;

    /**
     * Yeni event kaydı oluşturur
     */
    @Transactional
    public RegistrationEvent createEvent(UserRegistrationEvent kafkaEvent) {
        log.info("Event tracking kaydı oluşturuluyor: eventId={}, username={}",
                kafkaEvent.getEventId(), kafkaEvent.getUsername());

        RegistrationEvent event = RegistrationEvent.builder()
                .eventId(kafkaEvent.getEventId())
                .username(kafkaEvent.getUsername())
                .status(mapStatus(kafkaEvent.getStatus()))
                .message("Event Kafka'ya gönderildi")
                .retryCount(kafkaEvent.getRetryCount())
                .build();

        RegistrationEvent savedEvent = eventRepository.save(event);
        log.info("Event tracking kaydı oluşturuldu: id={}, eventId={}", savedEvent.getId(), savedEvent.getEventId());

        return savedEvent;
    }

    /**
     * Event durumunu günceller
     */
    @Transactional
    public void updateEventStatus(String eventId, UserRegistrationEvent.EventStatus kafkaStatus, String message) {
        log.info("Event durumu güncelleniyor: eventId={}, status={}, message={}",
                eventId, kafkaStatus, message);

        Optional<RegistrationEvent> optionalEvent = eventRepository.findByEventId(eventId);
        if (optionalEvent.isPresent()) {
            RegistrationEvent event = optionalEvent.get();
            event.setStatus(mapStatus(kafkaStatus));
            event.setMessage(message);
            event.setUpdatedAt(LocalDateTime.now());

            // Tamamlanma zamanını işaretle
            if (kafkaStatus == UserRegistrationEvent.EventStatus.COMPLETED ||
                    kafkaStatus == UserRegistrationEvent.EventStatus.FAILED) {
                event.setCompletedAt(LocalDateTime.now());
            }

            RegistrationEvent savedEvent = eventRepository.save(event);
            log.info("Event durumu güncellendi: eventId={}, newStatus={}", eventId, savedEvent.getStatus());
        } else {
            log.warn("Event bulunamadı, yeni kayıt oluşturuluyor: eventId={}", eventId);

            // Event bulunamadıysa yeni bir kayıt oluştur
            RegistrationEvent newEvent = RegistrationEvent.builder()
                    .eventId(eventId)
                    .username("UNKNOWN") // Username bilinmiyor
                    .status(mapStatus(kafkaStatus))
                    .message(message)
                    .retryCount(0)
                    .build();

            eventRepository.save(newEvent);
        }
    }

    /**
     * Retry count'u günceller
     */
    @Transactional
    public void updateRetryCount(String eventId, int retryCount) {
        log.info("Event retry count güncelleniyor: eventId={}, retryCount={}", eventId, retryCount);

        Optional<RegistrationEvent> optionalEvent = eventRepository.findByEventId(eventId);
        if (optionalEvent.isPresent()) {
            RegistrationEvent event = optionalEvent.get();
            event.setRetryCount(retryCount);
            event.setStatus(RegistrationEvent.EventStatus.RETRY);
            event.setMessage("Retry işlemi - " + retryCount + ". deneme");
            event.setUpdatedAt(LocalDateTime.now());

            RegistrationEvent savedEvent = eventRepository.save(event);
            log.info("Event retry count güncellendi: eventId={}, retryCount={}", eventId, savedEvent.getRetryCount());
        } else {
            log.warn("Retry update için event bulunamadı: eventId={}", eventId);
        }
    }

    /**
     * Event ID ile durumu sorgular
     */
    public Optional<RegistrationEvent> getEventStatus(String eventId) {
        log.info("Event durumu sorgulanıyor: eventId={}", eventId);

        Optional<RegistrationEvent> eventOpt = eventRepository.findByEventId(eventId);
        if (eventOpt.isPresent()) {
            log.info("Event bulundu: eventId={}, status={}", eventId, eventOpt.get().getStatus());
        } else {
            log.warn("Event bulunamadı: eventId={}", eventId);
        }

        return eventOpt;
    }

    /**
     * Kullanıcının tüm event'lerini getirir
     */
    public List<RegistrationEvent> getUserEvents(String username) {
        log.info("Kullanıcı event'leri getiriliyor: username={}", username);

        List<RegistrationEvent> events = eventRepository.findByUsernameOrderByCreatedAtDesc(username);
        log.info("Kullanıcı için {} event bulundu: username={}", events.size(), username);

        return events;
    }

    /**
     * Belirli durumdaki event'leri getirir
     */
    public List<RegistrationEvent> getEventsByStatus(RegistrationEvent.EventStatus status) {
        log.info("Status'e göre event'ler getiriliyor: status={}", status);

        List<RegistrationEvent> events = eventRepository.findByStatus(status);
        log.info("{} durumunda {} event bulundu", status, events.size());

        return events;
    }

    /**
     * Eski/takılı kalmış event'leri temizler
     * Bu metot scheduled olarak çalışır (her saat)
     */
    @Scheduled(fixedRate = 3600000) // Her saat (3600000 ms)
    @Transactional
    public void cleanupStaleEvents() {
        log.info("Eski event'ler temizleniyor...");

        try {
            // 2 saatten eski pending/processing event'ler
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);
            List<RegistrationEvent> staleEvents = eventRepository.findStaleEvents(cutoffTime);

            log.info("Temizlenecek eski event sayısı: {}", staleEvents.size());

            for (RegistrationEvent event : staleEvents) {
                event.setStatus(RegistrationEvent.EventStatus.FAILED);
                event.setMessage("Timeout - sistem tarafından başarısız olarak işaretlendi");
                event.setUpdatedAt(LocalDateTime.now());
                event.setCompletedAt(LocalDateTime.now());

                eventRepository.save(event);
                log.warn("Event timeout nedeniyle başarısız olarak işaretlendi: eventId={}", event.getEventId());
            }

            if (staleEvents.size() > 0) {
                log.info("Toplam {} eski event temizlendi", staleEvents.size());
            }

        } catch (Exception e) {
            log.error("Eski event'ler temizlenirken hata oluştu: {}", e.getMessage(), e);
        }
    }

    /**
     * Event istatistiklerini getirir
     */
    public EventStatistics getEventStatistics() {
        log.info("Event istatistikleri hesaplanıyor...");

        try {
            long totalEvents = eventRepository.count();
            long pendingEvents = eventRepository.findByStatus(RegistrationEvent.EventStatus.PENDING).size();
            long processingEvents = eventRepository.findByStatus(RegistrationEvent.EventStatus.PROCESSING).size();
            long completedEvents = eventRepository.findByStatus(RegistrationEvent.EventStatus.COMPLETED).size();
            long failedEvents = eventRepository.findByStatus(RegistrationEvent.EventStatus.FAILED).size();
            long retryEvents = eventRepository.findByStatus(RegistrationEvent.EventStatus.RETRY).size();

            EventStatistics stats = EventStatistics.builder()
                    .totalEvents(totalEvents)
                    .pendingEvents(pendingEvents)
                    .processingEvents(processingEvents)
                    .completedEvents(completedEvents)
                    .failedEvents(failedEvents)
                    .retryEvents(retryEvents)
                    .successRate(totalEvents > 0 ? (double) completedEvents / totalEvents * 100 : 0.0)
                    .build();

            log.info("Event istatistikleri: total={}, completed={}, failed={}, success rate={}%",
                    totalEvents, completedEvents, failedEvents, String.format("%.2f", stats.getSuccessRate()));

            return stats;

        } catch (Exception e) {
            log.error("Event istatistikleri hesaplanırken hata oluştu: {}", e.getMessage(), e);
            return EventStatistics.builder()
                    .totalEvents(0)
                    .successRate(0.0)
                    .build();
        }
    }

    /**
     * Son 24 saatteki event'leri getirir
     */
    public List<RegistrationEvent> getRecentEvents() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<RegistrationEvent> recentEvents = eventRepository.findByCreatedAtAfter(yesterday);

        log.info("Son 24 saatte {} event bulundu", recentEvents.size());
        return recentEvents;
    }

    /**
     * Event'i manuel olarak tamamlanmış olarak işaretle (admin işlemi)
     */
    @Transactional
    public boolean markEventAsCompleted(String eventId, String adminMessage) {
        log.info("Event manuel olarak tamamlanıyor: eventId={}, admin message={}", eventId, adminMessage);

        Optional<RegistrationEvent> optionalEvent = eventRepository.findByEventId(eventId);
        if (optionalEvent.isPresent()) {
            RegistrationEvent event = optionalEvent.get();
            event.setStatus(RegistrationEvent.EventStatus.COMPLETED);
            event.setMessage("Manuel olarak tamamlandı: " + adminMessage);
            event.setUpdatedAt(LocalDateTime.now());
            event.setCompletedAt(LocalDateTime.now());

            eventRepository.save(event);
            log.info("Event manuel olarak tamamlandı: eventId={}", eventId);
            return true;
        } else {
            log.warn("Manuel tamamlama için event bulunamadı: eventId={}", eventId);
            return false;
        }
    }

    /**
     * Kafka status'unu veritabanı status'una çevirir
     */
    private RegistrationEvent.EventStatus mapStatus(UserRegistrationEvent.EventStatus kafkaStatus) {
        return switch (kafkaStatus) {
            case PENDING -> RegistrationEvent.EventStatus.PENDING;
            case PROCESSING -> RegistrationEvent.EventStatus.PROCESSING;
            case COMPLETED -> RegistrationEvent.EventStatus.COMPLETED;
            case FAILED -> RegistrationEvent.EventStatus.FAILED;
        };
    }

    /**
     * Event istatistikleri için inner class
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EventStatistics {
        private long totalEvents;
        private long pendingEvents;
        private long processingEvents;
        private long completedEvents;
        private long failedEvents;
        private long retryEvents;
        private double successRate;
    }
}