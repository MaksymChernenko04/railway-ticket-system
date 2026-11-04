package com.maksymchernenko.railwayticketsystem.dto.ai.context;

import java.math.BigDecimal;
import java.util.List;

public record RouteContext(
        Long routeId,
        String routeNumber,
        String routeName,
        BigDecimal basePrice,
        String currencyCode,
        List<RouteStopContext> stops
) {
}
