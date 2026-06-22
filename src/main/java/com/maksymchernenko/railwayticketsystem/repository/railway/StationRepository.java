package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {

    boolean existsByCountryCodeIgnoreCaseAndNameIgnoreCase(String countryCode, String name);

    @Query("""
            select s
            from Station s
            where lower(s.name) like lower(concat('%', :query, '%'))
               or lower(s.country.code) like lower(concat('%', :query, '%'))
               or lower(s.address) like lower(concat('%', :query, '%'))
            order by s.name asc
            """)
    List<Station> searchForAssistant(@Param("query") String query);
}
