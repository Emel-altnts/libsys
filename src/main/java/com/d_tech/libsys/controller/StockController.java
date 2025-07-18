package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.BookStock;
import com.d_tech.libsys.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Stok yönetim controller'ı
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockService stockService;

    /**
     * Kitap için stok kaydı oluştur
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookStock> createBookStock(
            @RequestBody CreateStockRequest request) {

        log.info("Stok kaydı oluşturma isteği: bookId={}, quantity={}",
                request.getBookId(), request.getInitialQuantity());

        try {
            BookStock stock = stockService.createBookStock(
                    request.getBookId(),
                    request.getInitialQuantity(),
                    request.getUnitPrice(),
                    request.getSupplierName()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(stock);

        } catch (Exception e) {
            log.error("Stok kaydı oluşturma hatası: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Asenkron stok kontrolü
     */
    @PostMapping("/check/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AsyncResponse> checkStockAsync(
            @PathVariable Long bookId,
            Authentication authentication) {

        log.info("Asenkron stok kontrolü: bookId={}, user={}",
                bookId, authentication.getName());

        try {
            CompletableFuture<String> future = stockService.checkStockAsync(bookId, authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Stok kontrolü başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Stok kontrolü hatası: bookId={}, error={}", bookId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Stok kontrolü başlatılamadı", null));
        }
    }

    /**
     * Asenkron stok azaltma
     */
    @PostMapping("/decrease/{bookId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<AsyncResponse> decreaseStockAsync(
            @PathVariable Long bookId,
            @RequestBody StockQuantityRequest request,
            Authentication authentication) {

        log.info("Asenkron stok azaltma: bookId={}, quantity={}, user={}",
                bookId, request.getQuantity(), authentication.getName());

        try {
            CompletableFuture<String> future = stockService.decreaseStockAsync(
                    bookId, request.getQuantity(), authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Stok azaltma işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Stok azaltma hatası: bookId={}, error={}", bookId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Stok azaltma işlemi başlatılamadı", null));
        }
    }

    /**
     * Asenkron stok artırma
     */
    @PostMapping("/increase/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> increaseStockAsync(
            @PathVariable Long bookId,
            @RequestBody StockQuantityRequest request,
            Authentication authentication) {

        log.info("Asenkron stok artırma: bookId={}, quantity={}, user={}",
                bookId, request.getQuantity(), authentication.getName());

        try {
            CompletableFuture<String> future = stockService.increaseStockAsync(
                    bookId, request.getQuantity(), authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Stok artırma işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Stok artırma hatası: bookId={}, error={}", bookId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Stok artırma işlemi başlatılamadı", null));
        }
    }

    /**
     * Kitap stok bilgisini getir
     */
    @GetMapping("/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookStock> getBookStock(@PathVariable Long bookId) {
        log.info("Stok bilgisi istendi: bookId={}", bookId);

        Optional<BookStock> stock = stockService.getBookStock(bookId);
        return stock.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Düşük stoklu kitapları listele
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookStock>> getLowStockBooks() {
        log.info("Düşük stoklu kitaplar istendi");

        List<BookStock> lowStockBooks = stockService.getLowStockBooks();
        return ResponseEntity.ok(lowStockBooks);
    }

    /**
     * Yeniden stok gerekli kitapları listele
     */
    @GetMapping("/restock-needed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookStock>> getBooksNeedingRestock() {
        log.info("Yeniden stok gerekli kitaplar istendi");

        List<BookStock> booksNeedingRestock = stockService.getBooksNeedingRestock();
        return ResponseEntity.ok(booksNeedingRestock);
    }

    /**
     * Stok durumuna göre kitapları listele
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookStock>> getBooksByStockStatus(@PathVariable String status) {
        log.info("Stok durumuna göre kitaplar istendi: status={}", status);

        try {
            BookStock.StockStatus stockStatus = BookStock.StockStatus.valueOf(status.toUpperCase());
            List<BookStock> books = stockService.getBooksByStockStatus(stockStatus);
            return ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            log.warn("Geçersiz stok durumu: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tedarikçiye göre stokları listele
     */
    @GetMapping("/supplier/{supplierName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookStock>> getStocksBySupplier(@PathVariable String supplierName) {
        log.info("Tedarikçiye göre stoklar istendi: supplier={}", supplierName);

        List<BookStock> stocks = stockService.getStocksBySupplier(supplierName);
        return ResponseEntity.ok(stocks);
    }

    /**
     * Toplam stok değerini hesapla
     */
    @GetMapping("/total-value")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Double> getTotalStockValue() {
        log.info("Toplam stok değeri istendi");

        Double totalValue = stockService.getTotalStockValue();
        return ResponseEntity.ok(totalValue);
    }

    /**
     * Stok bilgilerini güncelle
     */
    @PutMapping("/{stockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookStock> updateBookStock(
            @PathVariable Long stockId,
            @RequestBody UpdateStockRequest request) {

        log.info("Stok güncelleme isteği: stockId={}", stockId);

        try {
            BookStock updatedStock = stockService.updateBookStock(
                    stockId,
                    request.getMinimumQuantity(),
                    request.getMaximumQuantity(),
                    request.getUnitPrice(),
                    request.getSupplierName(),
                    request.getSupplierContact()
            );

            return ResponseEntity.ok(updatedStock);

        } catch (Exception e) {
            log.error("Stok güncelleme hatası: stockId={}, error={}", stockId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Düşük stok uyarılarını manuel tetikle
     */
    @PostMapping("/alert/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerLowStockAlerts() {
        log.info("Düşük stok uyarıları manuel tetiklendi");

        try {
            stockService.sendLowStockAlerts();
            return ResponseEntity.ok("Düşük stok uyarıları gönderildi");
        } catch (Exception e) {
            log.error("Düşük stok uyarıları gönderme hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Uyarılar gönderilemedi: " + e.getMessage());
        }
    }

    // DTO sınıfları
    @lombok.Data
    public static class CreateStockRequest {
        private Long bookId;
        private Integer initialQuantity;
        private BigDecimal unitPrice;
        private String supplierName;
        private String supplierContact;
    }

    @lombok.Data
    public static class StockQuantityRequest {
        private Integer quantity;
    }

    @lombok.Data
    public static class UpdateStockRequest {
        private Integer minimumQuantity;
        private Integer maximumQuantity;
        private BigDecimal unitPrice;
        private String supplierName;
        private String supplierContact;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AsyncResponse {
        private String message;
        private String eventId;
    }
}