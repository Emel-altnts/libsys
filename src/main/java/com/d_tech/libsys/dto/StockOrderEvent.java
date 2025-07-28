package com.d_tech.libsys.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🚀 UPDATED: Stok sipariş event'i - SHIP_ORDER eklendi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOrderEvent {

    /**
     * Eşsiz event ID'si
     */
    private String eventId;

    /**
     * Event tipi
     */
    private EventType eventType;

    /**
     * Sipariş bilgileri
     */
    private StockOrderRequest orderRequest;

    /**
     * Sipariş ID'si (oluşturulduktan sonra)
     */
    private Long orderId;

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
     * 🚀 UPDATED: Event tipi enum'u - SHIP_ORDER eklendi
     */
    public enum EventType {
        CREATE_ORDER,      // Sipariş oluştur
        UPDATE_ORDER,      // Sipariş güncelle
        CONFIRM_ORDER,     // Sipariş onayla
        SHIP_ORDER,        // 🚀 YENİ: Sipariş kargoya ver (CONFIRMED → SHIPPED)
        CANCEL_ORDER,      // Sipariş iptal et
        RECEIVE_ORDER,     // Sipariş teslimat al
        GENERATE_INVOICE   // Fatura oluştur
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
        return retryCount < 3;
    }

    /**
     * Retry sayısını artırır
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}