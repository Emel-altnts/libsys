package com.d_tech.libsys.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * Fatura event'i - Kafka için
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEvent {

    /**
     * Eşsiz event ID'si
     */
    private String eventId;

    /**
     * Event tipi
     */
    private EventType eventType;

    /**
     * Sipariş ID'si
     */
    private Long orderId;

    /**
     * Fatura bilgileri
     */
    private InvoiceRequest invoiceRequest;

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
     * Event tipi enum'u
     */
    public enum EventType {
        GENERATE_INVOICE,  // Fatura oluştur
        UPDATE_INVOICE,    // Fatura güncelle
        MARK_PAID,         // Ödendi olarak işaretle
        CANCEL_INVOICE     // Fatura iptal et
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
}
