package com.palmonas.crm.module.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String id;
    private String externalOrderId;
    private String channel;
    private String status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String currency;
    private Map<String, Object> metadata;
    private Instant orderedAt;
    private Instant updatedAt;
    private List<OrderItemResponse> items;
    private List<StatusHistoryResponse> statusHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private String id;
        private String productName;
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryResponse {
        private String id;
        private String oldStatus;
        private String newStatus;
        private String changedBy;
        private String notes;
        private Instant changedAt;
    }
}
