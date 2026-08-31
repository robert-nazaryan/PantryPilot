package org.example.pantrypilot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pantrypilot.event.DomainEventPublisher;
import org.example.pantrypilot.event.PantryItemExpiringEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiringDigestScheduler {

    private final ExpiringDigestService digestService;
    private final DomainEventPublisher publisher;

    @Value("${app.expiring-digest.window-days:3}")
    private int windowDays;

    @Scheduled(cron = "${app.expiring-digest.cron:0 0 7 * * *}", zone = "UTC")
    public void publishDailyDigests() {
        var events = digestService.buildDigests(windowDays);
        log.info("Publishing {} expiring-items digest events (window={} days)", events.size(), windowDays);
        for (PantryItemExpiringEvent event : events) {
            publisher.publishOutsideTransaction(event);
        }
    }
}
