package com.manas.regfiling.rules;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every registered {@link EditRule} against a filing and collects every failure. Spring
 * injects the full list of rule beans; the order below is just the order failures are returned
 * in, it has no bearing on the workflow decision (that only cares about the set of severities
 * present, computed in FilingWorkflowService).
 */
@Component
public class EditEngine {

    private final List<EditRule> rules;

    public EditEngine(List<EditRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<EditFailureDraft> evaluate(FilingEvaluationContext ctx) {
        List<EditFailureDraft> all = new ArrayList<>();
        for (EditRule rule : rules) {
            all.addAll(rule.evaluate(ctx));
        }
        return all;
    }

    public int ruleCount() {
        return rules.size();
    }
}
