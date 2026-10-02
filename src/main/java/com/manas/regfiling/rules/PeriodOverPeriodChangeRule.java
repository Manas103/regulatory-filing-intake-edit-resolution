package com.manas.regfiling.rules;

import com.manas.regfiling.schema.DeclaredLineItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * QUALITY: a line item moved more than the plausibility threshold (30%) from the most recent
 * prior period for the same institution and report type. Held for an analyst explanation, not
 * blocking, because a large swing can be entirely legitimate (a merger, a large portfolio sale).
 * Skipped when there is no prior period, or the prior value is zero (percent change undefined).
 */
@Component
public class PeriodOverPeriodChangeRule implements EditRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("0.30");

    @Override
    public RuleId id() {
        return RuleId.PERIOD_OVER_PERIOD_CHANGE;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        if (ctx.priorPeriods().isEmpty()) {
            return failures;
        }
        PriorPeriod mostRecentPrior = ctx.priorPeriods().get(0);
        for (DeclaredLineItem item : ctx.catalog().lineItems(ctx.filing().getReportType())) {
            Optional<BigDecimal> currentOpt = ctx.numeric(item.scheduleCode(), item.lineItemCode());
            Optional<BigDecimal> priorOpt = mostRecentPrior.numeric(item.scheduleCode(), item.lineItemCode());
            if (currentOpt.isEmpty() || priorOpt.isEmpty()) {
                continue;
            }
            BigDecimal prior = priorOpt.get();
            if (prior.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal current = currentOpt.get();
            BigDecimal pctChange = current.subtract(prior).abs().divide(prior.abs(), MathContext.DECIMAL64);
            if (pctChange.compareTo(THRESHOLD) > 0) {
                failures.add(new EditFailureDraft(id(), item.scheduleCode(), item.lineItemCode(),
                        "Line item " + item.scheduleCode() + ":" + item.lineItemCode() + " moved "
                                + pctChange.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP)
                                + "% from " + prior + " to " + current + " since period "
                                + mostRecentPrior.periodEndDate() + ", beyond the 30% plausibility threshold"));
            }
        }
        return failures;
    }
}
