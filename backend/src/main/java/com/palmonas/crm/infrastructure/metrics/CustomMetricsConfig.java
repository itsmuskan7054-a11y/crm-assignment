package com.palmonas.crm.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricsConfig {

    @Bean
    public Timer orderApiTimer(MeterRegistry registry) {
        return Timer.builder("api.orders.request")
                .description("Order API request duration")
                .register(registry);
    }
}
