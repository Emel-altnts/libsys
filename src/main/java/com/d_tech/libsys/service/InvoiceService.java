package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.Invoice;
import com.d_tech.libsys.domain.model.StockOrder;
import com.d_tech.libsys.dto.InvoiceEvent;
import com.d_tech.libsys.dto.InvoiceRequest;
import com.d_tech.libsys.repository.InvoiceRepository;
import com.d_tech.libsys.repository.StockOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fatura yönetim servisi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StockOrderRepository stockOrderRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Asenkron fatura oluşturma
     */
    public CompletableFuture<String> generateInvoiceAsync(Long orderId, InvoiceRequest invoiceRequest) {
        log.info("Asenkron fatura oluşturma başlatılıyor: orderId={}", orderId);

        try {
            // Temel validasyonlar
            validateInvoiceRequest(orderId, invoiceRequest);

            // Event oluştur
            InvoiceEvent event = InvoiceEvent.builder()
                    .eventId(generateEventId("GENERATE_INVOICE"))
                    .eventType(InvoiceEvent.EventType.GENERATE_INVOICE)
                    .orderId(orderId)
                    .invoiceRequest(invoiceRequest)
                    .build();

            // Kafka'ya gönder
            return kafkaProducerService.sendInvoiceEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Fatura oluşturma event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            log.error("Fatura oluşturma event'i gönderilemedi: orderId={}", orderId);
                            throw new RuntimeException("Fatura oluşturma event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Asenkron fatura oluşturma hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Senkron fatura oluşturma (Consumer tarafından çağrılır)
     */
    @Transactional
    public Invoice generateInvoice(Long orderId, InvoiceRequest invoiceRequest) {
        log.info("Fatura oluşturuluyor: orderId={}", orderId);

        // Sipariş kontrol et
        StockOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));

        // Zaten fatura var mı kontrol et
        if (invoiceRepository.findByStockOrderId(orderId).isPresent()) {
            throw new IllegalStateException("Bu sipariş için zaten fatura mevcut: " + orderId);
        }

        // Sipariş tamamlandı mı kontrol et
        if (!order.isCompleted()) {
            throw new IllegalStateException("Sipariş henüz tamamlanmadı: " + order.getStatus());
        }

        // Fatura numarası oluştur
        String invoiceNumber = generateInvoiceNumber();

        // Fatura oluştur
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .stockOrder(order)
                .dueDate(invoiceRequest.getDueDate() != null ?
                        invoiceRequest.getDueDate() :
                        LocalDateTime.now().plusDays(30)) // Varsayılan 30 gün vade
                .supplierName(order.getSupplierName())
                .supplierAddress(invoiceRequest.getSupplierAddress())
                .supplierTaxNumber(invoiceRequest.getSupplierTaxNumber())
                .supplierPhone(invoiceRequest.getSupplierPhone())
                .supplierEmail(invoiceRequest.getSupplierEmail())
                .buyerName(invoiceRequest.getBuyerName() != null ?
                        invoiceRequest.getBuyerName() :
                        "D-Tech Kütüphane Sistemi")
                .buyerAddress(invoiceRequest.getBuyerAddress())
                .buyerTaxNumber(invoiceRequest.getBuyerTaxNumber())
                .notes(invoiceRequest.getNotes())
                .createdBy(invoiceRequest.getCreatedBy())
                .build();

        // Sipariş tutarlarını kopyala
        invoice.copyAmountsFromOrder();

        // Faturayı kaydet
        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info("Fatura oluşturuldu: invoiceId={}, invoiceNumber={}, total={}",
                savedInvoice.getId(), savedInvoice.getInvoiceNumber(), savedInvoice.getGrandTotal());

        return savedInvoice;
    }

    /**
     * Fatura ödendi olarak işaretle
     */
    public CompletableFuture<String> markInvoiceAsPaidAsync(Long invoiceId, String paymentMethod, String userId) {
        log.info("Fatura ödendi olarak işaretleniyor: invoiceId={}, paymentMethod={}", invoiceId, paymentMethod);

        try {
            InvoiceEvent event = InvoiceEvent.builder()
                    .eventId(generateEventId("MARK_PAID"))
                    .eventType(InvoiceEvent.EventType.MARK_PAID)
                    .message("Payment method: " + paymentMethod + ", User: " + userId)
                    .build();

            return kafkaProducerService.sendInvoiceEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Fatura ödeme event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            throw new RuntimeException("Fatura ödeme event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Fatura ödeme hatası: invoiceId={}, error={}", invoiceId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Senkron fatura ödeme işareti (Consumer tarafından çağrılır)
     */
    @Transactional
    public Invoice markInvoiceAsPaid(Long invoiceId, String paymentMethod) {
        log.info("Fatura ödendi olarak işaretleniyor: invoiceId={}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura bulunamadı: " + invoiceId));

        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PAID) {
            throw new IllegalStateException("Fatura zaten ödenmiş: " + invoiceId);
        }

        invoice.markAsPaid(paymentMethod);
        Invoice paidInvoice = invoiceRepository.save(invoice);

        log.info("Fatura ödendi olarak işaretlendi: invoiceId={}, paymentDate={}",
                invoiceId, paidInvoice.getPaymentDate());

        return paidInvoice;
    }

    /**
     * Fatura listele - ödeme durumuna göre
     */
    public List<Invoice> getInvoicesByPaymentStatus(Invoice.PaymentStatus paymentStatus) {
        return invoiceRepository.findByPaymentStatus(paymentStatus);
    }

    /**
     * Vadesi geçen faturaları listele
     */
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now());
    }

    /**
     * Tedarikçiye göre faturaları listele
     */
    public List<Invoice> getInvoicesBySupplier(String supplierName) {
        return invoiceRepository.findBySupplierNameContainingIgnoreCase(supplierName);
    }

    /**
     * Fatura detayını getir
     */
    public Optional<Invoice> getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    /**
     * Fatura numarasıyla getir
     */
    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    /**
     * Sipariş ID'siyle fatura getir
     */
    public Optional<Invoice> getInvoiceByOrderId(Long orderId) {
        return invoiceRepository.findByStockOrderId(orderId);
    }

    /**
     * Kullanıcının faturalarını listele
     */
    public List<Invoice> getInvoicesByUser(String userId) {
        return invoiceRepository.findByCreatedByOrderByInvoiceDateDesc(userId);
    }

    /**
     * Toplam ödenmemiş tutar
     */
    public Double getTotalUnpaidAmount() {
        return invoiceRepository.calculateTotalUnpaidAmount();
    }

    /**
     * Belirli dönemdeki toplam fatura tutarı
     */
    public Double getTotalInvoiceAmount(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.calculateTotalInvoiceAmount(startDate, endDate);
    }

    /**
     * Fatura iptal etme
     */
    @Transactional
    public Invoice cancelInvoice(Long invoiceId, String reason) {
        log.info("Fatura iptal ediliyor: invoiceId={}, reason={}", invoiceId, reason);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura bulunamadı: " + invoiceId));

        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PAID) {
            throw new IllegalStateException("Ödenmiş fatura iptal edilemez: " + invoiceId);
        }

        invoice.setPaymentStatus(Invoice.PaymentStatus.CANCELLED);
        invoice.setNotes(invoice.getNotes() + " | İptal nedeni: " + reason);

        Invoice cancelledInvoice = invoiceRepository.save(invoice);
        log.info("Fatura iptal edildi: invoiceId={}", invoiceId);

        return cancelledInvoice;
    }

    /**
     * Fatura güncelleme
     */
    @Transactional
    public Invoice updateInvoice(Long invoiceId, InvoiceRequest updateRequest) {
        log.info("Fatura güncelleniyor: invoiceId={}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura bulunamadı: " + invoiceId));

        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PAID) {
            throw new IllegalStateException("Ödenmiş fatura güncellenemez: " + invoiceId);
        }

        // Güncelleme
        if (updateRequest.getDueDate() != null) invoice.setDueDate(updateRequest.getDueDate());
        if (updateRequest.getSupplierAddress() != null) invoice.setSupplierAddress(updateRequest.getSupplierAddress());
        if (updateRequest.getSupplierTaxNumber() != null) invoice.setSupplierTaxNumber(updateRequest.getSupplierTaxNumber());
        if (updateRequest.getSupplierPhone() != null) invoice.setSupplierPhone(updateRequest.getSupplierPhone());
        if (updateRequest.getSupplierEmail() != null) invoice.setSupplierEmail(updateRequest.getSupplierEmail());
        if (updateRequest.getBuyerName() != null) invoice.setBuyerName(updateRequest.getBuyerName());
        if (updateRequest.getBuyerAddress() != null) invoice.setBuyerAddress(updateRequest.getBuyerAddress());
        if (updateRequest.getBuyerTaxNumber() != null) invoice.setBuyerTaxNumber(updateRequest.getBuyerTaxNumber());
        if (updateRequest.getNotes() != null) invoice.setNotes(updateRequest.getNotes());

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Fatura güncellendi: invoiceId={}", invoiceId);

        return updatedInvoice;
    }

    /**
     * Fatura request validasyonu
     */
    private void validateInvoiceRequest(Long orderId, InvoiceRequest invoiceRequest) {
        if (orderId == null) {
            throw new IllegalArgumentException("Sipariş ID'si boş olamaz");
        }

        if (invoiceRequest.getCreatedBy() == null || invoiceRequest.getCreatedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("Fatura oluşturan kişi bilgisi boş olamaz");
        }
    }

    /**
     * Fatura numarası oluşturucu
     */
    private String generateInvoiceNumber() {
        String invoiceNumber;
        do {
            invoiceNumber = Invoice.generateInvoiceNumber();
        } while (invoiceRepository.existsByInvoiceNumber(invoiceNumber));

        return invoiceNumber;
    }

    /**
     * Event ID oluşturucu
     */
    private String generateEventId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
