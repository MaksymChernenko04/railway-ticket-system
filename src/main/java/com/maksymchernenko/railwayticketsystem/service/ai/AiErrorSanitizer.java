package com.maksymchernenko.railwayticketsystem.service.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiErrorSanitizer {

    public String sanitize(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message) && exception.getCause() != null) {
            message = exception.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            message = exception.getClass().getSimpleName();
        }
        return message
                .replaceAll("AIza[0-9A-Za-z_\\-]{20,}", "[redacted-api-key]")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
