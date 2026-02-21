package com.palmonas.crm.order;

import com.palmonas.crm.module.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void pendingCanTransitionToConfirmedOrCancelled() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void confirmedCanTransitionToProcessingOrCancelled() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    void shippedCanTransitionToDeliveredOrReturned() {
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.RETURNED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void cancelledIsTerminalState() {
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    void returnedIsTerminalState() {
        assertFalse(OrderStatus.RETURNED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.RETURNED.canTransitionTo(OrderStatus.DELIVERED));
    }
}
