package com.manas.regfiling.rules;

import com.manas.regfiling.schema.DeclaredLineItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** VALIDITY: a line item that only allows non-negative values was submitted negative. */
@Component
public class NegativeNotAllowedRule implements EditRule {

    @Override
    public RuleId id() {
        return RuleId.NEGATIVE_NOT_ALLOWED;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        for (DeclaredLineItem item : ctx.catalog().lineItems(ctx.filing().getReportType())) {
            if (!item.nonNegativeOnly()) {
                continue;
            }
            ctx.numeric(item.scheduleCode(), item.lineItemCode()).ifPresent(value -> {
                if (value.compareTo(BigDecimal.ZERO) < 0) {
                    failures.add(new EditFailureDraft(id(), item.scheduleCode(), item.lineItemCode(),
                            "Line item " + item.scheduleCode() + ":" + item.lineItemCode()
                                    + " only allows non-negative values, got " + value));
                }
            });
        }
        return failures;
    }
}
