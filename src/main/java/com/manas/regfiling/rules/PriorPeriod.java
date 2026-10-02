package com.manas.regfiling.rules;

import com.manas.regfiling.domain.LineItemValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A previously filed period for the same institution and report type, most recent first. */
public record PriorPeriod(LocalDate periodEndDate, List<LineItemValue> values) {

    public Optional<BigDecimal> numeric(String scheduleCode, String lineItemCode) {
        return values.stream()
                .filter(v -> v.getScheduleCode().equals(scheduleCode) && v.getLineItemCode().equals(lineItemCode))
                .findFirst()
                .flatMap(v -> {
                    try {
                        return Optional.of(new BigDecimal(v.getRawValue().trim()));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }
}
