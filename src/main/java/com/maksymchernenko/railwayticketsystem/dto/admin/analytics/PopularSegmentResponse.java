package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import java.math.BigDecimal;

public record PopularSegmentResponse(
        String startStationName,
        String endStationName,
        String currencyCode,
        long ticketCount,
        BigDecimal totalRevenue
) {
}
