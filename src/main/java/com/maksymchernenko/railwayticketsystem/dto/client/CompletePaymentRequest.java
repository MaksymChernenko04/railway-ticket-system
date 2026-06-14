package com.maksymchernenko.railwayticketsystem.dto.client;

import jakarta.validation.constraints.Size;

public record CompletePaymentRequest(
        @Size(max = 32, message = "Card number must not exceed 32 characters")
        String cardNumber
) {
}
