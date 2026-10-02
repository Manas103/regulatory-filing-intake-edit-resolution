package com.manas.regfiling.rules;

import com.manas.regfiling.domain.Filing;
import com.manas.regfiling.domain.LineItemValue;
import com.manas.regfiling.schema.FilingSchemaCatalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Everything a rule needs to evaluate one filing: the filing itself, the values it submitted,
 * the declared schema for its report type, and its prior periods ordered most recent first
 * (needed by the period over period and flatline quality rules). Built fresh for every
 * evaluation, never mutated by a rule.
 */
public class FilingEvaluationContext {

    private final Filing filing;
    private final List<LineItemValue> values;
    private final FilingSchemaCatalog catalog;
    private final List<PriorPeriod> priorPeriods;

    public FilingEvaluationContext(Filing filing, List<LineItemValue> values, FilingSchemaCatalog catalog,
                                    List<PriorPeriod> priorPeriods) {
        this.filing = filing;
        this.values = values;
        this.catalog = catalog;
        this.priorPeriods = priorPeriods;
    }

    public Filing filing() {
        return filing;
    }

    public List<LineItemValue> values() {
        return values;
    }

    public FilingSchemaCatalog catalog() {
        return catalog;
    }

    public List<PriorPeriod> priorPeriods() {
        return priorPeriods;
    }

    public Optional<LineItemValue> find(String scheduleCode, String lineItemCode) {
        return values.stream()
                .filter(v -> v.getScheduleCode().equals(scheduleCode) && v.getLineItemCode().equals(lineItemCode))
                .findFirst();
    }

    public Optional<BigDecimal> numeric(String scheduleCode, String lineItemCode) {
        return find(scheduleCode, lineItemCode).flatMap(v -> {
            try {
                return Optional.of(new BigDecimal(v.getRawValue().trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }
}
