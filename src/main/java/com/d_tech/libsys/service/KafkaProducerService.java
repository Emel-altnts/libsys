// KafkaProducerService'e eklenecek metodlar
package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Güncellenmiş Kafka Producer Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.user-registration:user-registration-topic}")
    private String userRegistrationTopic;

    @Value("${app.kafka.topic.stock-control:stock-control-topic}")
    private String stockControlTopic;

    @Value("${app.kafka.topic.stock-order:stock-order-topic}")
    private String stockOrderTopic;

    @Value("${app.kafka.topic.invoice:invoice-topic}")
    private String invoiceTopic;

    /**
     * Stok kontrol event'ini gönderir
     */
    public CompletableFuture<Boolean> sendStockEvent(StockControlEvent event) {
        log.info("Kafka'ya stok kontrol event'i gönderiliyor: eventId={}, type={}, bookId={}",
                event.getEventId(), event.getEventType(), event.getBookId());

        event.setStatus(StockControlEvent.EventStatus.PENDING);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(stockControlTopic, event.getEventId(), event);

        return future.handle((result, throwable) -> {
            if (throwable != null) {
                log.error("Stok kontrol event'i gönderimi başarısız: eventId={}, error={}",
                        event.getEventId(), throwable.getMessage(), throwable);
                return false;
            } else {
                log.info("Stok kontrol event'i başarıyla gönderildi: eventId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return true;
            }
        });
    }

    /**
     * Stok sipariş event'ini gönderir
     */
    public CompletableFuture<Boolean> sendStockOrderEvent(StockOrderEvent event) {
        log.info("Kafka'ya stok sipariş event'i gönderiliyor: eventId={}, type={}",
                event.getEventId(), event.getEventType());

        event.setStatus(StockOrderEvent.EventStatus.PENDING);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(stockOrderTopic, event.getEventId(), event);

        return future.handle((result, throwable) -> {
            if (throwable != null) {
                log.error("Stok sipariş event'i gönderimi başarısız: eventId={}, error={}",
                        event.getEventId(), throwable.getMessage(), throwable);
                return false;
            } else {
                log.info("Stok sipariş event'i başarıyla gönderildi: eventId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return true;
            }
        });
    }

    /**
     * Fatura event'ini gönderir
     */
    public CompletableFuture<Boolean> sendInvoiceEvent(InvoiceEvent event) {
        log.info("Kafka'ya fatura event'i gönderiliyor: eventId={}, type={}",
                event.getEventId(), event.getEventType());

        event.setStatus(InvoiceEvent.EventStatus.PENDING);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(invoiceTopic, event.getEventId(), event);

        return future.handle((result, throwable) -> {
            if (throwable != null) {
                log.error("Fatura event'i gönderimi başarısız: eventId={}, error={}",
                        event.getEventId(), throwable.getMessage(), throwable);
                return false;
            } else {
                log.info("Fatura event'i başarıyla gönderildi: eventId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return true;
            }
        });
    }

    /**
     * Kullanıcı kayıt event'ini gönderir (mevcut)
     */
    public CompletableFuture<Boolean> sendUserRegistrationEvent(UserRegistrationEvent event) {
        log.info("Kafka'ya kullanıcı kayıt event'i gönderiliyor: eventId={}, username={}",
                event.getEventId(), event.getUsername());

        event.setStatus(UserRegistrationEvent.EventStatus.PENDING);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(userRegistrationTopic, event.getEventId(), event);

        return future.handle((result, throwable) -> {
            if (throwable != null) {
                log.error("Kullanıcı kayıt event'i gönderimi başarısız: eventId={}, error={}",
                        event.getEventId(), throwable.getMessage(), throwable);
                return false;
            } else {
                log.info("Kullanıcı kayıt event'i başarıyla gönderildi: eventId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return true;
            }
        });
    }

    /**
     * Stok event retry gönderimi
     */
    public void sendStockEventRetry(StockControlEvent event) {
        String retryTopic = stockControlTopic + ".retry";
        log.info("Stok kontrol retry event'i gönderiliyor: eventId={}, retryCount={}",
                event.getEventId(), event.getRetryCount());

        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    /**
     * Stok event DLQ gönderimi
     */
    public void sendStockEventToDLQ(StockControlEvent event, String errorReason) {
        String dlqTopic = stockControlTopic + ".dlq";
        event.setMessage("DLQ'ya gönderildi: " + errorReason);
        event.setStatus(StockControlEvent.EventStatus.FAILED);

        log.error("Stok kontrol event'i DLQ'ya gönderiliyor: eventId={}, reason={}",
                event.getEventId(), errorReason);

        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }

    /**
     * Sipariş event retry gönderimi
     */
    public void sendStockOrderEventRetry(StockOrderEvent event) {
        String retryTopic = stockOrderTopic + ".retry";
        log.info("Stok sipariş retry event'i gönderiliyor: eventId={}, retryCount={}",
                event.getEventId(), event.getRetryCount());

        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    /**
     * Sipariş event DLQ gönderimi
     */
    public void sendStockOrderEventToDLQ(StockOrderEvent event, String errorReason) {
        String dlqTopic = stockOrderTopic + ".dlq";
        event.setMessage("DLQ'ya gönderildi: " + errorReason);
        event.setStatus(StockOrderEvent.EventStatus.FAILED);

        log.error("Stok sipariş event'i DLQ'ya gönderiliyor: eventId={}, reason={}",
                event.getEventId(), errorReason);

        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }

    /**
     * Kullanıcı kayıt retry gönderimi (mevcut)
     */
    public void sendRetryEvent(UserRegistrationEvent event) {
        String retryTopic = userRegistrationTopic + ".retry";
        log.info("Kullanıcı kayıt retry event'i gönderiliyor: eventId={}, retryCount={}",
                event.getEventId(), event.getRetryCount());

        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    /**
     * Kullanıcı kayıt DLQ gönderimi (mevcut)
     */
    public void sendToDLQ(UserRegistrationEvent event, String errorReason) {
        String dlqTopic = userRegistrationTopic + ".dlq";
        event.setMessage("DLQ'ya gönderildi: " + errorReason);
        event.setStatus(UserRegistrationEvent.EventStatus.FAILED);

        log.error("Kullanıcı kayıt event'i DLQ'ya gönderiliyor: eventId={}, reason={}",
                event.getEventId(), errorReason);

        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }

    /**
     * Genel amaçlı mesaj gönderme metodu
     */
    public void sendMessage(String topic, String key, Object message) {
        log.info("Kafka'ya mesaj gönderiliyor: topic={}, key={}", topic, key);

        kafkaTemplate.send(topic, key, message)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Mesaj gönderimi başarısız: topic={}, key={}, error={}",
                                topic, key, throwable.getMessage());
                    } else {
                        log.info("Mesaj başarıyla gönderildi: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}