package com.palmonas.crm.module.order.service;

import com.palmonas.crm.common.dto.PagedResponse;
import com.palmonas.crm.common.exception.BadRequestException;
import com.palmonas.crm.common.exception.ResourceNotFoundException;
import com.palmonas.crm.module.order.dto.*;
import com.palmonas.crm.module.order.model.*;
import com.palmonas.crm.module.order.repository.OrderRepository;
import com.palmonas.crm.module.order.repository.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrders(OrderFilterRequest filter) {
        int pageSize = Math.min(Math.max(filter.getSize(), 1), 100);
        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                mapSortField(filter.getSortBy())
        );
        PageRequest pageRequest = PageRequest.of(filter.getPage(), pageSize, sort);

        Specification<Order> spec = Specification.where(null);

        if (filter.getChannel() != null && !filter.getChannel().isBlank()) {
            try {
                spec = spec.and(OrderSpecifications.hasChannel(Channel.valueOf(filter.getChannel().toUpperCase())));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid channel: " + filter.getChannel());
            }
        }

        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            try {
                spec = spec.and(OrderSpecifications.hasStatus(OrderStatus.valueOf(filter.getStatus().toUpperCase())));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + filter.getStatus());
            }
        }

        if (filter.getFrom() != null && !filter.getFrom().isBlank()) {
            spec = spec.and(OrderSpecifications.orderedAfter(Instant.parse(filter.getFrom())));
        }
        if (filter.getTo() != null && !filter.getTo().isBlank()) {
            spec = spec.and(OrderSpecifications.orderedBefore(Instant.parse(filter.getTo())));
        }

        spec = spec.and(OrderSpecifications.searchTerm(filter.getSearch()));

        Page<Order> page = orderRepository.findAll(spec, pageRequest);

        List<OrderResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return toDetailResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, UpdateStatusRequest request, UUID changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException(
                    String.format("Cannot transition from %s to %s", order.getStatus(), newStatus));
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(order.getStatus())
                .newStatus(newStatus)
                .changedBy(changedBy)
                .notes(request.getNotes())
                .build();

        order.getStatusHistory().add(history);
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        log.info("Order {} status changed: {} -> {} by user {}",
                order.getExternalOrderId(), history.getOldStatus(), newStatus, changedBy);

        return toDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        long todayOrders = orderRepository.countTodayOrders();

        List<DashboardStats.ChannelStat> channelStats = orderRepository.getStatsByChannel().stream()
                .map(row -> DashboardStats.ChannelStat.builder()
                        .channel(((Channel) row[0]).name())
                        .orderCount((Long) row[1])
                        .revenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusBreakdown.put(s.name(), 0L);
        }
        orderRepository.getStatsByStatus().forEach(row ->
                statusBreakdown.put(((OrderStatus) row[0]).name(), (Long) row[1])
        );

        return DashboardStats.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .todayOrders(todayOrders)
                .channelStats(channelStats)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    private OrderResponse toSummaryResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId().toString())
                .externalOrderId(order.getExternalOrderId())
                .channel(order.getChannel().name())
                .status(order.getStatus().name())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .orderedAt(order.getOrderedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderResponse toDetailResponse(Order order) {
        OrderResponse response = toSummaryResponse(order);
        response.setShippingAddress(order.getShippingAddress());
        response.setMetadata(order.getMetadata());

        response.setItems(order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId().toString())
                        .productName(item.getProductName())
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList()));

        response.setStatusHistory(order.getStatusHistory().stream()
                .map(h -> OrderResponse.StatusHistoryResponse.builder()
                        .id(h.getId().toString())
                        .oldStatus(h.getOldStatus() != null ? h.getOldStatus().name() : null)
                        .newStatus(h.getNewStatus().name())
                        .changedBy(h.getChangedBy() != null ? h.getChangedBy().toString() : null)
                        .notes(h.getNotes())
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList()));

        return response;
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "date", "orderedAt" -> "orderedAt";
            case "amount", "totalAmount" -> "totalAmount";
            case "customer", "customerName" -> "customerName";
            case "status" -> "status";
            case "channel" -> "channel";
            default -> "orderedAt";
        };
    }
}
