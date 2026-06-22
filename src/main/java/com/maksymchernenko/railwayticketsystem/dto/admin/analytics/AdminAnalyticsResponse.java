package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import java.util.List;

public record AdminAnalyticsResponse(
        List<SummaryKpiResponse> summary,
        List<RouteRevenueResponse> revenueByRoute,
        List<RouteRevenueResponse> demandByRoute,
        List<PopularSegmentResponse> popularSegments,
        List<TripOccupancyResponse> highSeatOccupancy,
        List<TripOccupancyResponse> lowSeatOccupancy,
        List<SalesTrendResponse> salesTrend
) {
}
