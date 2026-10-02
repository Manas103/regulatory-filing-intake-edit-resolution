package com.manas.regfiling.rules;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;

/**
 * QUALITY: the nonaccrual-to-total-loans ratio (RC-N:4 / HC-C:9) is outside the plausible band
 * [0, 0.15]. Held, not blocked, since an institution genuinely in distress can legitimately
 * exceed this band; the point is to surface it for an analyst to look at, not to reject it.
 */
@Component
public class RatioOutOfBandRule implements EditRule {

    private static final BigDecimal LOWER = BigDecimal.ZERO;
    private static final BigDecimal UPPER = new BigDecimal("0.15");

    @Override
    public RuleId id() {
        return RuleId.RATIO_OUT_OF_BAND;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        Optional<BigDecimal> nonaccrual = ctx.numeric("RC-N", "4");
        Optional<BigDecimal> totalLoans = ctx.numeric("HC-C", "9");
        if (nonaccrual.isEmpty() || totalLoans.isEmpty() || totalLoans.get().compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }
        BigDecimal ratio = nonaccrual.get().divide(totalLoans.get(), MathContext.DECIMAL64);
        if (ratio.compareTo(LOWER) < 0 || ratio.compareTo(UPPER) > 0) {
            return List.of(new EditFailureDraft(id(), "RC-N", "4",
                    "Nonaccrual ratio RC-N:4 / HC-C:9 = "
                            + ratio.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                            + "% is outside the plausible band [0%, 15%]"));
        }
        return List.of();
    }
}
