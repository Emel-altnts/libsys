package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.BookStock;
import com.d_tech.libsys.dto.StockControlEvent;
import com.d_tech.libsys.repository.BookStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stok kontrol event'lerini işleyen Kafka Consumer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockControlConsumer {

    private final BookStockRepository bookStockRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Stok kontrol event'lerini işler
     */
    @KafkaListener(
            topics = "${app.kafka.topic.stock-control:stock-control-topic}",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleStockControlEvent(
            @Payload StockControlEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Stok kontrol event'i alındı: eventId={}, type={}, bookId={}, partition={}, offset={}",
                event.getEventId(), event.getEventType(), event.getBookId(), partition, offset);

        try {
            // Event'i processing durumuna getir
            event.setStatus(StockControlEvent.EventStatus.PROCESSING);

            // Event tipine göre işlem yap
            switch (event.getEventType()) {
                case STOCK_CHECK -> handleStockCheck(event);
                case STOCK_DECREASE -> handleStockDecrease(event);
                case STOCK_INCREASE -> handleStockIncrease(event);
                case LOW_STOCK_ALERT -> handleLowStockAlert(event);
                case OUT_OF_STOCK_ALERT -> handleOutOfStockAlert(event);
                default -> {
                    log.warn("Bilinmeyen event tipi: {}", event.getEventType());
                    event.setStatus(StockControlEvent.EventStatus.FAILED);
                    event.setMessage("Bilinmeyen event tipi");
                }
            }

            // Başarılı tamamlandıysa commit et
            if (event.getStatus() == StockControlEvent.EventStatus.COMPLETED) {
                acknowledgment.acknowledge();
                log.info("Stok kontrol event'i başarıyla işlendi: eventId={}", event.getEventId());
            } else {
                // Hata durumunda retry
                handleStockControlError(event, new RuntimeException(event.getMessage()), acknowledgment);
            }

        } catch (Exception e) {
            log.error("Stok kontrol event'i işlenirken hata: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            handleStockControlError(event, e, acknowledgment);
        }
    }

    /**
     * Stok kontrolü işlemi
     */
    private void handleStockCheck(StockControlEvent event) {
        log.info("Stok kontrolü yapılıyor: bookId={}", event.getBookId());

        Optional<BookStock> stockOpt = bookStockRepository.findByBookId(event.getBookId());

        if (stockOpt.isPresent()) {
            BookStock stock = stockOpt.get();
            event.setStatus(StockControlEvent.EventStatus.COMPLETED);
            event.setMessage(String.format("Stok kontrolü tamamlandı. Mevcut miktar: %d, Durum: %s",
                    stock.getCurrentQuantity(), stock.getStatus()));

            // Düşük stok uyarısı gönder
            if (stock.isRestockNeeded()) {
                sendLowStockAlert(stock);
            }

        } else {
            event.setStatus(StockControlEvent.EventStatus.FAILED);
            event.setMessage("Kitap için stok kaydı bulunamadı");
        }
    }

    /**
     * Stok azaltma işlemi
     */
    private void handleStockDecrease(StockControlEvent event) {
        log.info("Stok azaltılıyor: bookId={}, quantity={}", event.getBookId(), event.getQuantity());

        Optional<BookStock> stockOpt = bookStockRepository.findByBookId(event.getBookId());

        if (stockOpt.isPresent()) {
            BookStock stock = stockOpt.get();

            if (stock.decreaseStock(event.getQuantity())) {
                bookStockRepository.save(stock);
                event.setStatus(StockControlEvent.EventStatus.COMPLETED);
                event.setMessage(String.format("Stok başarıyla azaltıldı. Yeni miktar: %d", stock.getCurrentQuantity()));

                // Düşük stok kontrolü
                if (stock.isRestockNeeded()) {
                    sendLowStockAlert(stock);
                }

            } else {
                event.setStatus(StockControlEvent.EventStatus.FAILED);
                event.setMessage(String.format("Yetersiz stok. Mevcut: %d, İstenen: %d",
                        stock.getCurrentQuantity(), event.getQuantity()));
            }

        } else {
            event.setStatus(StockControlEvent.EventStatus.FAILED);
            event.setMessage("Kitap için stok kaydı bulunamadı");
        }
    }

    /**
     * Stok artırma işlemi
     */
    private void handleStockIncrease(StockControlEvent event) {
        log.info("Stok artırılıyor: bookId={}, quantity={}", event.getBookId(), event.getQuantity());

        Optional<BookStock> stockOpt = bookStockRepository.findByBookId(event.getBookId());

        if (stockOpt.isPresent()) {
            BookStock stock = stockOpt.get();
            stock.increaseStock(event.getQuantity());
            bookStockRepository.save(stock);

            event.setStatus(StockControlEvent.EventStatus.COMPLETED);
            event.setMessage(String.format("Stok başarıyla artırıldı. Yeni miktar: %d", stock.getCurrentQuantity()));

        } else {
            event.setStatus(StockControlEvent.EventStatus.FAILED);
            event.setMessage("Kitap için stok kaydı bulunamadı");
        }
    }

    /**
     * Düşük stok uyarısı işlemi
     */
    private void handleLowStockAlert(StockControlEvent event) {
        log.warn("Düşük stok uyarısı: bookId={}, message={}", event.getBookId(), event.getMessage());

        // Burada email, SMS, push notification vs. gönderilebilir
        // Admin paneline bildirim gönderilebilir
        // Otomatik sipariş oluşturulabilir

        event.setStatus(StockControlEvent.EventStatus.COMPLETED);
        event.setMessage("Düşük stok uyarısı işlendi");
    }

    /**
     * Stok tükendi uyarısı işlemi
     */
    private void handleOutOfStockAlert(StockControlEvent event) {
        log.error("Stok tükendi uyarısı: bookId={}, message={}", event.getBookId(), event.getMessage());

        // Kritik uyarı gönder
        // Acil sipariş oluştur
        // Satışları durdur

        event.setStatus(StockControlEvent.EventStatus.COMPLETED);
        event.setMessage("Stok tükendi uyarısı işlendi");
    }

    /**
     * Düşük stok uyarısı gönder
     */
    private void sendLowStockAlert(BookStock stock) {
        StockControlEvent alertEvent = StockControlEvent.builder()
                .eventId("ALERT_" + System.currentTimeMillis())
                .eventType(stock.getStatus() == BookStock.StockStatus.OUT_OF_STOCK ?
                        StockControlEvent.EventType.OUT_OF_STOCK_ALERT :
                        StockControlEvent.EventType.LOW_STOCK_ALERT)
                .bookId(stock.getBook().getId())
                .quantity(stock.getCurrentQuantity())
                .userId("SYSTEM")
                .message(String.format("%s - Mevcut: %d, Minimum: %d, Önerilen sipariş: %d",
                        stock.getBook().getTitle(),
                        stock.getCurrentQuantity(),
                        stock.getMinimumQuantity(),
                        stock.getRecommendedOrderQuantity()))
                .build();

        kafkaProducerService.sendStockEvent(alertEvent);
    }

    /**
     * Stok kontrol hatası işleme
     */
    private void handleStockControlError(StockControlEvent event, Exception error, Acknowledgment acknowledgment) {
        event.incrementRetry();
        event.setMessage("Hata: " + error.getMessage());

        if (event.canRetry()) {
            log.warn("Stok kontrol event'i retry edilecek: eventId={}, retryCount={}, error={}",
                    event.getEventId(), event.getRetryCount(), error.getMessage());

            kafkaProducerService.sendStockEventRetry(event);
            acknowledgment.acknowledge();
        } else {
            log.error("Stok kontrol event'i maximum retry'a ulaştı: eventId={}, error={}",
                    event.getEventId(), error.getMessage());

            kafkaProducerService.sendStockEventToDLQ(event, error.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * Retry topic'ten gelen stok event'lerini işler
     */
    @KafkaListener(
            topics = "${app.kafka.topic.stock-control:stock-control-topic}.retry",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}.retry",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleStockControlRetry(
            @Payload StockControlEvent event,
            Acknowledgment acknowledgment) {

        log.info("Stok kontrol retry event'i alındı: eventId={}, retryCount={}",
                event.getEventId(), event.getRetryCount());

        try {
            // Kısa bekleme (exponential backoff)
            long waitTime = (long) Math.pow(2, event.getRetryCount()) * 1000;
            Thread.sleep(Math.min(waitTime, 30000));

            // Ana işlemi tekrar çalıştır
            handleStockControlEvent(event, 0, 0, acknowledgment);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            handleStockControlError(event, e, acknowledgment);
        }
    }
}

