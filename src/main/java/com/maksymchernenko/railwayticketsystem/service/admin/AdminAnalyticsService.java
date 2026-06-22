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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics(AdminAnalyticsRequest request) {
        validatePeriod(request.dateFrom(), request.dateTo());
        int limit = normalizeLimit(request.limit());

        return new AdminAnalyticsResponse(
                getSummary(request.dateFrom(), request.dateTo(), request.routeId()),
                getRevenueByRoute(request.dateFrom(), request.dateTo(), request.routeId(), limit),
                getDemandByRoute(request.dateFrom(), request.dateTo(), request.routeId(), limit),
                getPopularSegments(request.dateFrom(), request.dateTo(), request.routeId(), limit),
                getHighestOccupancyTrips(request.dateFrom(), request.dateTo(), request.routeId(), limit),
                getLowestOccupancyTrips(request.dateFrom(), request.dateTo(), request.routeId(), limit),
                getSalesTrend(request.dateFrom(), request.dateTo(), request.routeId())
        );
    }

    @Transactional(readOnly = true)
    public List<SummaryKpiResponse> getSummary(Instant dateFrom, Instant dateTo, Long routeId) {
        validatePeriod(dateFrom, dateTo);
        return ticketRepository.findSummary(dateFrom, dateTo, routeId, Order.Status.PAID, SOLD_TICKET_STATUSES)
                .stream()
                .map(row -> new SummaryKpiResponse(
                        row.getCurrencyCode(),
                        value(row.getSoldTicketCount()),
                        money(row.getTotalRevenue()),
                        average(row.getTotalRevenue(), row.getSoldTicketCount()),
                        value(row.getPaidOrderCount()),
                        value(row.getCustomerCount())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteRevenueResponse> getRevenueByRoute(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        validatePeriod(dateFrom, dateTo);
        return ticketRepository.findRevenueByRoute(
                        dateFrom,
                        dateTo,
                        routeId,
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(this::toRouteRevenueResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteRevenueResponse> getDemandByRoute(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        validatePeriod(dateFrom, dateTo);
        return ticketRepository.findDemandByRoute(
                        dateFrom,
                        dateTo,
                        routeId,
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(this::toRouteRevenueResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopularSegmentResponse> getPopularSegments(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        validatePeriod(dateFrom, dateTo);
        return ticketRepository.findPopularSegments(
                        dateFrom,
                        dateTo,
                        routeId,
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(row -> new PopularSegmentResponse(
                        row.getStartStationName(),
                        row.getEndStationName(),
                        row.getCurrencyCode(),
                        value(row.getTicketCount()),
                        money(row.getTotalRevenue())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripOccupancyResponse> getHighestOccupancyTrips(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        validatePeriod(dateFrom, dateTo);
        return tripRepository.findHighestOccupancyTrips(
                        dateFrom,
                        dateTo,
                        routeId,
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(this::toTripOccupancyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripOccupancyResponse> getLowestOccupancyTrips(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        validatePeriod(dateFrom, dateTo);
        return tripRepository.findLowestOccupancyTrips(
                        dateFrom,
                        dateTo,
                        routeId,
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(this::toTripOccupancyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesTrendResponse> getSalesTrend(Instant dateFrom, Instant dateTo, Long routeId) {
        validatePeriod(dateFrom, dateTo);
        return ticketRepository.findSalesTrend(dateFrom, dateTo, routeId, Order.Status.PAID, SOLD_TICKET_STATUSES)
                .stream()
                .map(row -> new SalesTrendResponse(
                        row.getSaleDate(),
                        row.getCurrencyCode(),
                        value(row.getSoldTicketCount()),
                        money(row.getTotalRevenue()),
                        value(row.getPaidOrderCount())
                ))
                .toList();
    }

    public int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private RouteRevenueResponse toRouteRevenueResponse(TicketRepository.RouteRevenueRow row) {
        return new RouteRevenueResponse(
                row.getRouteId(),
                row.getRouteNumber(),
                row.getRouteName(),
                row.getCurrencyCode(),
                value(row.getSoldTicketCount()),
                money(row.getTotalRevenue()),
                average(row.getTotalRevenue(), row.getSoldTicketCount()),
                value(row.getPaidOrderCount()),
                value(row.getCustomerCount())
        );
    }

    private TripOccupancyResponse toTripOccupancyResponse(TripRepository.TripOccupancyRow row) {
        long totalSeats = value(row.getTotalSeatCount());
        long soldSeats = value(row.getSoldSeatCount());

        return new TripOccupancyResponse(
                row.getTripId(),
                row.getRouteId(),
                row.getRouteNumber(),
                row.getRouteName(),
                row.getDepartureAtUtc(),
                value(row.getCarCount()),
                totalSeats,
                soldSeats,
                occupancyPercent(soldSeats, totalSeats)
        );
    }

    private void validatePeriod(Instant dateFrom, Instant dateTo) {
        if (dateFrom == null || dateTo == null || !dateFrom.isBefore(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be earlier than dateTo");
        }
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal average(BigDecimal totalRevenue, Long ticketCount) {
        long count = value(ticketCount);
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return money(totalRevenue).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private double occupancyPercent(long soldSeats, long totalSeats) {
        if (totalSeats == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(soldSeats)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
