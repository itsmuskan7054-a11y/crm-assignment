package com.palmonas.crm.module.order.repository;

import com.palmonas.crm.module.order.model.Channel;
import com.palmonas.crm.module.order.model.Order;
import com.palmonas.crm.module.order.model.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> hasChannel(Channel channel) {
        return (root, query, cb) -> channel == null ? null : cb.equal(root.get("channel"), channel);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> orderedAfter(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("orderedAt"), from);
    }

    public static Specification<Order> orderedBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("orderedAt"), to);
    }

    public static Specification<Order> searchTerm(String search) {
        if (search == null || search.isBlank()) return null;
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("customerName")), pattern),
                cb.like(cb.lower(root.get("customerEmail")), pattern),
                cb.like(cb.lower(root.get("externalOrderId")), pattern),
                cb.like(cb.lower(root.get("customerPhone")), pattern)
        );
    }
}
