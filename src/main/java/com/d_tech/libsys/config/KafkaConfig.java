package com.d_tech.libsys.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * 🚀 FIXED: Kafka konfigürasyon sınıfı
 * Acknowledgment problemleri çözüldü - AUTO_COMMIT kullanılıyor
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:libsys-group}")
    private String groupId;

    /**
     * ✅ Kafka Producer Factory Bean
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Producer performans ayarları
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * ✅ Kafka Template Bean
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * 🚀 FIXED: Consumer Factory - AUTO_COMMIT ile
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 🚀 CRITICAL FIX: AUTO_COMMIT aktif edildi
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);  // ✅ TRUE yapıldı
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);  // ✅ Eklendi
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // JSON deserializer güvenlik ayarları
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.d_tech.libsys.dto");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.d_tech.libsys.dto.StockOrderEvent");


        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * 🚀 FIXED: Kafka Listener Container Factory - AUTO acknowledge mode
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // 🚀 CRITICAL FIX: Acknowledge mode AUTO yapıldı
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);  // ✅ AUTO yerine RECORD
        factory.getContainerProperties().setPollTimeout(3000);
        factory.setConcurrency(1);

        // Error handling
        factory.setCommonErrorHandler(null);  // Default error handler kullan

        return factory;
    }

    /**
     * ✅ USER REGISTRATION için özel Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, Object> userRegistrationConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);

        // User registration event için özel settings
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.d_tech.libsys.dto");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.d_tech.libsys.dto.UserRegistrationEvent");


        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * ✅ USER REGISTRATION için özel Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> userRegistrationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userRegistrationConsumerFactory());

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.setConcurrency(1);

        return factory;
    }

    /**
     * ✅ STOCK CONTROL için özel Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, Object> stockControlConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);

        // Stock control event için özel settings
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.d_tech.libsys.dto");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.d_tech.libsys.dto.StockControlEvent");


        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * ✅ STOCK CONTROL için özel Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> stockControlKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stockControlConsumerFactory());

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.setConcurrency(1);

        return factory;
    }
}