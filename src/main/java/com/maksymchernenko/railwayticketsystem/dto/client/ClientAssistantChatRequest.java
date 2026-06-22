package com.maksymchernenko.railwayticketsystem.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientAssistantChatRequest(
        String conversationId,
        @NotBlank @Size(max = 2000) String message
) {
}
