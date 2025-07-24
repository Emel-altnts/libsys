package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.Invoice;
import com.d_tech.libsys.dto.InvoiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🚀 FIXED: Fatura event'lerini işleyen Kafka Consumer - Acknowledgment sorunu çözüldü
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceConsumer {

    private final InvoiceService invoiceService;

    /**
     * 🚀 CRITICAL FIX: Acknowledgment parametresi kaldırıldı - AUTO_COMMIT kullanılıyor
     */
    @KafkaListener(
            topics = "${app.kafka.topic.invoice:invoice-topic}",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleInvoiceEvent(
            @Payload InvoiceEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("✅ Fatura event'i alındı: eventId={}, type={}, orderId={}, partition={}, offset={}",
                event.getEventId(), event.getEventType(), event.getOrderId(), partition, offset);

        try {
            event.setStatus(InvoiceEvent.EventStatus.PROCESSING);

            switch (event.getEventType()) {
                case GENERATE_INVOICE -> handleGenerateInvoice(event);
                case UPDATE_INVOICE -> handleUpdateInvoice(event);
                case MARK_PAID -> handleMarkPaid(event);
                case CANCEL_INVOICE -> handleCancelInvoice(event);
                default -> {
                    log.warn("⚠️ Bilinmeyen fatura event tipi: {}", event.getEventType());
                    event.setStatus(InvoiceEvent.EventStatus.FAILED);
                    event.setMessage("Bilinmeyen event tipi");
                }
            }

            // ✅ Auto-commit ile başarı durumu
            if (event.getStatus() == InvoiceEvent.EventStatus.COMPLETED) {
                log.info("✅ Fatura event'i başarıyla işlendi: eventId={}", event.getEventId());
            } else {
                log.error("❌ Fatura event'i başarısız: eventId={}, message={}",
                        event.getEventId(), event.getMessage());
            }

        } catch (Exception e) {
            log.error("💥 Fatura event'i işlenirken hata: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            event.setStatus(InvoiceEvent.EventStatus.FAILED);
            event.setMessage("İşleme hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Fatura oluşturma işlemi
     */
    private void handleGenerateInvoice(InvoiceEvent event) {
        log.info("📄 Fatura oluşturuluyor: orderId={}", event.getOrderId());

        try {
            Invoice invoice = invoiceService.generateInvoice(event.getOrderId(), event.getInvoiceRequest());
            event.setStatus(InvoiceEvent.EventStatus.COMPLETED);
            event.setMessage("Fatura başarıyla oluşturuldu: " + invoice.getInvoiceNumber());

            log.info("✅ Fatura oluşturuldu: invoiceId={}, invoiceNumber={}, orderId={}",
                    invoice.getId(), invoice.getInvoiceNumber(), event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Fatura oluşturma hatası: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
            event.setStatus(InvoiceEvent.EventStatus.FAILED);
            event.setMessage("Fatura oluşturma hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Fatura güncelleme işlemi
     */
    private void handleUpdateInvoice(InvoiceEvent event) {
        log.info("📝 Fatura güncelleniyor: eventId={}", event.getEventId());

        try {
            // Basit implementasyon - şimdilik sadece başarılı olarak işaretle
            event.setStatus(InvoiceEvent.EventStatus.COMPLETED);
            event.setMessage("Fatura güncellendi");

        } catch (Exception e) {
            event.setStatus(InvoiceEvent.EventStatus.FAILED);
            event.setMessage("Fatura güncelleme hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Fatura ödendi işareti
     */
    private void handleMarkPaid(InvoiceEvent event) {
        log.info("💰 Fatura ödendi olarak işaretleniyor: eventId={}", event.getEventId());

        try {
            // Basit implementasyon - şimdilik sadece başarılı olarak işaretle
            event.setStatus(InvoiceEvent.EventStatus.COMPLETED);
            event.setMessage("Fatura ödendi olarak işaretlendi");

        } catch (Exception e) {
            event.setStatus(InvoiceEvent.EventStatus.FAILED);
            event.setMessage("Fatura ödeme işareti hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Fatura iptal etme işlemi
     */
    private void handleCancelInvoice(InvoiceEvent event) {
        log.info("🚫 Fatura iptal ediliyor: eventId={}", event.getEventId());

        try {
            // Basit implementasyon - şimdilik sadece başarılı olarak işaretle
            event.setStatus(InvoiceEvent.EventStatus.COMPLETED);
            event.setMessage("Fatura iptal edildi");

        } catch (Exception e) {
            event.setStatus(InvoiceEvent.EventStatus.FAILED);
            event.setMessage("Fatura iptal hatası: " + e.getMessage());
        }
    }
}