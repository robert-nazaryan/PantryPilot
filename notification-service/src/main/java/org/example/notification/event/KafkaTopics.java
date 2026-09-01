package org.example.notification.event;

public final class KafkaTopics {

    public static final String USER_REGISTERED = "user.registered";
    public static final String PANTRY_ITEM_EXPIRING = "pantry.item.expiring";
    public static final String USER_REGISTERED_DLT = "user.registered.dlt";
    public static final String PANTRY_ITEM_EXPIRING_DLT = "pantry.item.expiring.dlt";

    private KafkaTopics() {
    }
}
