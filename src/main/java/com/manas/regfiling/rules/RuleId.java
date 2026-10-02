package com.manas.regfiling.rules;

import com.manas.regfiling.domain.EditSeverity;

/** The nine named edit rule types this engine implements, five validity and four quality. */
public enum RuleId {
    REQUIRED_MISSING(EditSeverity.VALIDITY),
    NON_NUMERIC_TYPE(EditSeverity.VALIDITY),
    NEGATIVE_NOT_ALLOWED(EditSeverity.VALIDITY),
    CROSS_SCHEDULE_TIE_OUT(EditSeverity.VALIDITY),
    UNDECLARED_REFERENCE(EditSeverity.VALIDITY),

    PERIOD_OVER_PERIOD_CHANGE(EditSeverity.QUALITY),
    RATIO_OUT_OF_BAND(EditSeverity.QUALITY),
    FLATLINED_VALUE(EditSeverity.QUALITY),
    NET_INTEREST_MARGIN_BAND(EditSeverity.QUALITY);

    private final EditSeverity severity;

    RuleId(EditSeverity severity) {
        this.severity = severity;
    }

    public EditSeverity severity() {
        return severity;
    }
}
