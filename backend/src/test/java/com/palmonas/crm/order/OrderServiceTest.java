package com.palmonas.crm.order;

import com.palmonas.crm.common.exception.BadRequestException;
import com.palmonas.crm.common.exception.ResourceNotFoundException;
import com.palmonas.crm.module.order.dto.DashboardStats;
import com.palmonas.crm.module.order.dto.OrderFilterRequest;
import com.palmonas.crm.module.order.dto.UpdateStatusRequest;
import com.palmonas.crm.module.order.model.*;
import com.palmonas.crm.module.order.repository.OrderRepository;
import com.palmonas.crm.module.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderService orderService;

    private Order buildTestOrder(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .externalOrderId("TEST-001")
                .channel(Channel.AMAZON)
                .status(status)
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .totalAmount(new BigDecimal("1000.00"))
                .currency("INR")
                .orderedAt(Instant.now())
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrdersShouldReturnPagedResults() {
        OrderFilterRequest filter = new OrderFilterRequest();
        Order order = buildTestOrder(OrderStatus.PENDING);
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var result = orderService.getOrders(filter);

        assertEquals(1, result.getContent().size());
        assertEquals("TEST-001", result.getContent().get(0).getExternalOrderId());
    }

    @Test
    void getOrderByIdShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(id));
    }

    @Test
    void updateStatusShouldWorkForValidTransition() {
        Order order = buildTestOrder(OrderStatus.PENDING);
        UUID userId = UUID.randomUUID();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("CONFIRMED");
        request.setNotes("Test note");

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var result = orderService.updateStatus(order.getId(), request, userId);

        assertNotNull(result);
        verify(orderRepository).save(any());
    }

    @Test
    void updateStatusShouldThrowForInvalidTransition() {
        Order order = buildTestOrder(OrderStatus.DELIVERED);
        UUID userId = UUID.randomUUID();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("PENDING");

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.updateStatus(order.getId(), request, userId));
    }

    @Test
    void getDashboardStatsShouldReturnAggregates() {
        when(orderRepository.count()).thenReturn(60L);
        when(orderRepository.getTotalRevenue()).thenReturn(new BigDecimal("300000.00"));
        when(orderRepository.countTodayOrders()).thenReturn(5L);
        when(orderRepository.getStatsByChannel()).thenReturn(List.of(
                new Object[]{Channel.AMAZON, 20L, new BigDecimal("100000")},
                new Object[]{Channel.FLIPKART, 20L, new BigDecimal("120000")},
                new Object[]{Channel.WEBSITE, 20L, new BigDecimal("80000")}
        ));
        when(orderRepository.getStatsByStatus()).thenReturn(List.of(
                new Object[]{OrderStatus.PENDING, 10L},
                new Object[]{OrderStatus.DELIVERED, 30L}
        ));

        DashboardStats stats = orderService.getDashboardStats();

        assertEquals(60, stats.getTotalOrders());
        assertEquals(new BigDecimal("300000.00"), stats.getTotalRevenue());
        assertEquals(3, stats.getChannelStats().size());
    }
}
