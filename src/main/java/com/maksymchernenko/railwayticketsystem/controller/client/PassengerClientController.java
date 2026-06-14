package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientBenefitResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPassengerCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPassengerResponse;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import com.maksymchernenko.railwayticketsystem.service.client.PassengerClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/passengers")
@RequiredArgsConstructor
public class PassengerClientController {

    private final PassengerClientService clientPassengerService;

    @GetMapping
    public List<ClientPassengerResponse> getPassengers(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return clientPassengerService.getPassengers(userDetails.getId());
    }

    @GetMapping("/benefits")
    public List<ClientBenefitResponse> getBenefits() {
        return clientPassengerService.getBenefits();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientPassengerResponse createPassenger(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ClientPassengerCreateRequest request
    ) {
        return clientPassengerService.createPassenger(userDetails.getId(), request);
    }

    @DeleteMapping("/{passengerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePassenger(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long passengerId
    ) {
        clientPassengerService.deletePassenger(userDetails.getId(), passengerId);
    }
}
