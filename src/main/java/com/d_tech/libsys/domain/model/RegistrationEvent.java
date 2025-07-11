package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Kullanıcı kayıt event'lerini takip etmek için veritabanı tablosu
 * Kullanıcılar event ID ile kayıt durumlarını sorgulayabilir
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "registration_events")
public class RegistrationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private String message;

    private Integer retryCount;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Event durumu enum'u
     */
    public enum EventStatus {
        PENDING,     // Kafka'ya gönderildi, işlem bekleniyor
        PROCESSING,  // Consumer tarafından işleniyor
        COMPLETED,   // Başarıyla tamamlandı
        FAILED,      // Kalıcı olarak başarısız
        RETRY        // Tekrar denenecek
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == EventStatus.COMPLETED || status == EventStatus.FAILED) {
            completedAt = LocalDateTime.now();
        }
    }
}
