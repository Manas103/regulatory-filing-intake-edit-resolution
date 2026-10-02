package com.manas.regfiling.rules;

import com.manas.regfiling.schema.TieOutDefinition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * VALIDITY: a declared total does not equal the sum of its declared components (or, for a
 * subtraction tie out such as net interest income, the add/subtract combination).
 *
 * <p>Design tradeoff: if the target or any component is itself missing or non-numeric, this
 * rule skips the check rather than treating the absent value as zero. Otherwise a single
 * REQUIRED_MISSING or NON_NUMERIC_TYPE defect on a component would cascade into a second,
 * misleading tie out failure, and the engine would report two failures with two different
 * locations for what is really one root cause defect. The missing/non-numeric rule already
 * names the exact location of the real problem; this rule only fires when every operand is
 * present and numeric but the arithmetic itself is wrong.
 */
@Component
public class CrossScheduleTieOutRule implements EditRule {

    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    @Override
    public RuleId id() {
        return RuleId.CROSS_SCHEDULE_TIE_OUT;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> failures = new ArrayList<>();
        for (TieOutDefinition tieOut : ctx.catalog().tieOuts(ctx.filing().getReportType())) {
            Optional<BigDecimal> target = ctx.numeric(tieOut.targetScheduleCode(), tieOut.targetLineItemCode());
            if (target.isEmpty()) {
                continue;
            }
            BigDecimal expected = BigDecimal.ZERO;
            boolean allPresent = true;
            for (TieOutDefinition.LineItemRef ref : tieOut.addedComponents()) {
                Optional<BigDecimal> v = ctx.numeric(ref.scheduleCode(), ref.lineItemCode());
                if (v.isEmpty()) {
                    allPresent = false;
                    break;
                }
                expected = expected.add(v.get());
            }
            if (allPresent) {
                for (TieOutDefinition.LineItemRef ref : tieOut.subtractedComponents()) {
                    Optional<BigDecimal> v = ctx.numeric(ref.scheduleCode(), ref.lineItemCode());
                    if (v.isEmpty()) {
                        allPresent = false;
                        break;
                    }
                    expected = expected.subtract(v.get());
                }
            }
            if (!allPresent) {
                continue;
            }
            BigDecimal diff = target.get().subtract(expected).abs();
            if (diff.setScale(2, RoundingMode.HALF_UP).compareTo(EPSILON) > 0) {
                failures.add(new EditFailureDraft(id(), tieOut.targetScheduleCode(), tieOut.targetLineItemCode(),
                        "Tie out " + tieOut.id() + " failed: " + tieOut.targetScheduleCode() + ":"
                                + tieOut.targetLineItemCode() + " = " + target.get()
                                + " but components sum to " + expected));
            }
        }
        return failures;
    }
}
