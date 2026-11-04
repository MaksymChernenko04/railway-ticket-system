package com.maksymchernenko.railwayticketsystem.service.common;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AnalyticsMath {

    public long zeroIfNull(Long value) {
        return value == null ? 0 : value;
    }

    public BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal average(BigDecimal total, Long count) {
        long safeCount = zeroIfNull(count);
        if (safeCount == 0) {
            return BigDecimal.ZERO;
        }
        return zeroIfNull(total).divide(BigDecimal.valueOf(safeCount), 2, RoundingMode.HALF_UP);
    }

    public double percent(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
