package org.example.notification.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class EventDeduper {

    private static final int MAX_CACHED_IDS = 5_000;

    private final Set<UUID> processed = Collections.synchronizedSet(
            Collections.newSetFromMap(new BoundedLruMap<>()));

    public boolean isProcessed(UUID eventId) {
        return processed.contains(eventId);
    }

    public void markProcessed(UUID eventId) {
        processed.add(eventId);
    }

    private static final class BoundedLruMap<K> extends LinkedHashMap<K, Boolean> {

        private static final long serialVersionUID = 1L;

        BoundedLruMap() {
            super(MAX_CACHED_IDS, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, Boolean> eldest) {
            return size() > MAX_CACHED_IDS;
        }
    }
}
