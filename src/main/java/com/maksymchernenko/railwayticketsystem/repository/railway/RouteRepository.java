package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByNumberIgnoreCase(String number);

    Optional<Route> findByNumberIgnoreCase(String number);

    @Query("""
            select distinct r
            from Route r
            left join r.routeStops rs
            left join rs.station s
            where lower(r.number) like lower(concat('%', :query, '%'))
               or lower(r.name) like lower(concat('%', :query, '%'))
               or lower(s.name) like lower(concat('%', :query, '%'))
            order by r.number asc
            """)
    List<Route> searchForAdminAssistant(@Param("query") String query);
}
