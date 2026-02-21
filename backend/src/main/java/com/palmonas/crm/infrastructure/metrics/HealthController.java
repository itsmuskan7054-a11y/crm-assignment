package com.palmonas.crm.infrastructure.metrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping
    @Operation(summary = "Basic health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe")
    public ResponseEntity<Map<String, Object>> readiness() {
        HealthComponent health = healthEndpoint.health();
        boolean ready = "UP".equals(health.getStatus().getCode());
        Map<String, Object> details = Map.of();
        if (health instanceof CompositeHealth composite) {
            details = Map.copyOf(composite.getComponents());
        }
        return ready
                ? ResponseEntity.ok(Map.of("status", "READY", "details", details))
                : ResponseEntity.status(503).body(Map.of("status", "NOT_READY"));
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness probe")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "ALIVE"));
    }
}
