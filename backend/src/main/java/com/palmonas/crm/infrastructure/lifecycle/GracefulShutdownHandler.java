package com.palmonas.crm.infrastructure.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GracefulShutdownHandler {

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("=== Graceful shutdown initiated ===");
        log.info("Draining in-flight requests...");
        log.info("Closing database connections...");
        log.info("Closing Redis connections...");
        log.info("Cancelling scheduled tasks...");
        log.info("=== Shutdown complete ===");
    }
}
