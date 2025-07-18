package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.Invoice;
import com.d_tech.libsys.dto.InvoiceRequest;
import com.d_tech.libsys.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Fatura yönetim controller'ı
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Asenkron fatura oluştur
     */
    @PostMapping("/generate/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> generateInvoiceAsync(
            @PathVariable Long orderId,
            @RequestBody InvoiceRequest invoiceRequest,
            Authentication authentication) {

        log.info("Asenkron fatura oluşturma isteği: orderId={}, user={}",
                orderId, authentication.getName());

        // Oluşturan kişiyi set et
        invoiceRequest.setCreatedBy(authentication.getName());

        try {
            CompletableFuture<String> future = invoiceService.generateInvoiceAsync(orderId, invoiceRequest);
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Fatura oluşturma işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Fatura oluşturma hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Fatura oluşturma işlemi başlatılamadı", null));
        }
    }

    /**
     * Fatura ödendi olarak işaretle
     */
    @PostMapping("/{invoiceId}/mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> markInvoiceAsPaidAsync(
            @PathVariable Long invoiceId,
            @RequestBody PaymentRequest paymentRequest,
            Authentication authentication) {

        log.info("Fatura ödeme işareti isteği: invoiceId={}, paymentMethod={}, user={}",
                invoiceId, paymentRequest.getPaymentMethod(), authentication.getName());

        try {
            CompletableFuture<String> future = invoiceService.markInvoiceAsPaidAsync(
                    invoiceId, paymentRequest.getPaymentMethod(), authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Fatura ödeme işareti işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Fatura ödeme işareti hatası: invoiceId={}, error={}", invoiceId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Fatura ödeme işareti işlemi başlatılamadı", null));
        }
    }

    /**
     * Fatura detayını getir
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long invoiceId) {
        log.info("Fatura detayı istendi: invoiceId={}", invoiceId);

        Optional<Invoice> invoice = invoiceService.getInvoiceById(invoiceId);
        return invoice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Fatura numarasıyla getir
     */
    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("Fatura detayı istendi: invoiceNumber={}", invoiceNumber);

        Optional<Invoice> invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return invoice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Sipariş ID'siyle fatura getir
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> getInvoiceByOrderId(@PathVariable Long orderId) {
        log.info("Sipariş faturası istendi: orderId={}", orderId);

        Optional<Invoice> invoice = invoiceService.getInvoiceByOrderId(orderId);
        return invoice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ödeme durumuna göre faturaları listele
     */
    @GetMapping("/payment-status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getInvoicesByPaymentStatus(@PathVariable String status) {
        log.info("Ödeme durumuna göre faturalar istendi: status={}", status);

        try {
            Invoice.PaymentStatus paymentStatus = Invoice.PaymentStatus.valueOf(status.toUpperCase());
            List<Invoice> invoices = invoiceService.getInvoicesByPaymentStatus(paymentStatus);
            return ResponseEntity.ok(invoices);
        } catch (IllegalArgumentException e) {
            log.warn("Geçersiz ödeme durumu: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Vadesi geçen faturaları listele
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getOverdueInvoices() {
        log.info("Vadesi geçen faturalar istendi");

        List<Invoice> invoices = invoiceService.getOverdueInvoices();
        return ResponseEntity.ok(invoices);
    }

    /**
     * Ödenmemiş faturaları listele
     */
    @GetMapping("/unpaid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getUnpaidInvoices() {
        log.info("Ödenmemiş faturalar istendi");

        List<Invoice> invoices = invoiceService.getInvoicesByPaymentStatus(Invoice.PaymentStatus.UNPAID);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Tedarikçiye göre faturaları listele
     */
    @GetMapping("/supplier/{supplierName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getInvoicesBySupplier(@PathVariable String supplierName) {
        log.info("Tedarikçiye göre faturalar istendi: supplier={}", supplierName);

        List<Invoice> invoices = invoiceService.getInvoicesBySupplier(supplierName);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Kullanıcının faturalarını listele
     */
    @GetMapping("/my-invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getMyInvoices(Authentication authentication) {
        log.info("Kullanıcı faturaları istendi: user={}", authentication.getName());

        List<Invoice> invoices = invoiceService.getInvoicesByUser(authentication.getName());
        return ResponseEntity.ok(invoices);
    }

    /**
     * Toplam ödenmemiş tutar
     */
    @GetMapping("/total-unpaid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Double> getTotalUnpaidAmount() {
        log.info("Toplam ödenmemiş tutar istendi");

        Double totalUnpaid = invoiceService.getTotalUnpaidAmount();
        return ResponseEntity.ok(totalUnpaid);
    }

    /**
     * Belirli dönemdeki toplam fatura tutarı
     */
    @GetMapping("/total-amount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Double> getTotalInvoiceAmount(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        log.info("Dönemsel toplam fatura tutarı istendi: {} - {}", startDate, endDate);

        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);

            Double totalAmount = invoiceService.getTotalInvoiceAmount(start, end);
            return ResponseEntity.ok(totalAmount);
        } catch (Exception e) {
            log.error("Tarih parse hatası: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Fatura güncelle
     */
    @PutMapping("/{invoiceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable Long invoiceId,
            @RequestBody InvoiceRequest updateRequest) {

        log.info("Fatura güncelleme isteği: invoiceId={}", invoiceId);

        try {
            Invoice updatedInvoice = invoiceService.updateInvoice(invoiceId, updateRequest);
            return ResponseEntity.ok(updatedInvoice);
        } catch (Exception e) {
            log.error("Fatura güncelleme hatası: invoiceId={}, error={}", invoiceId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Fatura iptal et
     */
    @PostMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> cancelInvoice(
            @PathVariable Long invoiceId,
            @RequestBody CancelInvoiceRequest request) {

        log.info("Fatura iptal isteği: invoiceId={}, reason={}", invoiceId, request.getReason());

        try {
            Invoice cancelledInvoice = invoiceService.cancelInvoice(invoiceId, request.getReason());
            return ResponseEntity.ok(cancelledInvoice);
        } catch (Exception e) {
            log.error("Fatura iptal hatası: invoiceId={}, error={}", invoiceId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Fatura istatistikleri
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceStatistics> getInvoiceStatistics() {
        log.info("Fatura istatistikleri istendi");

        try {
            long totalInvoices = 0; // invoiceService.getTotalInvoiceCount();
            long unpaidInvoices = invoiceService.getInvoicesByPaymentStatus(Invoice.PaymentStatus.UNPAID).size();
            long overdueInvoices = invoiceService.getOverdueInvoices().size();
            Double totalUnpaidAmount = invoiceService.getTotalUnpaidAmount();
            Double monthlyTotal = invoiceService.getTotalInvoiceAmount(
                    LocalDateTime.now().minusDays(30), LocalDateTime.now());

            InvoiceStatistics stats = InvoiceStatistics.builder()
                    .totalInvoices(totalInvoices)
                    .unpaidInvoices(unpaidInvoices)
                    .overdueInvoices(overdueInvoices)
                    .totalUnpaidAmount(totalUnpaidAmount)
                    .monthlyTotal(monthlyTotal)
                    .build();

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Fatura istatistikleri hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // DTO sınıfları
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AsyncResponse {
        private String message;
        private String eventId;
    }

    @lombok.Data
    public static class PaymentRequest {
        private String paymentMethod;
    }

    @lombok.Data
    public static class CancelInvoiceRequest {
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InvoiceStatistics {
        private long totalInvoices;
        private long unpaidInvoices;
        private long overdueInvoices;
        private Double totalUnpaidAmount;
        private Double monthlyTotal;
    }
}