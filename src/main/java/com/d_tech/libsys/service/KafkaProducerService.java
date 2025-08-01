package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Conditional Kafka Producer Service - only active when Kafka is enabled
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaProducerService {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${app.kafka.topic.user-registration:user-registration-topic}")
    private String userRegistrationTopic;

    @Value("${app.kafka.topic.stock-control:stock-control-topic}")
    private String stockControlTopic;

    @Value("${app.kafka.topic.stock-order:stock-order-topic}")
    private String stockOrderTopic;

    @Value("${app.kafka.topic.invoice:invoice-topic}")
    private String invoiceTopic;

    public CompletableFuture<Boolean> sendUserRegistrationEvent(UserRegistrationEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send user registration event: eventId={}", event.getEventId());
            return CompletableFuture.completedFuture(false);
        }

        log.info("Sending user registration event to Kafka: eventId={}, username={}",
                event.getEventId(), event.getUsername());

        event.setStatus(UserRegistrationEvent.EventStatus.PENDING);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(userRegistrationTopic, event.getEventId(), event);

            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("User registration event send failed: eventId={}, error={}",
                            event.getEventId(), throwable.getMessage(), throwable);
                    return false;
                } else {
                    log.info("User registration event sent successfully: eventId={}, topic={}, partition={}, offset={}",
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return true;
                }
            });
        } catch (Exception e) {
            log.error("Exception sending user registration event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> sendStockEvent(StockControlEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send stock control event: eventId={}", event.getEventId());
            return CompletableFuture.completedFuture(false);
        }

        log.info("Sending stock control event to Kafka: eventId={}, type={}, bookId={}",
                event.getEventId(), event.getEventType(), event.getBookId());

        event.setStatus(StockControlEvent.EventStatus.PENDING);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(stockControlTopic, event.getEventId(), event);

            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("Stock control event send failed: eventId={}, error={}",
                            event.getEventId(), throwable.getMessage(), throwable);
                    return false;
                } else {
                    log.info("Stock control event sent successfully: eventId={}, topic={}, partition={}, offset={}",
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return true;
                }
            });
        } catch (Exception e) {
            log.error("Exception sending stock control event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> sendStockOrderEvent(StockOrderEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send stock order event: eventId={}", event.getEventId());
            return CompletableFuture.completedFuture(false);
        }

        log.info("Sending stock order event to Kafka: eventId={}, type={}",
                event.getEventId(), event.getEventType());

        event.setStatus(StockOrderEvent.EventStatus.PENDING);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(stockOrderTopic, event.getEventId(), event);

            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("Stock order event send failed: eventId={}, error={}",
                            event.getEventId(), throwable.getMessage(), throwable);
                    return false;
                } else {
                    log.info("Stock order event sent successfully: eventId={}, topic={}, partition={}, offset={}",
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return true;
                }
            });
        } catch (Exception e) {
            log.error("Exception sending stock order event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> sendInvoiceEvent(InvoiceEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send invoice event: eventId={}", event.getEventId());
            return CompletableFuture.completedFuture(false);
        }

        log.info("Sending invoice event to Kafka: eventId={}, type={}",
                event.getEventId(), event.getEventType());

        event.setStatus(InvoiceEvent.EventStatus.PENDING);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(invoiceTopic, event.getEventId(), event);

            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("Invoice event send failed: eventId={}, error={}",
                            event.getEventId(), throwable.getMessage(), throwable);
                    return false;
                } else {
                    log.info("Invoice event sent successfully: eventId={}, topic={}, partition={}, offset={}",
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return true;
                }
            });
        } catch (Exception e) {
            log.error("Exception sending invoice event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    // Retry and DLQ methods with null checks
    public void sendRetryEvent(UserRegistrationEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send retry event: eventId={}", event.getEventId());
            return;
        }

        String retryTopic = userRegistrationTopic + ".retry";
        log.info("Sending user registration retry event: eventId={}, retryCount={}",
                event.getEventId(), event.getRetryCount());

        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    public void sendToDLQ(UserRegistrationEvent event, String errorReason) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send to DLQ: eventId={}", event.getEventId());
            return;
        }

        String dlqTopic = userRegistrationTopic + ".dlq";
        event.setMessage("DLQ: " + errorReason);
        event.setStatus(UserRegistrationEvent.EventStatus.FAILED);

        log.error("Sending user registration event to DLQ: eventId={}, reason={}",
                event.getEventId(), errorReason);

        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }

    // Other retry and DLQ methods with similar null checks...
    public void sendStockEventRetry(StockControlEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) return;
        String retryTopic = stockControlTopic + ".retry";
        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    public void sendStockEventToDLQ(StockControlEvent event, String errorReason) {
        if (!kafkaEnabled || kafkaTemplate == null) return;
        String dlqTopic = stockControlTopic + ".dlq";
        event.setMessage("DLQ: " + errorReason);
        event.setStatus(StockControlEvent.EventStatus.FAILED);
        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }

    public void sendStockOrderEventRetry(StockOrderEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) return;
        String retryTopic = stockOrderTopic + ".retry";
        kafkaTemplate.send(retryTopic, event.getEventId(), event);
    }

    public void sendStockOrderEventToDLQ(StockOrderEvent event, String errorReason) {
        if (!kafkaEnabled || kafkaTemplate == null) return;
        String dlqTopic = stockOrderTopic + ".dlq";
        event.setMessage("DLQ: " + errorReason);
        event.setStatus(StockOrderEvent.EventStatus.FAILED);
        kafkaTemplate.send(dlqTopic, event.getEventId(), event);
    }

    public void sendMessage(String topic, String key, Object message) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.warn("Kafka disabled or template null - cannot send message to topic: {}", topic);
            return;
        }

        log.info("Sending message to Kafka: topic={}, key={}", topic, key);

        kafkaTemplate.send(topic, key, message)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Message send failed: topic={}, key={}, error={}",
                                topic, key, throwable.getMessage());
                    } else {
                        log.info("Message sent successfully: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}