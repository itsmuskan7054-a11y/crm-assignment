package com.palmonas.crm.module.channel;

import com.palmonas.crm.module.channel.adapter.ChannelAdapter;
import com.palmonas.crm.module.channel.model.ChannelOrder;
import com.palmonas.crm.module.notification.service.DeadLetterService;
import com.palmonas.crm.module.order.model.*;
import com.palmonas.crm.module.order.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChannelSyncService {

    private final List<ChannelAdapter> adapters;
    private final OrderRepository orderRepository;
    private final DeadLetterService deadLetterService;
    private final Counter syncSuccessCounter;
    private final Counter syncFailureCounter;

    public ChannelSyncService(List<ChannelAdapter> adapters,
                              OrderRepository orderRepository,
                              DeadLetterService deadLetterService,
                              MeterRegistry meterRegistry) {
        this.adapters = adapters;
        this.orderRepository = orderRepository;
        this.deadLetterService = deadLetterService;
        this.syncSuccessCounter = Counter.builder("channel.sync.success")
                .description("Successful channel syncs")
                .register(meterRegistry);
        this.syncFailureCounter = Counter.builder("channel.sync.failure")
                .description("Failed channel syncs")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${app.channel.sync-cron}")
    public void syncAllChannels() {
        log.info("Starting scheduled channel sync for {} adapters", adapters.size());
        for (ChannelAdapter adapter : adapters) {
            syncChannel(adapter);
        }
        log.info("Channel sync completed");
    }

    @Transactional
    public int syncChannel(ChannelAdapter adapter) {
        int imported = 0;
        try {
            List<ChannelOrder> orders = adapter.fetchOrders();
            for (ChannelOrder co : orders) {
                if (!orderRepository.existsByExternalOrderId(co.getExternalOrderId())) {
                    Order order = mapToOrder(co);
                    orderRepository.save(order);
                    imported++;
                }
            }
            syncSuccessCounter.increment();
            log.info("Synced {} new orders from {}", imported, adapter.getChannelName());
        } catch (Exception ex) {
            syncFailureCounter.increment();
            log.error("Failed to sync channel {}: {}", adapter.getChannelName(), ex.getMessage());
            deadLetterService.record(
                    "CHANNEL_SYNC_" + adapter.getChannelName(),
                    Map.of("channel", adapter.getChannelName(), "error", ex.getMessage()),
                    ex
            );
        }
        return imported;
    }

    private Order mapToOrder(ChannelOrder co) {
        Order order = Order.builder()
                .externalOrderId(co.getExternalOrderId())
                .channel(Channel.valueOf(co.getChannel()))
                .status(OrderStatus.PENDING)
                .customerName(co.getCustomerName())
                .customerEmail(co.getCustomerEmail())
                .customerPhone(co.getCustomerPhone())
                .shippingAddress(co.getShippingAddress())
                .totalAmount(co.getTotalAmount())
                .currency(co.getCurrency())
                .metadata(co.getMetadata())
                .orderedAt(co.getOrderedAt())
                .build();

        if (co.getItems() != null) {
            for (ChannelOrder.ChannelOrderItem ci : co.getItems()) {
                OrderItem item = OrderItem.builder()
                        .productName(ci.getProductName())
                        .sku(ci.getSku())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .totalPrice(ci.getTotalPrice())
                        .build();
                order.addItem(item);
            }
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .newStatus(OrderStatus.PENDING)
                .notes("Imported from " + co.getChannel())
                .build();
        order.getStatusHistory().add(history);

        return order;
    }
}
