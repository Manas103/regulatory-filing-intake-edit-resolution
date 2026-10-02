package com.manas.regfiling.support;

import com.manas.regfiling.rules.RuleId;
import com.manas.regfiling.workflow.LineItemInput;

import java.util.ArrayList;
import java.util.List;

/**
 * The 40 seeded defective filings behind the claim "40 of 40 seeded defects caught": one
 * injected defect per entry, spread across all nine rule types (26 validity, 14 quality),
 * varied across schedules and line items. Every entry starts from the consistent baseline
 * (every leaf value feeds every derived total, so nothing else breaks) and overrides exactly
 * the one thing the defect needs, so the engine failing to catch it would be the engine's
 * fault, not a fixture inconsistency.
 */
public final class SeededDefectCatalog {

    private SeededDefectCatalog() {
    }

    public static List<SeededDefect> all() {
        List<SeededDefect> defects = new ArrayList<>();
        defects.addAll(requiredMissing());
        defects.addAll(nonNumeric());
        defects.addAll(negativeNotAllowed());
        defects.addAll(undeclaredReference());
        defects.addAll(crossScheduleTieOut());
        defects.addAll(periodOverPeriodChange());
        defects.addAll(ratioOutOfBand());
        defects.addAll(flatlinedValue());
        defects.addAll(netInterestMarginBand());
        return defects;
    }

    private static List<SeededDefect> requiredMissing() {
        String[][] targets = {{"HC", "1.a"}, {"HC-B", "1"}, {"HC-C", "2"}, {"RI", "2"}, {"RC-N", "4"}};
        List<SeededDefect> out = new ArrayList<>();
        for (String[] t : targets) {
            List<LineItemInput> items = FilingTestFactory.removeItem(
                    FilingTestFactory.toLineItems(FilingLeafValues.baseline()), t[0], t[1]);
            out.add(new SeededDefect("required_missing_" + t[0] + "_" + t[1],
                    RuleId.REQUIRED_MISSING, t[0], t[1], items));
        }
        return out;
    }

    private static List<SeededDefect> nonNumeric() {
        String[][] targets = {{"HC", "2.a"}, {"HC-C", "1"}, {"RI", "1"}, {"RC-N", "1"}, {"HC", "12"}};
        List<SeededDefect> out = new ArrayList<>();
        for (String[] t : targets) {
            List<LineItemInput> items = FilingTestFactory.setRawValue(
                    FilingTestFactory.toLineItems(FilingLeafValues.baseline()), t[0], t[1], "NOT-A-NUMBER");
            out.add(new SeededDefect("non_numeric_" + t[0] + "_" + t[1],
                    RuleId.NON_NUMERIC_TYPE, t[0], t[1], items));
        }
        return out;
    }

    private static List<SeededDefect> negativeNotAllowed() {
        List<SeededDefect> out = new ArrayList<>();
        out.add(negOne("cash", "HC", "1.a", new FilingLeafValuesBuilder().cash(-1000)));
        out.add(negOne("htm", "HC", "2.a", new FilingLeafValuesBuilder().htm(-1000)));
        out.add(negOne("afs", "HC", "2.b", new FilingLeafValuesBuilder().afs(-1000)));
        out.add(negOne("loansRealEstate", "HC-C", "1", new FilingLeafValuesBuilder().loansRealEstate(-1000)));
        out.add(negOne("loansCommercial", "HC-C", "2", new FilingLeafValuesBuilder().loansCommercial(-1000)));
        return out;
    }

    private static SeededDefect negOne(String label, String schedule, String line, FilingLeafValuesBuilder b) {
        return new SeededDefect("negative_" + label, RuleId.NEGATIVE_NOT_ALLOWED, schedule, line,
                FilingTestFactory.toLineItems(b.build()));
    }

    private static List<SeededDefect> undeclaredReference() {
        String[][] targets = {{"HC", "99"}, {"ZZ", "1"}, {"RI", "9"}, {"HC-C", "99"}, {"RC-N", "99"}};
        List<SeededDefect> out = new ArrayList<>();
        for (String[] t : targets) {
            List<LineItemInput> items = FilingTestFactory.addItem(
                    FilingTestFactory.toLineItems(FilingLeafValues.baseline()), t[0], t[1], "123.45");
            out.add(new SeededDefect("undeclared_" + t[0] + "_" + t[1],
                    RuleId.UNDECLARED_REFERENCE, t[0], t[1], items));
        }
        return out;
    }

