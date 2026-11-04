package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AdminAssistantChatRequest(
        String conversationId,
        String language,
        @NotNull Instant dateFrom,
        @NotNull Instant dateTo,
        Long routeId,
        @Min(1) @Max(100) Integer limit,
        @NotBlank @Size(max = 2000) String message
) {
}
