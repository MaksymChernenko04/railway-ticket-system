package com.maksymchernenko.railwayticketsystem.service.ai.context;

import com.maksymchernenko.railwayticketsystem.dto.ai.context.CurrentTimeInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

@Component
public class AssistantToolSupport {

    public CurrentTimeInfo currentTime() {
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        return new CurrentTimeInfo(now.toString(), zoneId.toString(), now.atZone(zoneId).toString());
    }

    public int limit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        return Math.max(1, Math.min(limit, maxLimit));
    }

    public Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    public <T> T inTransaction(TransactionTemplate transactionTemplate, Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
