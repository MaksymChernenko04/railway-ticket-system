package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesTrendResponse(
        LocalDate saleDate,
        String currencyCode,
        long soldTicketCount,
        BigDecimal totalRevenue,
        long paidOrderCount
) {
}
