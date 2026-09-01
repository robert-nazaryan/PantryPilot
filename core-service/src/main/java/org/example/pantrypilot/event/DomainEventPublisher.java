package org.example.pantrypilot.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onUserRegistered(UserRegisteredEvent event) {
        publish(KafkaTopics.USER_REGISTERED, String.valueOf(event.userId()), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onPantryItemExpiring(PantryItemExpiringEvent event) {
        publish(KafkaTopics.PANTRY_ITEM_EXPIRING, String.valueOf(event.userId()), event);
    }

    public void publishOutsideTransaction(PantryItemExpiringEvent event) {
        publish(KafkaTopics.PANTRY_ITEM_EXPIRING, String.valueOf(event.userId()), event);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish event to topic {} for key {}: {}",
                    topic, key, ex.getMessage(), ex);
        }
    }
}
