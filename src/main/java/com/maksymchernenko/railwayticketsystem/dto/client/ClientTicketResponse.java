package com.maksymchernenko.railwayticketsystem.dto.client;

import java.math.BigDecimal;
import java.time.Instant;

public record ClientTicketResponse(
        Long id,
        Long orderId,
        Long tripId,
        String status,
        String passengerFirstName,
        String passengerLastName,
        String routeNumber,
        String routeName,
        String trainNumber,
        String trainName,
        String startStationName,
        String endStationName,
        Instant departureAtUtc,
        Instant arrivalAtUtc,
        String carNumber,
        Integer carOrdinal,
        String seatNumber,
        String seatTypeCode,
        BigDecimal price,
        String currencyCode,
        String appliedBenefitCode,
        BigDecimal appliedBenefitPercent,
        Instant orderCreatedAtUtc
) {
}
