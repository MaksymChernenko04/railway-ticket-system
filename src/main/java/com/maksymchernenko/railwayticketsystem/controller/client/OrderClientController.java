package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientOrderResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPaymentResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.CompletePaymentRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.CreateOrderRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.StartPaymentRequest;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import com.maksymchernenko.railwayticketsystem.service.client.OrderClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/orders")
@RequiredArgsConstructor
public class OrderClientController {

    private final OrderClientService clientOrderService;

    @GetMapping
    public Page<ClientOrderResponse> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ) {
        return clientOrderService.getOrders(userDetails.getId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientOrderResponse createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return clientOrderService.createOrder(userDetails.getId(), request);
    }

    @GetMapping("/{orderId}")
    public ClientOrderResponse getOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
        return clientOrderService.getOrder(userDetails.getId(), orderId);
    }

    @PostMapping("/{orderId}/payments")
    public ClientPaymentResponse startPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody StartPaymentRequest request
    ) {
        return clientOrderService.startPayment(userDetails.getId(), orderId, request);
    }

    @GetMapping("/payments/{paymentId}")
    public ClientPaymentResponse getPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId
    ) {
        return clientOrderService.getPayment(userDetails.getId(), paymentId);
    }

    @PostMapping("/payments/{paymentId}/complete")
    public ClientOrderResponse completePayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId,
            @Valid @RequestBody CompletePaymentRequest request
    ) {
        return clientOrderService.completePayment(userDetails.getId(), paymentId, request);
    }
}
