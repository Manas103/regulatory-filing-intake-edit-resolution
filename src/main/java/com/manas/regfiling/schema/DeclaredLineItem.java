package com.manas.regfiling.schema;

/**
 * One line item declared for a report type: the published form's schedule and line item code,
 * whether it is required, whether a negative value is allowed, and whether period over period
 * variance is expected (used by the flatline quality rule, which only fires for line items that
 * are supposed to move, not ones that are legitimately static).
 */
public record DeclaredLineItem(
        String scheduleCode,
        String lineItemCode,
        String label,
        boolean required,
        boolean nonNegativeOnly,
        boolean varianceExpected
) {
    public String key() {
        return scheduleCode + ":" + lineItemCode;
    }
}
