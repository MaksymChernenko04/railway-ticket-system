package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByRouteIdIn(Collection<Long> routeIds);

    @Query("""
            select distinct tr
            from Trip tr
            join tr.tripStops originStop
            join tr.tripStops destinationStop
            where originStop.routeStop.station.id = :originStationId
              and destinationStop.routeStop.station.id = :destinationStationId
              and originStop.departureOffsetMinutes < destinationStop.arrivalOffsetMinutes
            """)
    List<Trip> findTripsContainingSegment(
            @Param("originStationId") Long originStationId,
            @Param("destinationStationId") Long destinationStationId
    );

    @Query(value = """
            select tr.*
            from route_stop origin_route_stop
            join route_stop destination_route_stop
                on destination_route_stop.route_id = origin_route_stop.route_id
               and destination_route_stop.ordinal > origin_route_stop.ordinal
            join trip tr on tr.route_id = origin_route_stop.route_id
            join trip_stop origin_stop
                on origin_stop.trip_id = tr.id
               and origin_stop.route_stop_id = origin_route_stop.id
            join trip_stop destination_stop
                on destination_stop.trip_id = tr.id
               and destination_stop.route_stop_id = destination_route_stop.id
            where origin_route_stop.station_id = :originStationId
              and destination_route_stop.station_id = :destinationStationId
              and tr.departure_at_utc < :departureToUtc
              and origin_stop.departure_offset_minutes < destination_stop.arrival_offset_minutes
              and timestampadd(minute, origin_stop.departure_offset_minutes, tr.departure_at_utc) >= :departureFromUtc
              and timestampadd(minute, origin_stop.departure_offset_minutes, tr.departure_at_utc) < :departureToUtc
              and timestampadd(minute, origin_stop.departure_offset_minutes, tr.departure_at_utc) > :nowUtc
            order by timestampadd(minute, origin_stop.departure_offset_minutes, tr.departure_at_utc) asc
            """, nativeQuery = true)
    List<Trip> findTripsContainingSegmentDepartingBetween(
            @Param("originStationId") Long originStationId,
            @Param("destinationStationId") Long destinationStationId,
            @Param("departureFromUtc") Instant departureFromUtc,
            @Param("departureToUtc") Instant departureToUtc,
            @Param("nowUtc") Instant nowUtc
    );

    @Query("""
            select tr
            from Trip tr
            where tr.departureAtUtc >= :dateFrom
              and tr.departureAtUtc < :dateTo
              and (:routeId is null or tr.route.id = :routeId)
            order by tr.departureAtUtc asc
            """)
    List<Trip> findTripsInPeriod(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            Pageable pageable
    );

    @Query("""
            select
                tr.id as tripId,
                tr.route.id as routeId,
                tr.route.number as routeNumber,
                tr.route.name as routeName,
                tr.departureAtUtc as departureAtUtc,
                count(distinct tc.id) as carCount,
                count(distinct ts.id) as totalSeatCount,
                count(distinct case
                    when o.status = :paidStatus and t.status in :ticketStatuses
                    then t.tripSeat.id
                end) as soldSeatCount
            from Trip tr
            left join tr.tripCars tc
            left join tc.tripSeats ts
            left join Ticket t on t.tripSeat = ts
            left join t.order o
            where tr.departureAtUtc >= :dateFrom
              and tr.departureAtUtc < :dateTo
              and (:routeId is null or tr.route.id = :routeId)
            group by tr.id, tr.route.id, tr.route.number, tr.route.name, tr.departureAtUtc
            having count(distinct ts.id) > 0
            order by
                case when count(distinct ts.id) = 0 then 0
                     else (count(distinct case
                         when o.status = :paidStatus and t.status in :ticketStatuses
                         then t.tripSeat.id
                     end) * 1.0 / count(distinct ts.id))
                end desc,
                tr.departureAtUtc asc
            """)
    List<TripOccupancyRow> findHighestOccupancyTrips(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") com.maksymchernenko.railwayticketsystem.model.booking.Order.Status paidStatus,
            @Param("ticketStatuses") Collection<com.maksymchernenko.railwayticketsystem.model.booking.Ticket.Status> ticketStatuses,
            Pageable pageable
    );

    @Query("""
            select
                tr.id as tripId,
                tr.route.id as routeId,
                tr.route.number as routeNumber,
                tr.route.name as routeName,
                tr.departureAtUtc as departureAtUtc,
                count(distinct tc.id) as carCount,
                count(distinct ts.id) as totalSeatCount,
                count(distinct case
                    when o.status = :paidStatus and t.status in :ticketStatuses
                    then t.tripSeat.id
                end) as soldSeatCount
            from Trip tr
            left join tr.tripCars tc
            left join tc.tripSeats ts
            left join Ticket t on t.tripSeat = ts
            left join t.order o
            where tr.departureAtUtc >= :dateFrom
              and tr.departureAtUtc < :dateTo
              and (:routeId is null or tr.route.id = :routeId)
            group by tr.id, tr.route.id, tr.route.number, tr.route.name, tr.departureAtUtc
            having count(distinct ts.id) > 0
            order by
                case when count(distinct ts.id) = 0 then 0
                     else (count(distinct case
                         when o.status = :paidStatus and t.status in :ticketStatuses
                         then t.tripSeat.id
                     end) * 1.0 / count(distinct ts.id))
                end asc,
                tr.departureAtUtc asc
            """)
    List<TripOccupancyRow> findLowestOccupancyTrips(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("routeId") Long routeId,
            @Param("paidStatus") com.maksymchernenko.railwayticketsystem.model.booking.Order.Status paidStatus,
            @Param("ticketStatuses") Collection<com.maksymchernenko.railwayticketsystem.model.booking.Ticket.Status> ticketStatuses,
            Pageable pageable
    );

    @Query("""
            select
                tr.id as tripId,
                count(distinct tc.id) as carCount,
                count(distinct ts.id) as totalSeatCount,
                count(distinct case
                    when o.status = :paidStatus and t.status in :ticketStatuses
                    then t.tripSeat.id
                end) as soldSeatCount
            from Trip tr
            left join tr.tripCars tc
            left join tc.tripSeats ts
            left join Ticket t on t.tripSeat = ts
            left join t.order o
            where tr.id in :tripIds
            group by tr.id
            """)
    List<TripCapacityRow> findCapacityByTripIds(
            @Param("tripIds") Collection<Long> tripIds,
            @Param("paidStatus") com.maksymchernenko.railwayticketsystem.model.booking.Order.Status paidStatus,
            @Param("ticketStatuses") Collection<com.maksymchernenko.railwayticketsystem.model.booking.Ticket.Status> ticketStatuses
    );

    interface TripOccupancyRow {
        Long getTripId();

        Long getRouteId();

        String getRouteNumber();

        String getRouteName();

        Instant getDepartureAtUtc();

        Long getCarCount();

        Long getTotalSeatCount();

        Long getSoldSeatCount();
    }

    interface TripCapacityRow {
        Long getTripId();

        Long getCarCount();

        Long getTotalSeatCount();

        Long getSoldSeatCount();
    }
}
