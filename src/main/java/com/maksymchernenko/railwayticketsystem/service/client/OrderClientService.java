package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientOrderResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPaymentResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.CompletePaymentRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.CreateOrderRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.StartPaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderClientService {

    ClientOrderResponse createOrder(Long userId, CreateOrderRequest request);

    ClientOrderResponse getOrder(Long userId, Long orderId);

    Page<ClientOrderResponse> getOrders(Long userId, Pageable pageable);

    ClientPaymentResponse startPayment(Long userId, Long orderId, StartPaymentRequest request);

    ClientPaymentResponse getPayment(Long userId, Long paymentId);

    ClientOrderResponse completePayment(Long userId, Long paymentId, CompletePaymentRequest request);
}
