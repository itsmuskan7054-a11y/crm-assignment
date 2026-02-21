package com.palmonas.crm.module.order.model;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PROCESSING, CANCELLED),
            PROCESSING, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED, RETURNED),
            DELIVERED, Set.of(RETURNED),
            CANCELLED, Set.of(),
            RETURNED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
