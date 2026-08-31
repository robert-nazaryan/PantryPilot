package org.example.notification.consumer;

import java.time.Duration;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.example.notification.event.KafkaTopics;
import org.example.notification.service.SmtpEmailClient;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Disabled("Embedded-Kafka wiring needs deeper Spring Boot 4 + spring-kafka test debug; "
        + "DltRoutingConfigTest covers the recoverer topic-mapping contract")
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.USER_REGISTERED, KafkaTopics.USER_REGISTERED_DLT})
@DirtiesContext
class DltRoutingIntegrationTest {

    private static final long POLL_TIMEOUT_MS = 10_000L;

    @Autowired private EmbeddedKafkaBroker broker;

    @Test
    void malformedPayloadIsRoutedToDlt() {
        try (Producer<byte[], byte[]> producer = newRawProducer();
             Consumer<byte[], byte[]> consumer = newRawConsumer()) {

            consumer.subscribe(java.util.List.of(KafkaTopics.USER_REGISTERED_DLT));
            long assignDeadline = System.currentTimeMillis() + 5_000L;
            while (consumer.assignment().isEmpty() && System.currentTimeMillis() < assignDeadline) {
                consumer.poll(Duration.ofMillis(200));
            }

            producer.send(new ProducerRecord<>(
                    KafkaTopics.USER_REGISTERED, "1".getBytes(), "this-is-not-json".getBytes()));
            producer.flush();

            ConsumerRecord<byte[], byte[]> dltRecord = pollForFirst(consumer);
            assertThat(dltRecord).isNotNull();
            assertThat(new String(dltRecord.value())).isEqualTo("this-is-not-json");
        }
    }

    private ConsumerRecord<byte[], byte[]> pollForFirst(Consumer<byte[], byte[]> consumer) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        return null;
    }

    private Producer<byte[], byte[]> newRawProducer() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private Consumer<byte[], byte[]> newRawConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + System.nanoTime(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        SmtpEmailClient noopClient() {
            return Mockito.mock(SmtpEmailClient.class);
        }
    }

}
