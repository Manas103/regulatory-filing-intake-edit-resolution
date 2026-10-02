package com.manas.regfiling.rules;

import com.manas.regfiling.schema.DeclaredLineItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** VALIDITY: a line item declared required for this report type has no submitted value at all. */
@Component
public class RequiredMissingRule implements EditRule {

    @Override
    public RuleId id() {
        return RuleId.REQUIRED_MISSING;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        for (DeclaredLineItem item : ctx.catalog().lineItems(ctx.filing().getReportType())) {
            if (item.required() && ctx.find(item.scheduleCode(), item.lineItemCode()).isEmpty()) {
                failures.add(new EditFailureDraft(id(), item.scheduleCode(), item.lineItemCode(),
                        "Required line item " + item.scheduleCode() + ":" + item.lineItemCode()
                                + " (" + item.label() + ") was not submitted"));
            }
        }
        return failures;
    }
}
