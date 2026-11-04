package com.maksymchernenko.railwayticketsystem.service.admin;

import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.PopularSegmentResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.RouteRevenueResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.SalesTrendResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.SummaryKpiResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.TripOccupancyResponse;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.service.common.AnalyticsMath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final List<Ticket.Status> SOLD_TICKET_STATUSES = List.of(Ticket.Status.ACTIVE, Ticket.Status.FINISHED);

    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;
    private final AnalyticsMath analyticsMath;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics(AdminAnalyticsRequest request) {
        AnalyticsFilter filter = toFilter(request);

        return new AdminAnalyticsResponse(
                getSummary(filter),
                getRevenueByRoute(filter),
                getDemandByRoute(filter),
                getPopularSegments(filter),
                getHighestOccupancyTrips(filter),
                getLowestOccupancyTrips(filter),
                getSalesTrend(filter)
        );
    }

    @Transactional(readOnly = true)
    public List<SummaryKpiResponse> getSummary(Instant dateFrom, Instant dateTo, Long routeId) {
        return getSummary(toFilter(dateFrom, dateTo, routeId, DEFAULT_LIMIT));
    }

    private List<SummaryKpiResponse> getSummary(AnalyticsFilter filter) {
        return ticketRepository.findSummary(filter.dateFrom(), filter.dateTo(), filter.routeId(), Order.Status.PAID, SOLD_TICKET_STATUSES)
                .stream()
                .map(row -> new SummaryKpiResponse(
                        row.getCurrencyCode(),
                        analyticsMath.zeroIfNull(row.getSoldTicketCount()),
                        analyticsMath.zeroIfNull(row.getTotalRevenue()),
                        analyticsMath.average(row.getTotalRevenue(), row.getSoldTicketCount()),
                        analyticsMath.zeroIfNull(row.getPaidOrderCount()),
                        analyticsMath.zeroIfNull(row.getCustomerCount())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteRevenueResponse> getRevenueByRoute(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        return getRevenueByRoute(toFilter(dateFrom, dateTo, routeId, limit));
    }

    private List<RouteRevenueResponse> getRevenueByRoute(AnalyticsFilter filter) {
        return ticketRepository.findRevenueByRoute(
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.routeId(),
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        filter.pageRequest()
                )
                .stream()
                .map(this::toRouteRevenueResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteRevenueResponse> getDemandByRoute(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        return getDemandByRoute(toFilter(dateFrom, dateTo, routeId, limit));
    }

    private List<RouteRevenueResponse> getDemandByRoute(AnalyticsFilter filter) {
        return ticketRepository.findDemandByRoute(
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.routeId(),
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        filter.pageRequest()
                )
                .stream()
                .map(this::toRouteRevenueResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopularSegmentResponse> getPopularSegments(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        return getPopularSegments(toFilter(dateFrom, dateTo, routeId, limit));
    }

    private List<PopularSegmentResponse> getPopularSegments(AnalyticsFilter filter) {
        return ticketRepository.findPopularSegments(
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.routeId(),
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        filter.pageRequest()
                )
                .stream()
                .map(row -> new PopularSegmentResponse(
                        row.getStartStationName(),
                        row.getEndStationName(),
                        row.getCurrencyCode(),
                        analyticsMath.zeroIfNull(row.getTicketCount()),
                        analyticsMath.zeroIfNull(row.getTotalRevenue())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripOccupancyResponse> getHighestOccupancyTrips(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        return getHighestOccupancyTrips(toFilter(dateFrom, dateTo, routeId, limit));
    }

    private List<TripOccupancyResponse> getHighestOccupancyTrips(AnalyticsFilter filter) {
        return tripRepository.findHighestOccupancyTrips(
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.routeId(),
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        filter.pageRequest()
                )
                .stream()
                .map(this::toTripOccupancyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripOccupancyResponse> getLowestOccupancyTrips(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        return getLowestOccupancyTrips(toFilter(dateFrom, dateTo, routeId, limit));
    }

    private List<TripOccupancyResponse> getLowestOccupancyTrips(AnalyticsFilter filter) {
        return tripRepository.findLowestOccupancyTrips(
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.routeId(),
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        filter.pageRequest()
                )
                .stream()
                .map(this::toTripOccupancyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesTrendResponse> getSalesTrend(Instant dateFrom, Instant dateTo, Long routeId) {
        return getSalesTrend(toFilter(dateFrom, dateTo, routeId, DEFAULT_LIMIT));
    }

    private List<SalesTrendResponse> getSalesTrend(AnalyticsFilter filter) {
        return ticketRepository.findSalesTrend(filter.dateFrom(), filter.dateTo(), filter.routeId(), Order.Status.PAID, SOLD_TICKET_STATUSES)
                .stream()
                .map(row -> new SalesTrendResponse(
                        row.getSaleDate(),
                        row.getCurrencyCode(),
                        analyticsMath.zeroIfNull(row.getSoldTicketCount()),
                        analyticsMath.zeroIfNull(row.getTotalRevenue()),
                        analyticsMath.zeroIfNull(row.getPaidOrderCount())
                ))
                .toList();
    }

    public int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.clamp(limit, 1, MAX_LIMIT);
    }

    private RouteRevenueResponse toRouteRevenueResponse(TicketRepository.RouteRevenueRow row) {
        return new RouteRevenueResponse(
                row.getRouteId(),
                row.getRouteNumber(),
                row.getRouteName(),
                row.getCurrencyCode(),
                analyticsMath.zeroIfNull(row.getSoldTicketCount()),
                analyticsMath.zeroIfNull(row.getTotalRevenue()),
                analyticsMath.average(row.getTotalRevenue(), row.getSoldTicketCount()),
                analyticsMath.zeroIfNull(row.getPaidOrderCount()),
                analyticsMath.zeroIfNull(row.getCustomerCount())
        );
    }

    private TripOccupancyResponse toTripOccupancyResponse(TripRepository.TripOccupancyRow row) {
        long totalSeats = analyticsMath.zeroIfNull(row.getTotalSeatCount());
        long soldSeats = analyticsMath.zeroIfNull(row.getSoldSeatCount());

        return new TripOccupancyResponse(
                row.getTripId(),
                row.getRouteId(),
                row.getRouteNumber(),
                row.getRouteName(),
                row.getDepartureAtUtc(),
                analyticsMath.zeroIfNull(row.getCarCount()),
                totalSeats,
                soldSeats,
                analyticsMath.percent(soldSeats, totalSeats)
        );
    }

    private AnalyticsFilter toFilter(AdminAnalyticsRequest request) {
        return toFilter(request.dateFrom(), request.dateTo(), request.routeId(), request.limit());
    }

    private AnalyticsFilter toFilter(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        if (dateFrom == null || dateTo == null || !dateFrom.isBefore(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be earlier than dateTo");
        }
        return new AnalyticsFilter(dateFrom, dateTo, routeId, normalizeLimit(limit));
    }

    private record AnalyticsFilter(
            Instant dateFrom,
            Instant dateTo,
            Long routeId,
            int limit
    ) {

        private PageRequest pageRequest() {
            return PageRequest.of(0, limit);
        }
    }
}
