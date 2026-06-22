package com.maksymchernenko.railwayticketsystem.repository.booking;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
            select t
            from Ticket t
            where t.order.user.id = :userId
              and t.order.status = :orderStatus
            order by t.startTripStop.trip.departureAtUtc desc,
                     t.startTripStop.departureOffsetMinutes desc,
                     t.id desc
            """)
    Page<Ticket> findVisibleTicketsByUser(
            @Param("userId") Long userId,
            @Param("orderStatus") Order.Status orderStatus,
            Pageable pageable
    );

    @Query("""
            select t
            from Ticket t
            where t.order.user.id = :userId
              and t.order.status = :orderStatus
              and t.status = :ticketStatus
            """)
    List<Ticket> findTicketsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("orderStatus") Order.Status orderStatus,
            @Param("ticketStatus") Ticket.Status ticketStatus
    );

    boolean existsByPassengerId(Long passengerId);

    @Query("""
            select
                t.currency.code as currencyCode,
                count(t.id) as soldTicketCount,
                sum(t.price) as totalRevenue,
                count(distinct o.id) as paidOrderCount,
                count(distinct o.user.id) as customerCount
            from Ticket t
            join t.order o
            where o.status = :paidStatus
              and t.status in :ticketStatuses
              and o.createdAtUtc >= :dateFrom
              and o.createdAtUtc < :dateTo
              and (:routeId is null or t.startTripStop.trip.route.id = :routeId)
            group by t.currency.code
            order by sum(t.price) desc
            """)
    List<SummaryKpiRow> findSummary(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") Order.Status paidStatus,
            @Param("ticketStatuses") Collection<Ticket.Status> ticketStatuses
    );

    @Query("""
            select
                r.id as routeId,
                r.number as routeNumber,
                r.name as routeName,
                t.currency.code as currencyCode,
                count(t.id) as soldTicketCount,
                sum(t.price) as totalRevenue,
                count(distinct o.id) as paidOrderCount,
                count(distinct o.user.id) as customerCount
            from Ticket t
            join t.order o
            join t.startTripStop.trip.route r
            where o.status = :paidStatus
              and t.status in :ticketStatuses
              and o.createdAtUtc >= :dateFrom
              and o.createdAtUtc < :dateTo
              and (:routeId is null or r.id = :routeId)
            group by r.id, r.number, r.name, t.currency.code
            order by sum(t.price) desc
            """)
    List<RouteRevenueRow> findRevenueByRoute(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") Order.Status paidStatus,
            @Param("ticketStatuses") Collection<Ticket.Status> ticketStatuses,
            Pageable pageable
    );

    @Query("""
            select
                r.id as routeId,
                r.number as routeNumber,
                r.name as routeName,
                t.currency.code as currencyCode,
                count(t.id) as soldTicketCount,
                sum(t.price) as totalRevenue,
                count(distinct o.id) as paidOrderCount,
                count(distinct o.user.id) as customerCount
            from Ticket t
            join t.order o
            join t.startTripStop.trip.route r
            where o.status = :paidStatus
              and t.status in :ticketStatuses
              and o.createdAtUtc >= :dateFrom
              and o.createdAtUtc < :dateTo
              and (:routeId is null or r.id = :routeId)
            group by r.id, r.number, r.name, t.currency.code
            order by count(t.id) desc, sum(t.price) desc
            """)
    List<RouteRevenueRow> findDemandByRoute(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") Order.Status paidStatus,
            @Param("ticketStatuses") Collection<Ticket.Status> ticketStatuses,
            Pageable pageable
    );

    @Query("""
            select
                startStop.routeStop.station.name as startStationName,
                endStop.routeStop.station.name as endStationName,
                t.currency.code as currencyCode,
                count(t.id) as ticketCount,
                sum(t.price) as totalRevenue
            from Ticket t
            join t.order o
            join t.startTripStop startStop
            join t.endTripStop endStop
            where o.status = :paidStatus
              and t.status in :ticketStatuses
              and o.createdAtUtc >= :dateFrom
              and o.createdAtUtc < :dateTo
              and (:routeId is null or startStop.trip.route.id = :routeId)
            group by startStop.routeStop.station.name, endStop.routeStop.station.name, t.currency.code
            order by count(t.id) desc, sum(t.price) desc
            """)
    List<PopularSegmentRow> findPopularSegments(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") Order.Status paidStatus,
            @Param("ticketStatuses") Collection<Ticket.Status> ticketStatuses,
            Pageable pageable
    );

    @Query("""
            select
                function('date', o.createdAtUtc) as saleDate,
                t.currency.code as currencyCode,
                count(t.id) as soldTicketCount,
                sum(t.price) as totalRevenue,
                count(distinct o.id) as paidOrderCount
            from Ticket t
            join t.order o
            join t.startTripStop.trip.route r
            where o.status = :paidStatus
              and t.status in :ticketStatuses
              and o.createdAtUtc >= :dateFrom
              and o.createdAtUtc < :dateTo
              and (:routeId is null or r.id = :routeId)
            group by function('date', o.createdAtUtc), t.currency.code
            order by function('date', o.createdAtUtc) asc
            """)
    List<SalesTrendRow> findSalesTrend(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") Order.Status paidStatus,
            @Param("ticketStatuses") Collection<Ticket.Status> ticketStatuses
    );

    @Query("""
            select distinct t.tripSeat.id
            from Ticket t
            join t.order o
            where t.startTripStop.trip.id = :tripId
              and (o.status = :paidStatus or (o.status = :pendingStatus and o.expiresAtUtc > :now))
              and t.status in :ticketStatuses
              and t.startTripStop.departureOffsetMinutes < :requestedEndArrivalOffsetMinutes
              and :requestedStartDepartureOffsetMinutes < t.endTripStop.arrivalOffsetMinutes
            """)
    List<Integer> findOccupiedTripSeatIdsForSegment(
            @Param("tripId") Long tripId,
            @Param("requestedStartDepartureOffsetMinutes") Integer requestedStartDepartureOffsetMinutes,
            @Param("requestedEndArrivalOffsetMinutes") Integer requestedEndArrivalOffsetMinutes,
            @Param("paidStatus") Order.Status paidStatus,
            @Param("pendingStatus") Order.Status pendingStatus,
            @Param("now") Instant now,
            @Param("ticketStatuses") Collection<Ticket.Status> ticketStatuses
    );

    interface SummaryKpiRow {
        String getCurrencyCode();

        Long getSoldTicketCount();

        BigDecimal getTotalRevenue();

        Long getPaidOrderCount();

        Long getCustomerCount();
    }

    interface RouteRevenueRow {
        Long getRouteId();

        String getRouteNumber();

        String getRouteName();

        String getCurrencyCode();

        Long getSoldTicketCount();

        BigDecimal getTotalRevenue();

        Long getPaidOrderCount();

        Long getCustomerCount();
    }

    interface PopularSegmentRow {
        String getStartStationName();

        String getEndStationName();

        String getCurrencyCode();

        Long getTicketCount();

        BigDecimal getTotalRevenue();
    }

    interface SalesTrendRow {
        LocalDate getSaleDate();

        String getCurrencyCode();

        Long getSoldTicketCount();

        BigDecimal getTotalRevenue();

        Long getPaidOrderCount();
    }
}
