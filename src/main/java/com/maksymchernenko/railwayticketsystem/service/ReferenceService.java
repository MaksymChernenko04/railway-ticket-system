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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReferenceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TrainTypeRepository trainTypeRepository;
    private final CarTypeRepository carTypeRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final CountryRepository countryRepository;
    private final BenefitRepository benefitRepository;

    @Transactional(readOnly = true)
    public Page<ReferenceResponse> getReferences(String type, Pageable pageable) {
        return switch (type) {
            case "train-types" -> trainTypeRepository.findAll(pageable).map(this::toResponse);
            case "car-types" -> carTypeRepository.findAll(pageable).map(this::toResponse);
            case "seat-types" -> seatTypeRepository.findAll(pageable).map(this::toResponse);
            case "currencies" -> currencyRepository.findAll(pageable).map(this::toResponse);
            case "countries" -> countryRepository.findAll(pageable).map(this::toResponse);
            case "benefits" -> benefitRepository.findAll(pageable).map(this::toResponse);
            default -> throw unsupportedType(type);
        };
    }

    @Transactional
    public ReferenceResponse createReference(String type, ReferenceCreateRequest request) {
        return switch (type) {
            case "train-types" -> createMultiplierReference(
                    request,
                    trainTypeRepository,
                    TrainType::new,
                    TrainType::setCode,
                    TrainType::setPriceMultiplier,
                    this::toResponse
            );
            case "car-types" -> createMultiplierReference(
                    request,
                    carTypeRepository,
                    CarType::new,
                    CarType::setCode,
                    CarType::setPriceMultiplier,
                    this::toResponse
            );
            case "seat-types" -> createMultiplierReference(
                    request,
                    seatTypeRepository,
                    SeatType::new,
                    SeatType::setCode,
                    SeatType::setPriceMultiplier,
                    this::toResponse
            );
            case "currencies" -> createCodeReference(
                    request,
                    currencyRepository,
                    Currency::new,
                    Currency::setCode,
                    this::toResponse
            );
            case "countries" -> createCodeReference(
                    request,
                    countryRepository,
                    Country::new,
                    Country::setCode,
                    this::toResponse
            );
            case "benefits" -> createBenefit(request);
            default -> throw unsupportedType(type);
        };
    }

    @Transactional
    public void deleteReference(String type, String code) {
        String normalizedCode = normalizeCode(code);

        switch (type) {
            case "train-types" -> trainTypeRepository.deleteById(normalizedCode);
            case "car-types" -> carTypeRepository.deleteById(normalizedCode);
            case "seat-types" -> seatTypeRepository.deleteById(normalizedCode);
            case "currencies" -> currencyRepository.deleteById(normalizedCode);
            case "countries" -> countryRepository.deleteById(normalizedCode);
            case "benefits" -> benefitRepository.deleteById(normalizedCode);
            default -> throw unsupportedType(type);
        }
    }

    private <T> ReferenceResponse createMultiplierReference(
            ReferenceCreateRequest request,
            JpaRepository<T, String> repository,
            EntityFactory<T> factory,
            CodeSetter<T> codeSetter,
            DecimalSetter<T> priceMultiplierSetter,
            Function<T, ReferenceResponse> mapper
    ) {
        String normalizedCode = normalizeCode(request.code());
        ensureCodeIsUnique(repository, normalizedCode);

        BigDecimal priceMultiplier = requireDecimal(request.priceMultiplier(), "Price multiplier is required");
        validateRange(priceMultiplier, ZERO, null, "Price multiplier must be >= 0");

        T entity = factory.create();
        codeSetter.set(entity, normalizedCode);
        priceMultiplierSetter.set(entity, priceMultiplier);

        return mapper.apply(repository.save(entity));
    }

    private <T> ReferenceResponse createCodeReference(
            ReferenceCreateRequest request,
            JpaRepository<T, String> repository,
            EntityFactory<T> factory,
            CodeSetter<T> codeSetter,
            Function<T, ReferenceResponse> mapper
    ) {
        String normalizedCode = normalizeCode(request.code());
        ensureCodeIsUnique(repository, normalizedCode);

        T entity = factory.create();
        codeSetter.set(entity, normalizedCode);

        return mapper.apply(repository.save(entity));
    }

    private ReferenceResponse createBenefit(ReferenceCreateRequest request) {
        String normalizedCode = normalizeCode(request.code());
        ensureCodeIsUnique(benefitRepository, normalizedCode);

        BigDecimal percentAmount = requireDecimal(request.percentAmount(), "Percent amount is required");
        validateRange(percentAmount, ZERO, ONE_HUNDRED, "Percent amount must be between 0 and 100");

        Benefit benefit = new Benefit();
        benefit.setCode(normalizedCode);
        benefit.setPercentAmount(percentAmount);

        return toResponse(benefitRepository.save(benefit));
    }

    private void ensureCodeIsUnique(JpaRepository<?, String> repository, String code) {
        boolean exists = repository.findAll().stream()
                .anyMatch(entity -> getCode(entity).equalsIgnoreCase(code));

        if (exists) {
            throw new EntityAlreadyExistsException("Reference with this code already exists");
        }
    }

    private BigDecimal requireDecimal(BigDecimal value, String message) {
        if (value == null) {
            throw new ReferenceValidationException(message);
        }

        return value;
    }

    private void validateRange(BigDecimal value, BigDecimal min, BigDecimal max, String message) {
        if (value.compareTo(min) < 0 || (max != null && value.compareTo(max) > 0)) {
            throw new ReferenceValidationException(message);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private ReferenceValidationException unsupportedType(String type) {
        return new ReferenceValidationException("Unsupported reference type: " + type);
    }

    private String getCode(Object entity) {
        return switch (entity) {
            case TrainType trainType -> trainType.getCode();
            case CarType carType -> carType.getCode();
            case SeatType seatType -> seatType.getCode();
            case Currency currency -> currency.getCode();
            case Country country -> country.getCode();
            case Benefit benefit -> benefit.getCode();
            default -> throw new ReferenceValidationException("Unsupported reference entity");
        };
    }

    private ReferenceResponse toResponse(TrainType trainType) {
        return new ReferenceResponse(trainType.getCode(), trainType.getPriceMultiplier(), null);
    }

    private ReferenceResponse toResponse(CarType carType) {
        return new ReferenceResponse(carType.getCode(), carType.getPriceMultiplier(), null);
    }

    private ReferenceResponse toResponse(SeatType seatType) {
        return new ReferenceResponse(seatType.getCode(), seatType.getPriceMultiplier(), null);
    }

    private ReferenceResponse toResponse(Currency currency) {
        return new ReferenceResponse(currency.getCode(), null, null);
    }

    private ReferenceResponse toResponse(Country country) {
        return new ReferenceResponse(country.getCode(), null, null);
    }

    private ReferenceResponse toResponse(Benefit benefit) {
        return new ReferenceResponse(benefit.getCode(), null, benefit.getPercentAmount());
    }

    @FunctionalInterface
    private interface EntityFactory<T> {
        T create();
    }

    @FunctionalInterface
    private interface CodeSetter<T> {
        void set(T entity, String code);
    }

    @FunctionalInterface
    private interface DecimalSetter<T> {
        void set(T entity, BigDecimal value);
    }
}
