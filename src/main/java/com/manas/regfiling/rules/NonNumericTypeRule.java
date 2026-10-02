package com.manas.regfiling.rules;

import com.manas.regfiling.domain.LineItemValue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** VALIDITY: a submitted line item is declared numeric but its raw value does not parse as one. */
@Component
public class NonNumericTypeRule implements EditRule {

    @Override
    public RuleId id() {
        return RuleId.NON_NUMERIC_TYPE;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        for (LineItemValue v : ctx.values()) {
            if (!ctx.catalog().isDeclared(v.getScheduleCode(), v.getLineItemCode())) {
                continue; // handled by UndeclaredReferenceRule
            }
            try {
                new BigDecimal(v.getRawValue().trim());
            } catch (NumberFormatException e) {
                failures.add(new EditFailureDraft(id(), v.getScheduleCode(), v.getLineItemCode(),
                        "Value '" + v.getRawValue() + "' at " + v.getScheduleCode() + ":" + v.getLineItemCode()
                                + " is not a valid number"));
            }
        }
        return failures;
    }
}
