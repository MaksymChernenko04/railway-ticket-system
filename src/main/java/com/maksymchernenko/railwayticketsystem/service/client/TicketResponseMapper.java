package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientTicketResponse;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripSeat;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class TicketResponseMapper {

    public ClientTicketResponse toResponse(Ticket ticket) {
        TripSeat tripSeat = ticket.getTripSeat();
        Trip trip = ticket.getStartTripStop().getTrip();
        return new ClientTicketResponse(
                ticket.getId(),
                ticket.getOrder().getId(),
                trip.getId(),
                ticket.getStatus().name(),
                ticket.getPassenger().getFirstName(),
                ticket.getPassenger().getLastName(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                ticket.getStartTripStop().getRouteStop().getStation().getName(),
                ticket.getEndTripStop().getRouteStop().getStation().getName(),
                trip.getDepartureAtUtc().plus(ticket.getStartTripStop().getDepartureOffsetMinutes(), ChronoUnit.MINUTES),
                trip.getDepartureAtUtc().plus(ticket.getEndTripStop().getArrivalOffsetMinutes(), ChronoUnit.MINUTES),
                tripSeat.getTripCar().getCar().getNumber(),
                tripSeat.getTripCar().getOrdinal(),
                tripSeat.getSeat().getNumber(),
                tripSeat.getSeat().getType().getCode(),
                ticket.getPrice(),
                ticket.getCurrency().getCode(),
                ticket.getAppliedBenefitCode(),
                ticket.getAppliedBenefitPercent(),
                ticket.getOrder().getCreatedAtUtc()
        );
    }
}
