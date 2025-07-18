package com.d_tech.libsys.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stok kontrol event'i - Kafka için
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockControlEvent {

    /**
     * Eşsiz event ID'si
     */
    private String eventId;

    /**
     * Event tipi
     */
    private EventType eventType;

    /**
     * Kitap ID'si
     */
    private Long bookId;

    /**
     * İşlem miktarı (stok azaltma/artırma)
     */
    private Integer quantity;

    /**
     * İşlem yapan kullanıcı
     */
    private String userId;

    /**
     * Event oluşturulma zamanı
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    /**
     * Event durumu
     */
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    /**
     * İşlem sonucu mesajı
     */
    private String message;

    /**
     * Retry sayısı
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maksimum retry sayısı
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Event tipi enum'u
     */
    public enum EventType {
        STOCK_CHECK,        // Stok kontrolü
        STOCK_DECREASE,     // Stok azaltma
        STOCK_INCREASE,     // Stok artırma
        RESTOCK_NEEDED,     // Yeniden stok gerekli
        LOW_STOCK_ALERT,    // Düşük stok uyarısı
        OUT_OF_STOCK_ALERT  // Stok tükendi uyarısı
    }

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

