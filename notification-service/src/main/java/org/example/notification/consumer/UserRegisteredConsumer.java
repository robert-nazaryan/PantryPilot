package org.example.notification.consumer;

import lombok.RequiredArgsConstructor;
import org.example.notification.event.KafkaTopics;
import org.example.notification.event.UserRegisteredEvent;
import org.example.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "notification-service")
    public void onEvent(UserRegisteredEvent event) {
        notificationService.handleUserRegistered(event);
    }
}
