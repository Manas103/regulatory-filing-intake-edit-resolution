package com.manas.regfiling.rules;

import java.util.List;

public interface EditRule {

    RuleId id();

    List<EditFailureDraft> evaluate(FilingEvaluationContext ctx);
}
