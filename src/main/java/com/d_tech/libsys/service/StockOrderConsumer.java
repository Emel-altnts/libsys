package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.StockOrder;
import com.d_tech.libsys.dto.StockOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stok sipariş event'lerini işleyen Kafka Consumer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderConsumer {

    private final StockOrderService stockOrderService;
    private final InvoiceService invoiceService;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Stok sipariş event'lerini işler
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
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Stok sipariş event'i alındı: eventId={}, type={}, orderId={}, partition={}, offset={}",
                event.getEventId(), event.getEventType(), event.getOrderId(), partition, offset);

        try {
            event.setStatus(StockOrderEvent.EventStatus.PROCESSING);

            switch (event.getEventType()) {
                case CREATE_ORDER -> handleCreateOrder(event);
                case CONFIRM_ORDER -> handleConfirmOrder(event);
                case CANCEL_ORDER -> handleCancelOrder(event);
                case RECEIVE_ORDER -> handleReceiveOrder(event);
                case GENERATE_INVOICE -> handleGenerateInvoice(event);
                default -> {
                    log.warn("Bilinmeyen sipariş event tipi: {}", event.getEventType());
                    event.setStatus(StockOrderEvent.EventStatus.FAILED);
                    event.setMessage("Bilinmeyen event tipi");
                }
            }

            if (event.getStatus() == StockOrderEvent.EventStatus.COMPLETED) {
                acknowledgment.acknowledge();
                log.info("Stok sipariş event'i başarıyla işlendi: eventId={}", event.getEventId());
            } else {
                handleStockOrderError(event, new RuntimeException(event.getMessage()), acknowledgment);
            }

        } catch (Exception e) {
            log.error("Stok sipariş event'i işlenirken hata: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            handleStockOrderError(event, e, acknowledgment);
        }
    }

    /**
     * Sipariş oluşturma işlemi
     */
    private void handleCreateOrder(StockOrderEvent event) {
        log.info("Sipariş oluşturuluyor: eventId={}", event.getEventId());

        try {
            StockOrder order = stockOrderService.createOrder(event.getOrderRequest());
            event.setOrderId(order.getId());
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş başarıyla oluşturuldu: " + order.getOrderNumber());

        } catch (Exception e) {
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş oluşturma hatası: " + e.getMessage());
        }
    }

    /**
     * Sipariş onaylama işlemi
     */
    private void handleConfirmOrder(StockOrderEvent event) {
        log.info("Sipariş onaylanıyor: orderId={}", event.getOrderId());

        try {
            StockOrder order = stockOrderService.confirmOrder(event.getOrderId());
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş onaylandı: " + order.getOrderNumber());

        } catch (Exception e) {
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş onaylama hatası: " + e.getMessage());
        }
    }

    /**
     * Sipariş iptal etme işlemi
     */
    private void handleCancelOrder(StockOrderEvent event) {
        log.info("Sipariş iptal ediliyor: orderId={}", event.getOrderId());

        try {
            StockOrder order = stockOrderService.cancelOrder(event.getOrderId(), "Kafka event ile iptal");
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş iptal edildi: " + order.getOrderNumber());

        } catch (Exception e) {
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Sipariş iptal hatası: " + e.getMessage());
        }
    }

    /**
     * Sipariş teslimat alma işlemi
     */
    private void handleReceiveOrder(StockOrderEvent event) {
        log.info("Sipariş teslimatı alınıyor: orderId={}", event.getOrderId());

        try {
            // Bu örnek implementasyonda tüm kalemleri tam teslimat olarak kabul ediyoruz
            // Gerçek uygulamada event içinden teslimat detayları alınmalı

            // Burada teslimat detayları parse edilmeli
            // StockOrder order = stockOrderService.receiveOrder(event.getOrderId(), receiptItems);

            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Sipariş teslimatı alındı");

        } catch (Exception e) {
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Teslimat alma hatası: " + e.getMessage());
        }
    }

    /**
     * Fatura oluşturma işlemi
     */
    private void handleGenerateInvoice(StockOrderEvent event) {
        log.info("Fatura oluşturuluyor: orderId={}", event.getOrderId());

        try {
            // Default invoice request oluştur
            com.d_tech.libsys.dto.InvoiceRequest invoiceRequest = com.d_tech.libsys.dto.InvoiceRequest.builder()
                    .createdBy("SYSTEM")
                    .notes("Otomatik oluşturulan fatura")
                    .build();

            var invoice = invoiceService.generateInvoice(event.getOrderId(), invoiceRequest);
            event.setStatus(StockOrderEvent.EventStatus.COMPLETED);
            event.setMessage("Fatura oluşturuldu: " + invoice.getInvoiceNumber());

        } catch (Exception e) {
            event.setStatus(StockOrderEvent.EventStatus.FAILED);
            event.setMessage("Fatura oluşturma hatası: " + e.getMessage());
        }
    }

    /**
     * Sipariş hatası işleme
     */
    private void handleStockOrderError(StockOrderEvent event, Exception error, Acknowledgment acknowledgment) {
        event.incrementRetry();
        event.setMessage("Hata: " + error.getMessage());

        if (event.canRetry()) {
            log.warn("Stok sipariş event'i retry edilecek: eventId={}, retryCount={}",
                    event.getEventId(), event.getRetryCount());

            kafkaProducerService.sendStockOrderEventRetry(event);
            acknowledgment.acknowledge();
        } else {
            log.error("Stok sipariş event'i maximum retry'a ulaştı: eventId={}",
                    event.getEventId());

            kafkaProducerService.sendStockOrderEventToDLQ(event, error.getMessage());
            acknowledgment.acknowledge();
        }
    }
}

