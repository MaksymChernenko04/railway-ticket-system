package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
