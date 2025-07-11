package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.RegistrationEvent;
import com.d_tech.libsys.service.EventTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Controller
 * Event monitoring ve yönetim işlemleri için
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final EventTrackingService eventTrackingService;

    /**
     * Event istatistiklerini getirir
     */
    @GetMapping("/events/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventTrackingService.EventStatistics> getEventStatistics() {
        log.info("Admin event istatistikleri istendi");

        EventTrackingService.EventStatistics stats = eventTrackingService.getEventStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Son 24 saatteki event'leri getirir
     */
    @GetMapping("/events/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationEvent>> getRecentEvents() {
        log.info("Admin son event'leri istedi");

        List<RegistrationEvent> recentEvents = eventTrackingService.getRecentEvents();
        return ResponseEntity.ok(recentEvents);
    }

    /**
     * Belirli durumdaki event'leri getirir
     */
    @GetMapping("/events/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationEvent>> getEventsByStatus(@PathVariable String status) {
        log.info("Admin {} durumundaki event'leri istedi", status);

        try {
            RegistrationEvent.EventStatus eventStatus = RegistrationEvent.EventStatus.valueOf(status.toUpperCase());
            List<RegistrationEvent> events = eventTrackingService.getEventsByStatus(eventStatus);
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            log.warn("Geçersiz event status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Kullanıcının tüm event'lerini getirir
     */
    @GetMapping("/events/user/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationEvent>> getUserEvents(@PathVariable String username) {
        log.info("Admin {} kullanıcısının event'lerini istedi", username);

        List<RegistrationEvent> userEvents = eventTrackingService.getUserEvents(username);
        return ResponseEntity.ok(userEvents);
    }

    /**
     * Event'i manuel olarak tamamlanmış olarak işaretle
     */
    @PutMapping("/events/{eventId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> markEventAsCompleted(
            @PathVariable String eventId,
            @RequestBody AdminActionRequest request) {

        log.info("Admin event'i manuel olarak tamamlıyor: eventId={}, message={}",
                eventId, request.getMessage());

        boolean success = eventTrackingService.markEventAsCompleted(eventId, request.getMessage());

        if (success) {
            return ResponseEntity.ok("Event başarıyla tamamlandı olarak işaretlendi");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Manuel event cleanup tetikle
     */
    @PostMapping("/events/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerEventCleanup() {
        log.info("Admin manuel event cleanup tetikledi");

        try {
            eventTrackingService.cleanupStaleEvents();
            return ResponseEntity.ok("Event cleanup başarıyla tamamlandı");
        } catch (Exception e) {
            log.error("Event cleanup hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Cleanup işlemi başarısız: " + e.getMessage());
        }
    }

    /**
     * Sistem durumu kontrolü
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        log.info("Admin sistem durumu kontrolü istedi");

        try {
            EventTrackingService.EventStatistics stats = eventTrackingService.getEventStatistics();

            SystemHealthResponse health = SystemHealthResponse.builder()
                    .status("HEALTHY")
                    .totalEvents(stats.getTotalEvents())
                    .pendingEvents(stats.getPendingEvents())
                    .processingEvents(stats.getProcessingEvents())
                    .successRate(stats.getSuccessRate())
                    .message("Sistem normal çalışıyor")
                    .build();

            // Eğer çok fazla pending/processing varsa uyarı
            if (stats.getPendingEvents() + stats.getProcessingEvents() > 100) {
                health.setStatus("WARNING");
                health.setMessage("Yüksek sayıda bekleyen event var");
            }

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("Sistem durumu kontrolü hatası: {}", e.getMessage(), e);

            SystemHealthResponse health = SystemHealthResponse.builder()
                    .status("ERROR")
                    .message("Sistem durumu kontrol edilemedi: " + e.getMessage())
                    .build();

            return ResponseEntity.internalServerError().body(health);
        }
    }

    /**
     * Admin action request DTO
     */
    @lombok.Data
    public static class AdminActionRequest {
        private String message;
    }

    /**
     * System health response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SystemHealthResponse {
        private String status;
        private String message;
        private Long totalEvents;
        private Long pendingEvents;
        private Long processingEvents;
        private Double successRate;
    }
}