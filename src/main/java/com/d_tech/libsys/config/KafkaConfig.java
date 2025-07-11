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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka konfigürasyon sınıfı
 * Producer ve Consumer yapılandırmalarını içerir
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.group-id:1}")
    private String groupId;

    /**
     * Kafka Producer Factory Bean
     * Mesaj göndermek için kullanılır
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Producer performans ayarları
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Tüm replikalardan onay bekle
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // 3 kez deneme
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Batch boyutu
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Batch bekleme süresi
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // Buffer memory

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka Template Bean
     * Producer'ı kullanmak için template
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka Consumer Factory Bean
     * Mesaj almak için kullanılır
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Consumer ayarları
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // En baştan oku
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manuel commit
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // Session timeout
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // Heartbeat interval

        // JSON deserializer güvenlik ayarları
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.d_tech.libsys.dto");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.d_tech.libsys.dto.UserRegistrationEvent");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka Listener Container Factory
     * @KafkaListener annotasyonu için gerekli
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Listener ayarları
        factory.setConcurrency(3); // 3 paralel consumer
        factory.getContainerProperties().setPollTimeout(3000); // Poll timeout

        return factory;
    }
}