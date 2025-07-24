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
 * 🚀 ENHANCED: Fatura yönetim controller'ı - Lazy Loading sorunları çözüldü
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

        try {
            Optional<Invoice> invoice = invoiceService.getInvoiceById(invoiceId);
            return invoice.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Fatura detayı getirme hatası: invoiceId={}, error={}", invoiceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Fatura numarasıyla getir
     */
    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("Fatura detayı istendi: invoiceNumber={}", invoiceNumber);

        try {
            Optional<Invoice> invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
            return invoice.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Fatura numarası ile getirme hatası: invoiceNumber={}, error={}",
                    invoiceNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 CRITICAL FIX: Sipariş ID'siyle fatura getir - Lazy Loading çözüldü
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getInvoiceByOrderId(@PathVariable Long orderId) {
        System.out.println("=== ENHANCED: Sipariş faturası isteniyor ===");
        System.out.println("🔍 Order ID: " + orderId);
        log.info("Sipariş faturası istendi: orderId={}", orderId);

        try {
            // ✅ Input validation
            if (orderId == null || orderId <= 0) {
                log.warn("Geçersiz order ID: {}", orderId);
                return ResponseEntity.badRequest().body(
                        ErrorResponse.builder()
                                .error("INVALID_ORDER_ID")
                                .message("Geçersiz sipariş ID'si: " + orderId)
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            }

            // ✅ Enhanced service call with multiple strategies
            Optional<Invoice> invoiceOpt = invoiceService.getInvoiceByOrderId(orderId);

            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();
                System.out.println("✅ Fatura bulundu: ID=" + invoice.getId() +
                        ", Number=" + invoice.getInvoiceNumber() +
                        ", Total=" + invoice.getGrandTotal());

                log.info("Sipariş faturası bulundu: orderId={}, invoiceId={}, invoiceNumber={}",
                        orderId, invoice.getId(), invoice.getInvoiceNumber());

                return ResponseEntity.ok(invoice);
            } else {
                System.out.println("❌ Fatura bulunamadı: orderId=" + orderId);
                log.warn("Sipariş faturası bulunamadı: orderId={}", orderId);

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.out.println("💥 Sipariş faturası getirme hatası: " + e.getMessage());
            log.error("Sipariş faturası getirme hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("Fatura getirilirken hata oluştu: " + e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .orderId(orderId)
                            .build()
            );
        }
    }

    /**
     * 🚀 NEW: Sipariş fatura durumu kontrolü - hızlı endpoint
     */
    @GetMapping("/order/{orderId}/exists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceExistsResponse> checkInvoiceExists(@PathVariable Long orderId) {
        log.info("Sipariş fatura varlığı kontrolü: orderId={}", orderId);

        try {
            Optional<Invoice> invoiceOpt = invoiceService.getInvoiceByOrderId(orderId);

            InvoiceExistsResponse response = InvoiceExistsResponse.builder()
                    .orderId(orderId)
                    .hasInvoice(invoiceOpt.isPresent())
                    .invoiceId(invoiceOpt.map(Invoice::getId).orElse(null))
                    .invoiceNumber(invoiceOpt.map(Invoice::getInvoiceNumber).orElse(null))
                    .checkTime(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Fatura varlık kontrolü hatası: orderId={}, error={}", orderId, e.getMessage(), e);

            InvoiceExistsResponse errorResponse = InvoiceExistsResponse.builder()
                    .orderId(orderId)
                    .hasInvoice(false)
                    .error("Kontrol edilemedi: " + e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
        } catch (Exception e) {
            log.error("Ödeme durumu sorgusu hatası: status={}, error={}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Vadesi geçen faturaları listele
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getOverdueInvoices() {
        log.info("Vadesi geçen faturalar istendi");

        try {
            List<Invoice> invoices = invoiceService.getOverdueInvoices();
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            log.error("Vadesi geçen faturalar sorgusu hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Ödenmemiş faturaları listele
     */
    @GetMapping("/unpaid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getUnpaidInvoices() {
        log.info("Ödenmemiş faturalar istendi");

        try {
            List<Invoice> invoices = invoiceService.getInvoicesByPaymentStatus(Invoice.PaymentStatus.UNPAID);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            log.error("Ödenmemiş faturalar sorgusu hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tedarikçiye göre faturaları listele
     */
    @GetMapping("/supplier/{supplierName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getInvoicesBySupplier(@PathVariable String supplierName) {
        log.info("Tedarikçiye göre faturalar istendi: supplier={}", supplierName);

        try {
            List<Invoice> invoices = invoiceService.getInvoicesBySupplier(supplierName);
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            log.error("Tedarikçi faturaları sorgusu hatası: supplier={}, error={}",
                    supplierName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Kullanıcının faturalarını listele
     */
    @GetMapping("/my-invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Invoice>> getMyInvoices(Authentication authentication) {
        log.info("Kullanıcı faturaları istendi: user={}", authentication.getName());

        try {
            List<Invoice> invoices = invoiceService.getInvoicesByUser(authentication.getName());
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            log.error("Kullanıcı faturaları sorgusu hatası: user={}, error={}",
                    authentication.getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Toplam ödenmemiş tutar
     */
    @GetMapping("/total-unpaid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Double> getTotalUnpaidAmount() {
        log.info("Toplam ödenmemiş tutar istendi");

        try {
            Double totalUnpaid = invoiceService.getTotalUnpaidAmount();
            return ResponseEntity.ok(totalUnpaid);
        } catch (Exception e) {
            log.error("Toplam ödenmemiş tutar sorgusu hatası: {}", e.getMessage(), e);
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

    /**
     * 🚀 NEW: Error response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private String message;
        private LocalDateTime timestamp;
        private Long orderId;
        private String details;
    }

    /**
     * 🚀 NEW: Invoice exists response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InvoiceExistsResponse {
        private Long orderId;
        private Boolean hasInvoice;
        private Long invoiceId;
        private String invoiceNumber;
        private LocalDateTime checkTime;
        private String error;
    }
}