package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.Book;
import com.d_tech.libsys.domain.model.BookStock;
import com.d_tech.libsys.dto.StockControlEvent;
import com.d_tech.libsys.repository.BookRepository;
import com.d_tech.libsys.repository.BookStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Stok yönetim servisi
 * Kitap stoklarını kontrol eder ve Kafka event'leri gönderir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final BookStockRepository bookStockRepository;
    private final BookRepository bookRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Kitap için stok kaydı oluşturur
     */
    @Transactional
    public BookStock createBookStock(Long bookId, Integer initialQuantity, BigDecimal unitPrice, String supplierName) {
        log.info("Kitap için stok kaydı oluşturuluyor: bookId={}, quantity={}, supplier={}",
                bookId, initialQuantity, supplierName);

        // Kitap var mı kontrol et
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Kitap bulunamadı: " + bookId));

        // Zaten stok kaydı var mı kontrol et
        if (bookStockRepository.existsByBookId(bookId)) {
            throw new IllegalStateException("Bu kitap için zaten stok kaydı mevcut: " + bookId);
        }

        // Stok kaydı oluştur
        BookStock bookStock = BookStock.builder()
                .book(book)
                .currentQuantity(initialQuantity)
                .unitPrice(unitPrice)
                .supplierName(supplierName)
                .build();

        BookStock savedStock = bookStockRepository.save(bookStock);
        log.info("Stok kaydı oluşturuldu: bookId={}, stockId={}", bookId, savedStock.getId());

        return savedStock;
    }

    /**
     * Asenkron stok kontrolü - Kafka event gönderir
     */
    public CompletableFuture<String> checkStockAsync(Long bookId, String userId) {
        log.info("Asenkron stok kontrolü başlatılıyor: bookId={}, userId={}", bookId, userId);

        try {
            // Event oluştur
            StockControlEvent event = StockControlEvent.builder()
                    .eventId(generateEventId("STOCK_CHECK"))
                    .eventType(StockControlEvent.EventType.STOCK_CHECK)
                    .bookId(bookId)
                    .userId(userId)
                    .build();

            // Kafka'ya gönder
            return kafkaProducerService.sendStockEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Stok kontrol event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            log.error("Stok kontrol event'i gönderilemedi: bookId={}", bookId);
                            throw new RuntimeException("Stok kontrol event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Asenkron stok kontrolü hatası: bookId={}, error={}", bookId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Asenkron stok azaltma - Kafka event gönderir
     */
    public CompletableFuture<String> decreaseStockAsync(Long bookId, Integer quantity, String userId) {
        log.info("Asenkron stok azaltma başlatılıyor: bookId={}, quantity={}, userId={}", bookId, quantity, userId);

        try {
            // Temel validasyonlar
            if (quantity <= 0) {
                throw new IllegalArgumentException("Miktar pozitif olmalıdır");
            }

            // Event oluştur
            StockControlEvent event = StockControlEvent.builder()
                    .eventId(generateEventId("STOCK_DECREASE"))
                    .eventType(StockControlEvent.EventType.STOCK_DECREASE)
                    .bookId(bookId)
                    .quantity(quantity)
                    .userId(userId)
                    .build();

            // Kafka'ya gönder
            return kafkaProducerService.sendStockEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Stok azaltma event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            log.error("Stok azaltma event'i gönderilemedi: bookId={}", bookId);
                            throw new RuntimeException("Stok azaltma event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Asenkron stok azaltma hatası: bookId={}, error={}", bookId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Asenkron stok artırma - Kafka event gönderir
     */
    public CompletableFuture<String> increaseStockAsync(Long bookId, Integer quantity, String userId) {
        log.info("Asenkron stok artırma başlatılıyor: bookId={}, quantity={}, userId={}", bookId, quantity, userId);

        try {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Miktar pozitif olmalıdır");
            }

            StockControlEvent event = StockControlEvent.builder()
                    .eventId(generateEventId("STOCK_INCREASE"))
                    .eventType(StockControlEvent.EventType.STOCK_INCREASE)
                    .bookId(bookId)
                    .quantity(quantity)
                    .userId(userId)
                    .build();

            return kafkaProducerService.sendStockEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Stok artırma event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            log.error("Stok artırma event'i gönderilemedi: bookId={}", bookId);
                            throw new RuntimeException("Stok artırma event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Asenkron stok artırma hatası: bookId={}, error={}", bookId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Senkron stok bilgisi getir
     */
    public Optional<BookStock> getBookStock(Long bookId) {
        log.info("Stok bilgisi getiriliyor: bookId={}", bookId);
        return bookStockRepository.findByBookId(bookId);
    }

    /**
     * Düşük stoklu kitapları listele
     */
    public List<BookStock> getLowStockBooks() {
        log.info("Düşük stoklu kitaplar listeleniyor");
        return bookStockRepository.findLowStockBooks();
    }

    /**
     * Yeniden stok gerekli kitapları listele
     */
    public List<BookStock> getBooksNeedingRestock() {
        log.info("Yeniden stok gerekli kitaplar listeleniyor");
        return bookStockRepository.findBooksNeedingRestock();
    }

    /**
     * Toplam stok değerini hesapla
     */
    public Double getTotalStockValue() {
        log.info("Toplam stok değeri hesaplanıyor");
        return bookStockRepository.calculateTotalStockValue();
    }

    /**
     * Stok durumuna göre kitapları listele
     */
    public List<BookStock> getBooksByStockStatus(BookStock.StockStatus status) {
        log.info("Stok durumuna göre kitaplar listeleniyor: status={}", status);
        return bookStockRepository.findByStatus(status);
    }

    /**
     * Tedarikçiye göre stokları listele
     */
    public List<BookStock> getStocksBySupplier(String supplierName) {
        log.info("Tedarikçiye göre stoklar listeleniyor: supplier={}", supplierName);
        return bookStockRepository.findBySupplierNameContainingIgnoreCase(supplierName);
    }

    /**
     * Stok bilgilerini güncelle
     */
    @Transactional
    public BookStock updateBookStock(Long stockId, Integer minQuantity, Integer maxQuantity,
                                     BigDecimal unitPrice, String supplierName, String supplierContact) {
        log.info("Stok bilgileri güncelleniyor: stockId={}", stockId);

        BookStock stock = bookStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stok kaydı bulunamadı: " + stockId));

        // Güncelleme
        if (minQuantity != null) stock.setMinimumQuantity(minQuantity);
        if (maxQuantity != null) stock.setMaximumQuantity(maxQuantity);
        if (unitPrice != null) stock.setUnitPrice(unitPrice);
        if (supplierName != null) stock.setSupplierName(supplierName);
        if (supplierContact != null) stock.setSupplierContact(supplierContact);

        BookStock updatedStock = bookStockRepository.save(stock);
        log.info("Stok bilgileri güncellendi: stockId={}", stockId);

        return updatedStock;
    }

    /**
     * Düşük stok uyarısı gönder (scheduled task tarafından çağrılabilir)
     */
    public void sendLowStockAlerts() {
        log.info("Düşük stok uyarıları kontrol ediliyor");

        List<BookStock> lowStockBooks = getLowStockBooks();

        for (BookStock stock : lowStockBooks) {
            StockControlEvent event = StockControlEvent.builder()
                    .eventId(generateEventId("LOW_STOCK_ALERT"))
                    .eventType(StockControlEvent.EventType.LOW_STOCK_ALERT)
                    .bookId(stock.getBook().getId())
                    .quantity(stock.getCurrentQuantity())
                    .userId("SYSTEM")
                    .message(String.format("Düşük stok uyarısı: %s - Mevcut: %d, Minimum: %d",
                            stock.getBook().getTitle(), stock.getCurrentQuantity(), stock.getMinimumQuantity()))
                    .build();

            kafkaProducerService.sendStockEvent(event);
            log.warn("Düşük stok uyarısı gönderildi: bookId={}, currentQty={}, minQty={}",
                    stock.getBook().getId(), stock.getCurrentQuantity(), stock.getMinimumQuantity());
        }
    }

    /**
     * Event ID oluşturucu
     */
    private String generateEventId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}