    private static List<SeededDefect> crossScheduleTieOut() {
        String[][] targets = {{"HC", "12"}, {"HC-B", "1"}, {"HC-B", "2"}, {"HC-C", "9"}, {"HC", "4.a"}, {"RI", "3"}};
        List<SeededDefect> out = new ArrayList<>();
        for (String[] t : targets) {
            List<LineItemInput> items = FilingTestFactory.setRawValue(
                    FilingTestFactory.toLineItems(FilingLeafValues.baseline()), t[0], t[1], "999999999");
            out.add(new SeededDefect("tie_out_" + t[0] + "_" + t[1],
                    RuleId.CROSS_SCHEDULE_TIE_OUT, t[0], t[1], items));
        }
        return out;
    }

    private static List<SeededDefect> periodOverPeriodChange() {
        Object[][] variants = {
                {"cash", "HC", "1.a", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.cash(2_000_000)},
                {"htm", "HC", "2.a", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.htm(900_000)},
                {"loansRealEstate", "HC-C", "1", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.loansRealEstate(2_500_000)},
                {"interestIncome", "RI", "1", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.interestIncome(200_000)},
                {"pastDue", "RC-N", "1", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.pastDue(50_000)},
                {"loansCommercial", "HC-C", "2", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.loansCommercial(1_200_000)},
                {"afs", "HC", "2.b", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.afs(600_000)},
                {"interestExpense", "RI", "2", (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) b -> b.interestExpense(80_000)},
        };
        List<SeededDefect> out = new ArrayList<>();
        for (Object[] v : variants) {
            String label = (String) v[0];
            String schedule = (String) v[1];
            String line = (String) v[2];
            @SuppressWarnings("unchecked")
            java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder> mutate =
                    (java.util.function.Function<FilingLeafValuesBuilder, FilingLeafValuesBuilder>) v[3];
            List<LineItemInput> priorItems = FilingTestFactory.toLineItems(FilingLeafValues.baseline());
            List<LineItemInput> currentItems = FilingTestFactory.toLineItems(mutate.apply(new FilingLeafValuesBuilder()).build());
            out.add(new SeededDefect("period_over_period_" + label, RuleId.PERIOD_OVER_PERIOD_CHANGE,
                    schedule, line, currentItems, List.of(priorItems)));
        }
        return out;
    }

    private static List<SeededDefect> ratioOutOfBand() {
        List<SeededDefect> out = new ArrayList<>();
        out.add(new SeededDefect("ratio_high", RuleId.RATIO_OUT_OF_BAND, "RC-N", "4",
                FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().nonaccrual(400_000).build())));
        out.add(new SeededDefect("ratio_very_high", RuleId.RATIO_OUT_OF_BAND, "RC-N", "4",
                FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().nonaccrual(900_000).build())));
        return out;
    }

    private static List<SeededDefect> flatlinedValue() {
        List<SeededDefect> out = new ArrayList<>();

        // RC-N:1 identical across three periods; RC-N:4 varied slightly each period so it never flatlines.
        List<LineItemInput> prior2a = FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().nonaccrual(5100).build());
        List<LineItemInput> prior1a = FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().nonaccrual(5200).build());
        List<LineItemInput> currenta = FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().nonaccrual(5300).build());
        out.add(new SeededDefect("flatlined_past_due", RuleId.FLATLINED_VALUE, "RC-N", "1", currenta,
                List.of(prior1a, prior2a)));

        // RC-N:4 identical across three periods; RC-N:1 varied slightly each period so it never flatlines.
        List<LineItemInput> prior2b = FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().pastDue(8100).build());
        List<LineItemInput> prior1b = FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().pastDue(8200).build());
        List<LineItemInput> currentb = FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().pastDue(8300).build());
        out.add(new SeededDefect("flatlined_nonaccrual", RuleId.FLATLINED_VALUE, "RC-N", "4", currentb,
                List.of(prior1b, prior2b)));
        return out;
    }

    private static List<SeededDefect> netInterestMarginBand() {
        List<SeededDefect> out = new ArrayList<>();
        out.add(new SeededDefect("margin_too_high", RuleId.NET_INTEREST_MARGIN_BAND, "RI", "3",
                FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().interestIncome(200_000).interestExpense(1_000).build())));
        out.add(new SeededDefect("margin_too_low", RuleId.NET_INTEREST_MARGIN_BAND, "RI", "3",
                FilingTestFactory.toLineItems(new FilingLeafValuesBuilder().interestIncome(1_000).interestExpense(100_000).build())));
        return out;
    }
}
