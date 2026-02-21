package com.palmonas.crm.infrastructure.metrics;

import com.palmonas.crm.module.channel.adapter.ChannelAdapter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("channelHealth")
public class ChannelHealthIndicator implements HealthIndicator {

    private final List<ChannelAdapter> adapters;

    public ChannelHealthIndicator(List<ChannelAdapter> adapters) {
        this.adapters = adapters;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean anyDown = false;

        for (ChannelAdapter adapter : adapters) {
            boolean available = adapter.isAvailable();
            builder.withDetail(adapter.getChannelName(), available ? "UP" : "DEGRADED");
            if (!available) {
                anyDown = true;
            }
        }

        if (anyDown) {
            builder.status("DEGRADED");
        }

        return builder.build();
    }
}
