package org.example.notification.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    private static final long BACKOFF_INTERVAL_MS = 1_000L;
    private static final long MAX_RETRIES = 2L;

    @Bean
    @Primary
    public ProducerFactory<String, Object> jsonProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, Object> factory =
                new DefaultKafkaProducerFactory<>(props);
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(javaTimeAwareMapper());
        valueSerializer.setTypeMapper(typeMapper());
        factory.setValueSerializer(valueSerializer);
        return factory;
    }

    private static ObjectMapper javaTimeAwareMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private static org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper typeMapper() {
        org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper mapper =
                new org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper();
        mapper.setIdClassMapping(Map.of(
                "userRegistered", org.example.notification.event.UserRegisteredEvent.class,
                "pantryItemExpiring", org.example.notification.event.PantryItemExpiringEvent.class));
        return mapper;
    }

    @Bean(name = "kafkaTemplate")
    @Primary
    public KafkaTemplate<String, Object> jsonKafkaTemplate(ProducerFactory<String, Object> jsonProducerFactory) {
        return new KafkaTemplate<>(jsonProducerFactory);
    }

    // DLT reuses the JSON template. The source record's key is a String and the value is
    // either a deserialized event or (on ErrorHandlingDeserializer failure) raw bytes;
    // JsonSerializer can serialize either. This is simpler than juggling separate templates
    // per value type and avoids the type-mismatch issues at the recoverer boundary.

    @Bean
    @Primary
    public ConsumerFactory<String, Object> jsonConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "org.example.notification.event");
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "userRegistered:org.example.notification.event.UserRegisteredEvent,"
                        + "pantryItemExpiring:org.example.notification.event.PantryItemExpiringEvent");
        JsonDeserializer<Object> delegate = new JsonDeserializer<>(javaTimeAwareMapper());
        delegate.configure(props, false);
        ErrorHandlingDeserializer<Object> valueDeserializer = new ErrorHandlingDeserializer<>(delegate);
        valueDeserializer.configure(props, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> jsonConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate, KafkaConfig::resolveDltDestination);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(BACKOFF_INTERVAL_MS, MAX_RETRIES));
    }

    static TopicPartition resolveDltDestination(
            org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception ex) {
        log.error("Routing failed record from topic {} partition {} offset {} to DLT: {}",
                record.topic(), record.partition(), record.offset(), ex.getMessage());
        return new TopicPartition(record.topic() + ".dlt", record.partition());
    }
}
