package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.reference.ReferenceCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.reference.ReferenceResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.reference.ReferenceValidationException;
import com.maksymchernenko.railwayticketsystem.model.reference.Benefit;
import com.maksymchernenko.railwayticketsystem.model.reference.CarType;
import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
import com.maksymchernenko.railwayticketsystem.repository.reference.BenefitRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CarTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CountryRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CurrencyRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.SeatTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.TrainTypeRepository;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceServiceTest {

    @Mock
    private TrainTypeRepository trainTypeRepository;

    @Mock
    private CarTypeRepository carTypeRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private BenefitRepository benefitRepository;

    @InjectMocks
    private ReferenceService referenceService;

    @Test
    void getReferencesShouldMapTrainTypesToResponses() {
        Pageable pageable = PageRequest.of(0, 2);

        TrainType trainType = new TrainType();
        trainType.setCode("INTERCITY");
        trainType.setPriceMultiplier(new BigDecimal("1.25"));

        when(trainTypeRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(trainType), pageable, 1));

        Page<ReferenceResponse> result = referenceService.getReferences("train-types", pageable);

        assertThat(result.getContent())
                .containsExactly(new ReferenceResponse("INTERCITY", new BigDecimal("1.25"), null));
    }

    @Test
    void getReferencesShouldMapBenefitsToResponses() {
        Pageable pageable = PageRequest.of(0, 2);

        Benefit benefit = new Benefit();
        benefit.setCode("STUDENT");
        benefit.setPercentAmount(new BigDecimal("50"));

        when(benefitRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(benefit), pageable, 1));

        Page<ReferenceResponse> result = referenceService.getReferences("benefits", pageable);

        assertThat(result.getContent())
                .containsExactly(new ReferenceResponse("STUDENT", null, new BigDecimal("50")));
    }

    @Test
    void createTrainTypeShouldNormalizeCodeValidateMultiplierAndReturnResponse() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(
                " intercity ",
                new BigDecimal("1.25"),
                null
        );

        when(trainTypeRepository.findAll()).thenReturn(List.of());
        when(trainTypeRepository.save(any(TrainType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceResponse result = referenceService.createReference("train-types", request);

        ArgumentCaptor<TrainType> trainTypeCaptor = ArgumentCaptor.forClass(TrainType.class);
        verify(trainTypeRepository).save(trainTypeCaptor.capture());
        TrainType trainTypeToSave = trainTypeCaptor.getValue();

        assertThat(trainTypeToSave.getCode()).isEqualTo("INTERCITY");
        assertThat(trainTypeToSave.getPriceMultiplier()).isEqualByComparingTo("1.25");
        assertThat(result).isEqualTo(new ReferenceResponse("INTERCITY", new BigDecimal("1.25"), null));
    }

    @Test
    void createCurrencyShouldNormalizeCodeAndReturnResponse() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(" usd ", null, null);

        when(currencyRepository.findAll()).thenReturn(List.of());
        when(currencyRepository.save(any(Currency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceResponse result = referenceService.createReference("currencies", request);

        ArgumentCaptor<Currency> currencyCaptor = ArgumentCaptor.forClass(Currency.class);
        verify(currencyRepository).save(currencyCaptor.capture());

        assertThat(currencyCaptor.getValue().getCode()).isEqualTo("USD");
        assertThat(result).isEqualTo(new ReferenceResponse("USD", null, null));
    }

    @Test
    void createCountryShouldNormalizeCodeAndReturnResponse() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(" ua ", null, null);

        when(countryRepository.findAll()).thenReturn(List.of());
        when(countryRepository.save(any(Country.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceResponse result = referenceService.createReference("countries", request);

        ArgumentCaptor<Country> countryCaptor = ArgumentCaptor.forClass(Country.class);
        verify(countryRepository).save(countryCaptor.capture());

        assertThat(countryCaptor.getValue().getCode()).isEqualTo("UA");
        assertThat(result).isEqualTo(new ReferenceResponse("UA", null, null));
    }

    @Test
    void createBenefitShouldNormalizeCodeValidatePercentAndReturnResponse() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(
                " student ",
                null,
                new BigDecimal("50")
        );

        when(benefitRepository.findAll()).thenReturn(List.of());
        when(benefitRepository.save(any(Benefit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceResponse result = referenceService.createReference("benefits", request);

        ArgumentCaptor<Benefit> benefitCaptor = ArgumentCaptor.forClass(Benefit.class);
        verify(benefitRepository).save(benefitCaptor.capture());
        Benefit benefitToSave = benefitCaptor.getValue();

        assertThat(benefitToSave.getCode()).isEqualTo("STUDENT");
        assertThat(benefitToSave.getPercentAmount()).isEqualByComparingTo("50");
        assertThat(result).isEqualTo(new ReferenceResponse("STUDENT", null, new BigDecimal("50")));
    }

    @Test
    void createReferenceShouldThrowWhenCodeAlreadyExistsIgnoringCase() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(
                " intercity ",
                new BigDecimal("1.25"),
                null
        );

        TrainType existing = new TrainType();
        existing.setCode("INTERCITY");

        when(trainTypeRepository.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> referenceService.createReference("train-types", request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Reference with this code already exists");

        verify(trainTypeRepository, never()).save(any());
    }

    @Test
    void createMultiplierReferenceShouldThrowWhenPriceMultiplierIsMissing() {
        ReferenceCreateRequest request = new ReferenceCreateRequest("INTERCITY", null, null);

        when(trainTypeRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> referenceService.createReference("train-types", request))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Price multiplier is required");

        verify(trainTypeRepository, never()).save(any());
    }

    @Test
    void createMultiplierReferenceShouldThrowWhenPriceMultiplierIsNegative() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(
                "INTERCITY",
                new BigDecimal("-0.1"),
                null
        );

        when(trainTypeRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> referenceService.createReference("train-types", request))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Price multiplier must be >= 0");

        verify(trainTypeRepository, never()).save(any());
    }

    @Test
    void createBenefitShouldThrowWhenPercentIsMissing() {
        ReferenceCreateRequest request = new ReferenceCreateRequest("STUDENT", null, null);

        when(benefitRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> referenceService.createReference("benefits", request))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Percent amount is required");

        verify(benefitRepository, never()).save(any());
    }

    @Test
    void createBenefitShouldThrowWhenPercentIsGreaterThanOneHundred() {
        ReferenceCreateRequest request = new ReferenceCreateRequest(
                "STUDENT",
                null,
                new BigDecimal("101")
        );

        when(benefitRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> referenceService.createReference("benefits", request))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Percent amount must be between 0 and 100");

        verify(benefitRepository, never()).save(any());
    }

    @Test
    void deleteReferenceShouldNormalizeCodeAndDeleteByType() {
        referenceService.deleteReference("seat-types", " window ");

        verify(seatTypeRepository).deleteById("WINDOW");
    }

    @Test
    void getReferencesShouldThrowWhenTypeIsUnsupported() {
        Pageable pageable = PageRequest.of(0, 2);

        assertThatThrownBy(() -> referenceService.getReferences("unknown", pageable))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Unsupported reference type: unknown");
    }

    @Test
    void createReferenceShouldThrowWhenTypeIsUnsupported() {
        ReferenceCreateRequest request = new ReferenceCreateRequest("CODE", null, null);

        assertThatThrownBy(() -> referenceService.createReference("unknown", request))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Unsupported reference type: unknown");
    }

    @Test
    void deleteReferenceShouldThrowWhenTypeIsUnsupported() {
        assertThatThrownBy(() -> referenceService.deleteReference("unknown", "CODE"))
                .isInstanceOf(ReferenceValidationException.class)
                .hasMessage("Unsupported reference type: unknown");
    }

    @Test
    void createOtherMultiplierReferencesShouldUseCorrespondingRepositories() {
        ReferenceCreateRequest carTypeRequest = new ReferenceCreateRequest(
                " standard ",
                new BigDecimal("1.10"),
                null
        );
        ReferenceCreateRequest seatTypeRequest = new ReferenceCreateRequest(
                " window ",
                new BigDecimal("1.05"),
                null
        );

        when(carTypeRepository.findAll()).thenReturn(List.of());
        when(carTypeRepository.save(any(CarType.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatTypeRepository.findAll()).thenReturn(List.of());
        when(seatTypeRepository.save(any(SeatType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceResponse carTypeResult = referenceService.createReference("car-types", carTypeRequest);
        ReferenceResponse seatTypeResult = referenceService.createReference("seat-types", seatTypeRequest);

        assertThat(carTypeResult).isEqualTo(new ReferenceResponse("STANDARD", new BigDecimal("1.10"), null));
        assertThat(seatTypeResult).isEqualTo(new ReferenceResponse("WINDOW", new BigDecimal("1.05"), null));
        verify(carTypeRepository).save(any(CarType.class));
        verify(seatTypeRepository).save(any(SeatType.class));
    }
}
