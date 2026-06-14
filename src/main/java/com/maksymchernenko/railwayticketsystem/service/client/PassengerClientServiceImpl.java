package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientBenefitResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPassengerCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPassengerResponse;
import com.maksymchernenko.railwayticketsystem.exception.client.TripClientValidationException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.auth.User;
import com.maksymchernenko.railwayticketsystem.model.booking.Passenger;
import com.maksymchernenko.railwayticketsystem.model.reference.Benefit;
import com.maksymchernenko.railwayticketsystem.repository.auth.UserRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.PassengerRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.BenefitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerClientServiceImpl implements PassengerClientService {

    private final PassengerRepository passengerRepository;
    private final BenefitRepository benefitRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClientPassengerResponse> getPassengers(Long userId) {
        return passengerRepository.findByUserIdOrderByIdAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientBenefitResponse> getBenefits() {
        return benefitRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Benefit::getCode))
                .map(benefit -> new ClientBenefitResponse(benefit.getCode(), benefit.getPercentAmount()))
                .toList();
    }

    @Transactional
    public ClientPassengerResponse createPassenger(Long userId, ClientPassengerCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        Benefit benefit = findBenefit(request.benefitCode());

        Passenger passenger = new Passenger();
        passenger.setUser(user);
        passenger.setFirstName(request.firstName().trim());
        passenger.setLastName(request.lastName().trim());
        passenger.setBenefit(benefit);

        return toResponse(passengerRepository.save(passenger));
    }

    @Transactional
    public void deletePassenger(Long userId, Long passengerId) {
        Passenger passenger = passengerRepository.findByIdAndUserId(passengerId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Passenger not found: " + passengerId));

        if (ticketRepository.existsByPassengerId(passenger.getId())) {
            throw new TripClientValidationException("Passenger already has tickets and cannot be deleted");
        }

        passengerRepository.delete(passenger);
    }

    private ClientPassengerResponse toResponse(Passenger passenger) {
        return new ClientPassengerResponse(
                passenger.getId(),
                passenger.getFirstName(),
                passenger.getLastName(),
                passenger.getBenefit() != null ? passenger.getBenefit().getCode() : null,
                passenger.getBenefit() != null ? passenger.getBenefit().getPercentAmount() : null
        );
    }

    private Benefit findBenefit(String benefitCode) {
        if (benefitCode == null || benefitCode.isBlank()) {
            return null;
        }

        return benefitRepository.findById(benefitCode)
                .orElseThrow(() -> new EntityNotFoundException("Benefit not found: " + benefitCode));
    }
}
