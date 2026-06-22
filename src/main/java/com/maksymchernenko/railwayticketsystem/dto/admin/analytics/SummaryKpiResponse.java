package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import java.math.BigDecimal;

public record SummaryKpiResponse(
        String currencyCode,
        long soldTicketCount,
        BigDecimal totalRevenue,
        BigDecimal averageTicketPrice,
        long paidOrderCount,
        long customerCount
) {
}
