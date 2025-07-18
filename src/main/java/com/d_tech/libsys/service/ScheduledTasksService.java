package com.d_tech.libsys.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Zamanlanmış görevler servisi
 * Stok kontrolü ve diğer otomatik işlemler için
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final StockService stockService;
    private final EventTrackingService eventTrackingService;

    /**
     * Her saat düşük stok kontrolü yapar
     */
    @Scheduled(fixedRate = 3600000) // Her saat (3600000 ms)
    public void checkLowStock() {
        log.info("Zamanlanmış düşük stok kontrolü başlatılıyor...");

        try {
            stockService.sendLowStockAlerts();
            log.info("Düşük stok kontrolü tamamlandı");
        } catch (Exception e) {
            log.error("Düşük stok kontrolü hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Her gece yarısı eski event'leri temizler
     */
    @Scheduled(cron = "0 0 0 * * *") // Her gece 00:00
    public void cleanupOldEvents() {
        log.info("Zamanlanmış event temizliği başlatılıyor...");

        try {
            eventTrackingService.cleanupStaleEvents();
            log.info("Event temizliği tamamlandı");
        } catch (Exception e) {
            log.error("Event temizliği hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Her pazartesi 09:00'da haftalık stok raporu hazırlar
     */
    @Scheduled(cron = "0 0 9 * * MON") // Her pazartesi 09:00
    public void generateWeeklyStockReport() {
        log.info("Haftalık stok raporu hazırlanıyor...");

        try {
            // Haftalık stok raporu logic'i burada olacak
            var lowStockBooks = stockService.getLowStockBooks();
            var booksNeedingRestock = stockService.getBooksNeedingRestock();

            log.info("Haftalık stok raporu: {} düşük stoklu kitap, {} yeniden stok gerekli kitap",
                    lowStockBooks.size(), booksNeedingRestock.size());

            // Burada email ile rapor gönderilebilir
            // emailService.sendWeeklyStockReport(lowStockBooks, booksNeedingRestock);

        } catch (Exception e) {
            log.error("Haftalık stok raporu hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Her 15 dakikada bir sistem durumu kontrolü
     */
    @Scheduled(fixedRate = 900000) // Her 15 dakika (900000 ms)
    public void systemHealthCheck() {
        log.debug("Sistem durumu kontrolü yapılıyor...");

        try {
            // Sistem durumu kontrolü
            var stats = eventTrackingService.getEventStatistics();

            // Eğer çok fazla başarısız event varsa uyarı
            if (stats.getSuccessRate() < 80.0) {
                log.warn("Düşük başarı oranı tespit edildi: {}%", stats.getSuccessRate());
                // Alert gönder
            }

            // Eğer çok fazla bekleyen event varsa uyarı
            if (stats.getPendingEvents() + stats.getProcessingEvents() > 100) {
                log.warn("Yüksek sayıda bekleyen event: pending={}, processing={}",
                        stats.getPendingEvents(), stats.getProcessingEvents());
                // Alert gönder
            }

        } catch (Exception e) {
            log.error("Sistem durumu kontrolü hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Her ayın 1'inde aylık istatistikler hazırlar
     */
    @Scheduled(cron = "0 0 6 1 * *") // Her ayın 1'i 06:00
    public void generateMonthlyStatistics() {
        log.info("Aylık istatistikler hazırlanıyor...");

        try {
            // Aylık istatistikler
            var totalStockValue = stockService.getTotalStockValue();
            var eventStats = eventTrackingService.getEventStatistics();

            log.info("Aylık istatistikler: Toplam stok değeri={}, Event başarı oranı={}%",
                    totalStockValue, eventStats.getSuccessRate());

            // Burada database'e istatistik kayıt edilebilir
            // monthlyStatisticsService.saveStatistics(totalStockValue, eventStats);

        } catch (Exception e) {
            log.error("Aylık istatistikler hatası: {}", e.getMessage(), e);
        }
    }
}
