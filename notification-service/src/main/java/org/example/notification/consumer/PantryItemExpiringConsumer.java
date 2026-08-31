package org.example.notification.consumer;

import lombok.RequiredArgsConstructor;
import org.example.notification.event.KafkaTopics;
import org.example.notification.event.PantryItemExpiringEvent;
import org.example.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PantryItemExpiringConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.PANTRY_ITEM_EXPIRING, groupId = "notification-service")
    public void onEvent(PantryItemExpiringEvent event) {
        notificationService.handlePantryItemExpiring(event);
    }
}
