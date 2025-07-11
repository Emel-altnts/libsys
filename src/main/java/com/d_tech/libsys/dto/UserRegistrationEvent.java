package com.d_tech.libsys.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Kafka üzerinden gönderilecek kullanıcı kayıt event'i
 * Bu sınıf, asenkron kullanıcı kayıt işlemi için gerekli tüm bilgileri içerir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationEvent {

    /**
     * Eşsiz event ID'si - her kayıt isteği için farklı
     */
    private String eventId;

    /**
     * Kayıt olmak isteyen kullanıcı adı
     */
    private String username;

    /**
     * Ham şifre (henüz encode edilmemiş)
     */
    private String password;

    /**
     * Şifre onayı
     */
    private String confirmPassword;

    /**
     * Kullanıcıya atanacak roller (varsayılan: USER)
     */
    @Builder.Default
    private Set<String> roles = Set.of("USER");

    /**
     * Event oluşturulma zamanı
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    /**
     * Event durumu (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    /**
     * İşlem sonucunda oluşan mesaj (başarı/hata)
     */
    private String message;

    /**
     * Retry sayısı - hata durumunda kaç kez deneneceği
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maksimum retry sayısı
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Event durumu enum'u
     */
    public enum EventStatus {
        PENDING,     // Beklemede
        PROCESSING,  // İşleniyor
        COMPLETED,   // Tamamlandı
        FAILED       // Başarısız
    }

    /**
     * Retry edilebilir mi kontrol eder
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Retry sayısını artırır
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}