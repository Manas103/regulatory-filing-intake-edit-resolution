package com.manas.regfiling.rules;

/** An edit failure a rule wants recorded, before it has been persisted and given an id. */
public record EditFailureDraft(RuleId ruleId, String scheduleCode, String lineItemCode, String message) {
}
