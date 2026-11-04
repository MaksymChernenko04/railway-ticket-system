package com.maksymchernenko.railwayticketsystem.dto.client;

import java.util.Map;

public record ClientRecommendationResponse(
        String message,
        String messageKey,
        Map<String, String> messageArgs,
        String actionType,
        Map<String, String> actionParams
) {

    public ClientRecommendationResponse(String message) {
        this(message, null, Map.of(), null, Map.of());
    }

    public static ClientRecommendationResponse localizedFallback(
            String message,
            String messageKey,
            Map<String, String> messageArgs
    ) {
        return new ClientRecommendationResponse(message, messageKey, messageArgs, null, Map.of());
    }

    public static ClientRecommendationResponse localized(
            String message,
            String messageKey,
            Map<String, String> messageArgs,
            String actionType,
            Map<String, String> actionParams
    ) {
        return new ClientRecommendationResponse(
                message,
                messageKey,
                messageArgs == null ? Map.of() : messageArgs,
                actionType,
                actionParams == null ? Map.of() : actionParams
        );
    }
}
