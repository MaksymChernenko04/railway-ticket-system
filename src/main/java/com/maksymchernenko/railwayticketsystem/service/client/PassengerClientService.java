package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientBenefitResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPassengerCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPassengerResponse;

import java.util.List;

public interface PassengerClientService {

    List<ClientPassengerResponse> getPassengers(Long userId);

    List<ClientBenefitResponse> getBenefits();

    ClientPassengerResponse createPassenger(Long userId, ClientPassengerCreateRequest request);

    void deletePassenger(Long userId, Long passengerId);
}
