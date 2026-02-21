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
public class FlipkartAdapter implements ChannelAdapter {

    private final ChannelSimulator simulator;

    public FlipkartAdapter(AppProperties props) {
        this.simulator = new ChannelSimulator(props.getChannel().getSimulator().getFailureRate());
    }

    @Override
    public String getChannelName() {
        return "FLIPKART";
    }

    @Override
    @CircuitBreaker(name = "flipkartChannel", fallbackMethod = "fetchOrdersFallback")
    @Retry(name = "flipkartChannel")
    @Bulkhead(name = "flipkartChannel")
    public List<ChannelOrder> fetchOrders() {
        log.debug("Fetching orders from Flipkart channel");
        simulator.simulateLatency();
        simulator.maybeThrowError("Flipkart");
        return simulator.generateOrders("FLIPKART", "OD", 3);
    }

    @Override
    public boolean isAvailable() {
        try {
            simulator.maybeThrowError("Flipkart");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private List<ChannelOrder> fetchOrdersFallback(Exception ex) {
        log.warn("Flipkart channel unavailable, returning empty results: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
