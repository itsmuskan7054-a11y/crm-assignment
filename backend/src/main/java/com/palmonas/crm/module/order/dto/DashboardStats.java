package com.palmonas.crm.module.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private long totalOrders;
    private BigDecimal totalRevenue;
    private long todayOrders;
    private List<ChannelStat> channelStats;
    private Map<String, Long> statusBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelStat {
        private String channel;
        private long orderCount;
        private BigDecimal revenue;
    }
}
