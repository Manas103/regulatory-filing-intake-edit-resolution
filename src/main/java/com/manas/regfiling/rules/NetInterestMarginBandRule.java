package com.manas.regfiling.rules;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;

/**
 * QUALITY: net interest income as a share of total assets (RI:3 / HC:12) is outside the
 * plausible band [-2%, 10%] for a single period. Held, not blocked.
 */
@Component
public class NetInterestMarginBandRule implements EditRule {

    private static final BigDecimal LOWER = new BigDecimal("-0.02");
    private static final BigDecimal UPPER = new BigDecimal("0.10");

    @Override
    public RuleId id() {
        return RuleId.NET_INTEREST_MARGIN_BAND;
    }

    @Override
    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        Optional<BigDecimal> netInterestIncome = ctx.numeric("RI", "3");
        Optional<BigDecimal> totalAssets = ctx.numeric("HC", "12");
        if (netInterestIncome.isEmpty() || totalAssets.isEmpty() || totalAssets.get().compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }
        BigDecimal margin = netInterestIncome.get().divide(totalAssets.get(), MathContext.DECIMAL64);
        if (margin.compareTo(LOWER) < 0 || margin.compareTo(UPPER) > 0) {
            return List.of(new EditFailureDraft(id(), "RI", "3",
                    "Net interest margin RI:3 / HC:12 = "
                            + margin.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                            + "% is outside the plausible band [-2%, 10%]"));
        }
        return List.of();
    }
}
