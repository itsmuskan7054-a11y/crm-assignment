package com.palmonas.crm.module.order.repository;

import com.palmonas.crm.module.order.model.Channel;
import com.palmonas.crm.module.order.model.Order;
import com.palmonas.crm.module.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByExternalOrderId(String externalOrderId);

    boolean existsByExternalOrderId(String externalOrderId);

    long countByChannel(Channel channel);

    long countByStatus(OrderStatus status);

    @Query("SELECT o.channel, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o GROUP BY o.channel")
    List<Object[]> getStatsByChannel();

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getStatsByStatus();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderedAt >= CURRENT_DATE")
    long countTodayOrders();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    java.math.BigDecimal getTotalRevenue();
}
