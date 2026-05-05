package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.StationCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.StationResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public Page<StationResponse> getAllStations(Pageable pageable) {
        return stationRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public StationResponse createStation(StationCreateRequest request) {
        String normalizedCountryCode = request.countryCode().trim().toUpperCase(Locale.ROOT);
        String normalizedName = request.name().trim();

        if (stationRepository.existsByCountry_CodeIgnoreCaseAndNameIgnoreCase(normalizedCountryCode, normalizedName)) {
            throw new EntityAlreadyExistsException("Station already exists for this country");
        }

        Country country = countryRepository.findById(normalizedCountryCode)
                .orElseThrow(() -> new EntityNotFoundException("Country not found: " + normalizedCountryCode));

        Station station = new Station();
        station.setName(normalizedName);
        station.setCountry(country);
        station.setTimezone(request.timezone().trim());
        station.setAddress(request.address() == null ? null : request.address().trim());
        station.setAccessible(request.accessible());

        return toResponse(stationRepository.save(station));
    }

    @Transactional
    public void deleteStation(Long stationId) {
        stationRepository.deleteById(stationId);
    }

    private StationResponse toResponse(Station station) {
        return new StationResponse(
                station.getId(),
                station.getName(),
                station.getCountry().getCode(),
                station.getTimezone(),
                station.getAddress(),
                station.getAccessible()
        );
    }
}
