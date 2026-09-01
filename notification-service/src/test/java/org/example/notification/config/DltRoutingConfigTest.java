package org.example.notification.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.example.notification.event.KafkaTopics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DltRoutingConfigTest {

    @Test
    void userRegisteredFailuresRouteToUserRegisteredDlt() {
        TopicPartition dest = KafkaConfig.resolveDltDestination(
                new ConsumerRecord<>(KafkaTopics.USER_REGISTERED, 0, 0L, null, null),
                new RuntimeException("boom"));

        assertThat(dest.topic()).isEqualTo("user.registered.dlt");
        assertThat(dest.partition()).isZero();
    }

    @Test
    void pantryItemExpiringFailuresRouteToItsDlt() {
        TopicPartition dest = KafkaConfig.resolveDltDestination(
                new ConsumerRecord<>(KafkaTopics.PANTRY_ITEM_EXPIRING, 2, 0L, null, null),
                new IllegalArgumentException("bad payload"));

        assertThat(dest.topic()).isEqualTo("pantry.item.expiring.dlt");
        assertThat(dest.partition()).isEqualTo(2);
    }

    @Test
    void arbitraryTopicFailuresAppendDltSuffix() {
        TopicPartition dest = KafkaConfig.resolveDltDestination(
                new ConsumerRecord<>("some.other.topic", 1, 0L, null, null),
                new RuntimeException("boom"));

        assertThat(dest.topic()).isEqualTo("some.other.topic.dlt");
        assertThat(dest.partition()).isEqualTo(1);
    }
}
