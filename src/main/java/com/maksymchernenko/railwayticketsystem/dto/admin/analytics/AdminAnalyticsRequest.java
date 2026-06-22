package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AdminAnalyticsRequest(
        @NotNull Instant dateFrom,
        @NotNull Instant dateTo,
        Long routeId,
        @Min(1) @Max(100) Integer limit
) {
}
