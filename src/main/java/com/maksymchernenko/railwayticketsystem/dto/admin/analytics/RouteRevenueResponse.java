package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import java.math.BigDecimal;

public record RouteRevenueResponse(
        Long routeId,
        String routeNumber,
        String routeName,
        String currencyCode,
        long soldTicketCount,
        BigDecimal totalRevenue,
        BigDecimal averageTicketPrice,
        long paidOrderCount,
        long customerCount
) {
}
