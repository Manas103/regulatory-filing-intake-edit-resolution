package com.manas.regfiling.rules;

import com.manas.regfiling.domain.LineItemValue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** VALIDITY: a submitted value references a (schedule_code, line_item_code) not declared at all
 * for this report type. The failure is reported at exactly the undeclared location submitted. */
@Component
public class UndeclaredReferenceRule implements EditRule {

    @Override
    public RuleId id() {
        return RuleId.UNDECLARED_REFERENCE;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        for (LineItemValue v : ctx.values()) {
            if (!ctx.catalog().isDeclared(v.getScheduleCode(), v.getLineItemCode())) {
                failures.add(new EditFailureDraft(id(), v.getScheduleCode(), v.getLineItemCode(),
                        "Schedule/line item " + v.getScheduleCode() + ":" + v.getLineItemCode()
                                + " is not declared for report type " + ctx.filing().getReportType()));
            }
        }
        return failures;
    }
}
