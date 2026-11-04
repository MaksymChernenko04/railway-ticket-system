package com.maksymchernenko.railwayticketsystem.service;

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
import com.maksymchernenko.railwayticketsystem.service.client.PassengerClientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerClientServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PassengerClientServiceImpl passengerClientService;

    @Test
    void createPassengerShouldTrimNamesAttachBenefitAndReturnResponse() {
        User user = new User();
        user.setId(USER_ID);
        Benefit benefit = new Benefit();
        benefit.setCode("STUDENT");
        benefit.setPercentAmount(new BigDecimal("50.00"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(benefitRepository.findById("STUDENT")).thenReturn(Optional.of(benefit));
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(invocation -> {
            Passenger passenger = invocation.getArgument(0);
            passenger.setId(10L);
            return passenger;
        });

        ClientPassengerResponse result = passengerClientService.createPassenger(
                USER_ID,
                new ClientPassengerCreateRequest("  Ivan  ", "  Petrenko  ", "STUDENT")
        );

        ArgumentCaptor<Passenger> passengerCaptor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).save(passengerCaptor.capture());
        Passenger toSave = passengerCaptor.getValue();
        assertThat(toSave.getUser()).isSameAs(user);
        assertThat(toSave.getFirstName()).isEqualTo("Ivan");
        assertThat(toSave.getLastName()).isEqualTo("Petrenko");
        assertThat(toSave.getBenefit()).isSameAs(benefit);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.firstName()).isEqualTo("Ivan");
        assertThat(result.lastName()).isEqualTo("Petrenko");
        assertThat(result.benefitCode()).isEqualTo("STUDENT");
        assertThat(result.benefitPercent()).isEqualByComparingTo("50.00");
    }

    @Test
    void createPassengerShouldThrowWhenBenefitIsMissing() {
        User user = new User();
        user.setId(USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(benefitRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerClientService.createPassenger(
                USER_ID,
                new ClientPassengerCreateRequest("Ivan", "Petrenko", "UNKNOWN")
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Benefit not found: UNKNOWN");

        verify(passengerRepository, never()).save(any());
    }

    @Test
    void deletePassengerShouldRejectPassengerWithTickets() {
        Passenger passenger = new Passenger();
        passenger.setId(10L);

        when(passengerRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(passenger));
        when(ticketRepository.existsByPassengerId(10L)).thenReturn(true);

        assertThatThrownBy(() -> passengerClientService.deletePassenger(USER_ID, 10L))
                .isInstanceOf(TripClientValidationException.class)
                .hasMessage("Passenger already has tickets and cannot be deleted");

        verify(passengerRepository, never()).delete(any());
    }

    @Test
    void deletePassengerShouldDeleteWhenPassengerHasNoTickets() {
        Passenger passenger = new Passenger();
        passenger.setId(10L);

        when(passengerRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(passenger));
        when(ticketRepository.existsByPassengerId(10L)).thenReturn(false);

        passengerClientService.deletePassenger(USER_ID, 10L);

        verify(passengerRepository).delete(passenger);
    }
}
