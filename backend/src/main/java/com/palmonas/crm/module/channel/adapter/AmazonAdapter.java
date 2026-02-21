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
public class AmazonAdapter implements ChannelAdapter {

    private final ChannelSimulator simulator;

    public AmazonAdapter(AppProperties props) {
        this.simulator = new ChannelSimulator(props.getChannel().getSimulator().getFailureRate());
    }

    @Override
    public String getChannelName() {
        return "AMAZON";
    }

    @Override
    @CircuitBreaker(name = "amazonChannel", fallbackMethod = "fetchOrdersFallback")
    @Retry(name = "amazonChannel")
    @Bulkhead(name = "amazonChannel")
    public List<ChannelOrder> fetchOrders() {
        log.debug("Fetching orders from Amazon channel");
        simulator.simulateLatency();
        simulator.maybeThrowError("Amazon");
        return simulator.generateOrders("AMAZON", "114-", 3);
    }

    @Override
    public boolean isAvailable() {
        try {
            simulator.maybeThrowError("Amazon");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private List<ChannelOrder> fetchOrdersFallback(Exception ex) {
        log.warn("Amazon channel unavailable, returning empty results: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
