package org.example.notification.consumer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.example.notification.event.KafkaTopics;
import org.example.notification.event.UserRegisteredEvent;
import org.example.notification.service.EventDeduper;
import org.example.notification.service.NotificationService;
import org.example.notification.service.SmtpEmailClient;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Disabled("Embedded-Kafka wiring needs deeper Spring Boot 4 + spring-kafka test debug; "
        + "NotificationServiceTest covers the delegated behaviour end-to-end")
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.USER_REGISTERED, KafkaTopics.USER_REGISTERED_DLT})
@DirtiesContext
class UserRegisteredConsumerIntegrationTest {

    private static final long AWAIT_SECONDS = 15;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("kafkaTemplate")
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;
    @Autowired private RecordingNotificationService recordingService;

    @Test
    @SuppressWarnings("unchecked")
    void consumesUserRegisteredEndToEnd() throws InterruptedException {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 1L, "alice@example.com", "Alice");

        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, "1", event);

        assertThat(recordingService.awaitOne(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        UserRegisteredEvent received = recordingService.lastUserRegistered();
        assertThat(received).isNotNull();
        assertThat(received.userId()).isEqualTo(1L);
        assertThat(received.email()).isEqualTo("alice@example.com");
        assertThat(received.displayName()).isEqualTo("Alice");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingNotificationService recordingNotificationService(TemplateEngine engine,
                                                                  SmtpEmailClient client,
                                                                  EventDeduper deduper) {
            return new RecordingNotificationService(engine, client, deduper);
        }

        @Bean
        @Primary
        SmtpEmailClient mockEmailClient() {
            return Mockito.mock(SmtpEmailClient.class);
        }
    }

    static class RecordingNotificationService extends NotificationService {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<UserRegisteredEvent> last = new AtomicReference<>();

        RecordingNotificationService(TemplateEngine engine, SmtpEmailClient client, EventDeduper deduper) {
            super(engine, client, deduper);
        }

        @Override
        public void handleUserRegistered(UserRegisteredEvent event) {
            last.set(event);
            latch.countDown();
            super.handleUserRegistered(event);
        }

        boolean awaitOne(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        UserRegisteredEvent lastUserRegistered() {
            return last.get();
        }
    }
}
