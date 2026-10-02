package com.manas.regfiling.rules;

import com.manas.regfiling.schema.DeclaredLineItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * QUALITY: a line item flagged as one where variance is expected (the two RC-N past due and
 * nonaccrual lines, which almost never stay bit for bit identical across quarters at a real
 * institution) reports the exact same value for the current period and the two most recent
 * prior periods. Held, not blocked, since a value can legitimately be static, it is just
 * unusual enough to want an analyst's eyes on it. Skipped when fewer than two prior periods
 * exist, there is nothing to compare a flatline against yet.
 */
@Component
public class FlatlinedValueRule implements EditRule {

    @Override
    public RuleId id() {
        return RuleId.FLATLINED_VALUE;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        if (ctx.priorPeriods().size() < 2) {
            return failures;
        }
        PriorPeriod prior1 = ctx.priorPeriods().get(0);
        PriorPeriod prior2 = ctx.priorPeriods().get(1);
        for (DeclaredLineItem item : ctx.catalog().lineItems(ctx.filing().getReportType())) {
            if (!item.varianceExpected()) {
                continue;
            }
            Optional<BigDecimal> current = ctx.numeric(item.scheduleCode(), item.lineItemCode());
            Optional<BigDecimal> p1 = prior1.numeric(item.scheduleCode(), item.lineItemCode());
            Optional<BigDecimal> p2 = prior2.numeric(item.scheduleCode(), item.lineItemCode());
            if (current.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
                continue;
            }
            if (current.get().compareTo(p1.get()) == 0 && current.get().compareTo(p2.get()) == 0) {
                failures.add(new EditFailureDraft(id(), item.scheduleCode(), item.lineItemCode(),
                        "Line item " + item.scheduleCode() + ":" + item.lineItemCode() + " reported "
                                + current.get() + " identically for the last three periods including "
                                + ctx.filing().getPeriodEndDate() + ", flatlined where variance is expected"));
            }
        }
        return failures;
    }
}
