package com.manas.regfiling.support;

import com.manas.regfiling.rules.RuleId;
import com.manas.regfiling.workflow.LineItemInput;

import java.util.List;

/**
 * One seeded defective filing: exactly one injected defect, the rule and exact (schedule, line)
 * location it is expected to fire at, the current period's line items, and zero or more prior
 * periods (newest first) the engine needs for the temporal quality rules.
 */
public record SeededDefect(
        String label,
        RuleId expectedRule,
        String expectedSchedule,
        String expectedLine,
        List<LineItemInput> currentItems,
        List<List<LineItemInput>> priorPeriodsNewestFirst
) {
    public SeededDefect(String label, RuleId expectedRule, String expectedSchedule, String expectedLine,
                         List<LineItemInput> currentItems) {
        this(label, expectedRule, expectedSchedule, expectedLine, currentItems, List.of());
    }
}
