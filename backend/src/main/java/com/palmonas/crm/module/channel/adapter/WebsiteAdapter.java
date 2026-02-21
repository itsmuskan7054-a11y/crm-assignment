package com.palmonas.crm.module.channel.adapter;

import com.palmonas.crm.config.AppProperties;
import com.palmonas.crm.module.channel.model.ChannelOrder;
import com.palmonas.crm.module.channel.simulator.ChannelSimulator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class WebsiteAdapter implements ChannelAdapter {

    private final ChannelSimulator simulator;

    public WebsiteAdapter(AppProperties props) {
        this.simulator = new ChannelSimulator(props.getChannel().getSimulator().getFailureRate() * 0.5);
    }

    @Override
    public String getChannelName() {
        return "WEBSITE";
    }

    @Override
    @CircuitBreaker(name = "websiteChannel", fallbackMethod = "fetchOrdersFallback")
    @Retry(name = "websiteChannel")
    @Bulkhead(name = "websiteChannel")
    public List<ChannelOrder> fetchOrders() {
        log.debug("Fetching orders from Website channel");
        simulator.simulateLatency();
        simulator.maybeThrowError("Website");
        return simulator.generateOrders("WEBSITE", "WEB-2026-", 2);
    }

    @Override
    public boolean isAvailable() {
        try {
            simulator.maybeThrowError("Website");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private List<ChannelOrder> fetchOrdersFallback(Exception ex) {
        log.warn("Website channel unavailable, returning empty results: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
