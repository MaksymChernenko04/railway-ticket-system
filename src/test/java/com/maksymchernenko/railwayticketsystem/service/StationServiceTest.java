package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.StationCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.StationResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CountryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private StationService stationService;

    @Test
    void getAllStationsShouldMapEntitiesToResponses() {
        Pageable pageable = PageRequest.of(0, 2);

        Country country = new Country();
        country.setCode("UA");

        Station station = new Station();
        station.setId(1L);
        station.setName("Kyiv");
        station.setCountry(country);
        station.setTimezone("Europe/Kyiv");
        station.setAddress("Vokzalna Sq.");
        station.setAccessible(true);

        when(stationRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(station), pageable, 1));

        Page<StationResponse> result = stationService.getAllStations(pageable);

        assertThat(result.getContent()).containsExactly(
                new StationResponse(1L, "Kyiv", "UA", "Europe/Kyiv", "Vokzalna Sq.", true)
        );
    }

    @Test
    void createStationShouldTrimValuesNormalizeCountryCodeAndReturnResponse() {
        StationCreateRequest request = new StationCreateRequest(
                "  Kyiv-Pasazhyrskyi  ",
                " ua ",
                " Europe/Kyiv ",
                "  Vokzalna Sq.  ",
                true
        );

        Country country = new Country();
        country.setCode("UA");

        Station savedStation = new Station();
        savedStation.setId(10L);
        savedStation.setName("Kyiv-Pasazhyrskyi");
        savedStation.setCountry(country);
        savedStation.setTimezone("Europe/Kyiv");
        savedStation.setAddress("Vokzalna Sq.");
        savedStation.setAccessible(true);

        when(stationRepository.existsByCountry_CodeIgnoreCaseAndNameIgnoreCase("UA", "Kyiv-Pasazhyrskyi"))
                .thenReturn(false);
        when(countryRepository.findById("UA")).thenReturn(Optional.of(country));
        when(stationRepository.save(any(Station.class))).thenReturn(savedStation);

        StationResponse result = stationService.createStation(request);

        ArgumentCaptor<Station> stationCaptor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository).save(stationCaptor.capture());
        Station stationToSave = stationCaptor.getValue();

        assertThat(stationToSave.getName()).isEqualTo("Kyiv-Pasazhyrskyi");
        assertThat(stationToSave.getCountry()).isSameAs(country);
        assertThat(stationToSave.getTimezone()).isEqualTo("Europe/Kyiv");
        assertThat(stationToSave.getAddress()).isEqualTo("Vokzalna Sq.");
        assertThat(stationToSave.getAccessible()).isTrue();
        assertThat(result).isEqualTo(new StationResponse(
                10L,
                "Kyiv-Pasazhyrskyi",
                "UA",
                "Europe/Kyiv",
                "Vokzalna Sq.",
                true
        ));
    }

    @Test
    void createStationShouldThrowWhenStationAlreadyExists() {
        StationCreateRequest request = new StationCreateRequest(
                "  Kyiv  ",
                " ua ",
                "Europe/Kyiv",
                null,
                true
        );

        when(stationRepository.existsByCountry_CodeIgnoreCaseAndNameIgnoreCase("UA", "Kyiv")).thenReturn(true);

        assertThatThrownBy(() -> stationService.createStation(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Station already exists for this country");

        verify(countryRepository, never()).findById(any());
        verify(stationRepository, never()).save(any());
    }

    @Test
    void createStationShouldThrowWhenCountryDoesNotExist() {
        StationCreateRequest request = new StationCreateRequest(
                "Lviv",
                " ua ",
                "Europe/Kyiv",
                null,
                false
        );

        when(stationRepository.existsByCountry_CodeIgnoreCaseAndNameIgnoreCase("UA", "Lviv")).thenReturn(false);
        when(countryRepository.findById("UA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.createStation(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Country not found: UA");

        verify(stationRepository, never()).save(any());
    }

}
