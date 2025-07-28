package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.StockOrder;
import com.d_tech.libsys.dto.StockOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🚀 UPDATED: Stok sipariş event'lerini işleyen Kafka Consumer - SHIP_ORDER handler eklendi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderConsumer {

    private final StockOrderService stockOrderService;
    private final InvoiceService invoiceService;
    private final KafkaProducerService kafkaProducerService;

    /**
     * 🚀 FIXED: Acknowledgment parametresi kaldırıldı - AUTO_COMMIT kullanılıyor
     */
    @KafkaListener(
            topics = "${app.kafka.topic.stock-order:stock-order-topic}",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleStockOrderEvent(
            @Payload StockOrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("✅ Stok sipariş event'i alındı: eventId={}, type={}, orderId={}, partition={}, offset={}",
                event.getEventId(), event.getEventType(), event.getOrderId(), partition, offset);

        try {
            event.setStatus(StockOrderEvent.EventStatus.PROCESSING);

            switch (event.getEventType()) {
                case CREATE_ORDER -> handleCreateOrder(event);
                case CONFIRM_ORDER -> handleConfirmOrder(event);
                case SHIP_ORDER -> handleShipOrder(event);  // 🚀 YENİ
                case CANCEL_ORDER -> handleCancelOrder(event);
                case RECEIVE_ORDER -> handleReceiveOrder(event);
                case GENERATE_INVOICE -> handleGenerateInvoice(event);
                default -> {
                    log.warn("⚠️ Bilinmeyen sipariş event tipi: {}", event.getEventType());
                    event.setStatus(StockOrderEvent.EventStatus.FAILED);
                    event.setMessage("Bilinmeyen event tipi");
                }
            }

            // ✅ Auto-commit ile başarı durumu
            if (event.getStatus() == StockOrderEvent.EventStatus.COMPLETED) {
                log.info("✅ Stok sipariş event'i başarıyla işlendi: eventId={}", event.getEventId());
            } else {
                log.error("❌ Stok sipariş event'i başarısız: eventId={}, message={}",
                        event.getEventId(), event.getMessage());
                handleStockOrderError(event, new RuntimeException(event.getMessage()));
            }

        } catch (Exception e) {
            log.error("💥 Stok sipariş event'i işlenirken hata: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            handleStockOrderError(event, e);
        }
    }

    /**
     * ✅ Sipariş oluşturma işlemi
     */
    private void handleCreateOrder(StockOrderEvent event) {
        log.info("📦 Sipariş oluşturuluyor: eventId={}", event.getEventId());

        try {
            StockOrder order = stockOrderService.createOrder(event.getOrderRequest());
            event.setOrderId(order.getId());
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş başarıyla oluşturuldu: " + order.getOrderNumber());

            log.info("✅ Sipariş oluşturuldu: orderId={}, orderNumber={}, supplier={}",
                    order.getId(), order.getOrderNumber(), order.getSupplierName());

        } catch (Exception e) {
            log.error("❌ Sipariş oluşturma hatası: {}", e.getMessage(), e);
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş oluşturma hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Sipariş onaylama işlemi
     */
    private void handleConfirmOrder(StockOrderEvent event) {
        log.info("✅ Sipariş onaylanıyor: orderId={}", event.getOrderId());

        try {
            StockOrder order = stockOrderService.confirmOrder(event.getOrderId());
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş onaylandı: " + order.getOrderNumber());

            log.info("✅ Sipariş onaylandı: orderId={}, status={}", event.getOrderId(), order.getStatus());

        } catch (Exception e) {
            log.error("❌ Sipariş onaylama hatası: {}", e.getMessage(), e);
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş onaylama hatası: " + e.getMessage());
        }
    }

    /**
     * 🚀 YENİ: Sipariş kargoya verme işlemi (CONFIRMED → SHIPPED)
     */
    private void handleShipOrder(StockOrderEvent event) {
        log.info("🚚 Sipariş kargoya veriliyor: orderId={}", event.getOrderId());

        try {
            StockOrder order = stockOrderService.shipOrder(event.getOrderId());
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş kargoya verildi: " + order.getOrderNumber());

            log.info("✅ Sipariş kargoya verildi: orderId={}, status={}", event.getOrderId(), order.getStatus());

        } catch (Exception e) {
            log.error("❌ Sipariş kargoya verme hatası: {}", e.getMessage(), e);
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş kargoya verme hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Sipariş iptal etme işlemi
     */
    private void handleCancelOrder(StockOrderEvent event) {
        log.info("🚫 Sipariş iptal ediliyor: orderId={}", event.getOrderId());

        try {
            StockOrder order = stockOrderService.cancelOrder(event.getOrderId(), "Kafka event ile iptal");
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş iptal edildi: " + order.getOrderNumber());

            log.info("✅ Sipariş iptal edildi: orderId={}, status={}", event.getOrderId(), order.getStatus());

        } catch (Exception e) {
            log.error("❌ Sipariş iptal hatası: {}", e.getMessage(), e);
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş iptal hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Sipariş teslimat alma işlemi
     */
    private void handleReceiveOrder(StockOrderEvent event) {
        log.info("📦 Sipariş teslimatı alınıyor: orderId={}", event.getOrderId());

        try {
            // ✅ OrderItems'ları al ve tam teslimat olarak işaretle
            var orderItems = stockOrderService.getOrderItemsForDelivery(event.getOrderId());

            if (orderItems.isEmpty()) {
                throw new IllegalStateException("Sipariş kalemleri bulunamadı: " + event.getOrderId());
            }

            // Tüm kalemleri tam teslimat olarak işaretle
            var receiptItems = orderItems.stream()
                    .map(item -> com.d_tech.libsys.dto.StockReceiptItem.builder()
                            .orderItemId(item.getId())
                            .receivedQuantity(item.getQuantity()) // Tam teslimat
                            .notes("Otomatik tam teslimat")
                            .build())
                    .toList();

            StockOrder order = stockOrderService.receiveOrder(event.getOrderId(), receiptItems);
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş teslimatı alındı: " + order.getOrderNumber() +
                    " (Status: " + order.getStatus() + ")");

            log.info("✅ Sipariş teslimatı kabul edildi: orderId={}, status={}",
                    event.getOrderId(), order.getStatus());

        } catch (Exception e) {
            log.error("❌ Teslimat alma hatası: {}", e.getMessage(), e);
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Teslimat alma hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ Fatura oluşturma işlemi
     */
    private void handleGenerateInvoice(StockOrderEvent event) {
        log.info("🧾 Fatura oluşturuluyor: orderId={}", event.getOrderId());

        try {
            // Default invoice request oluştur
            com.d_tech.libsys.dto.InvoiceRequest invoiceRequest = com.d_tech.libsys.dto.InvoiceRequest.builder()
                    .createdBy("SYSTEM")
                    .notes("Otomatik oluşturulan fatura")
                    .build();

            var invoice = invoiceService.generateInvoice(event.getOrderId(), invoiceRequest);
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Fatura oluşturuldu: " + invoice.getInvoiceNumber());

            log.info("✅ Fatura oluşturuldu: invoiceId={}, invoiceNumber={}",
                    invoice.getId(), invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("❌ Fatura oluşturma hatası: {}", e.getMessage(), e);
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Fatura oluşturma hatası: " + e.getMessage());
        }
    }

    /**
     * ✅ FIXED: Acknowledgment parametresi kaldırıldı
     */
    private void handleStockOrderError(StockOrderEvent event, Exception error) {
        event.incrementRetry();
        event.setMessage("Hata: " + error.getMessage());

        if (event.canRetry()) {
            log.warn("🔄 Stok sipariş event'i retry edilecek: eventId={}, retryCount={}",
                    event.getEventId(), event.getRetryCount());

            kafkaProducerService.sendStockOrderEventRetry(event);
        } else {
            log.error("💀 Stok sipariş event'i maximum retry'a ulaştı: eventId={}",
                    event.getEventId());

            kafkaProducerService.sendStockOrderEventToDLQ(event, error.getMessage());
        }
    }

    /**
     * ✅ FIXED: Retry consumer - Acknowledgment kaldırıldı
     */
    @KafkaListener(
            topics = "${app.kafka.topic.stock-order:stock-order-topic}.retry",
            groupId = "${spring.kafka.consumer.group-id:libsys-group}.retry",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleStockOrderRetry(
            @Payload StockOrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("🔄 Stok sipariş retry event'i alındı: eventId={}, retryCount={}, partition={}, offset={}",
                event.getEventId(), event.getRetryCount(), partition, offset);

        try {
            // Exponential backoff
            long waitTime = (long) Math.pow(2, event.getRetryCount()) * 1000;
            Thread.sleep(Math.min(waitTime, 30000));

            // Ana işlemi tekrar çalıştır
            handleStockOrderEvent(event, partition, offset);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🛑 Retry işlemi kesildi: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("💥 Retry işleminde hata: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            handleStockOrderError(event, e);
        }
    }
}