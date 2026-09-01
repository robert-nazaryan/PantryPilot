package org.example.pantrypilot.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.pantrypilot.event.KafkaTopics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(javaTimeAwareMapper());
        valueSerializer.setTypeMapper(typeMapper());
        factory.setValueSerializer(valueSerializer);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(KafkaTopics.USER_REGISTERED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic pantryItemExpiringTopic() {
        return TopicBuilder.name(KafkaTopics.PANTRY_ITEM_EXPIRING).partitions(1).replicas(1).build();
    }

    private static ObjectMapper javaTimeAwareMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private static org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper typeMapper() {
        org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper mapper =
                new org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper();
        mapper.setIdClassMapping(Map.of(
                "userRegistered", org.example.pantrypilot.event.UserRegisteredEvent.class,
                "pantryItemExpiring", org.example.pantrypilot.event.PantryItemExpiringEvent.class));
        return mapper;
    }
}
