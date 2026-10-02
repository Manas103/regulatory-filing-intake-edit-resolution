package com.manas.regfiling.schema;

import java.util.List;

/**
 * One cross schedule or intra schedule tie out: a target line item that must equal the sum of
 * its component line items. The failure is reported against the target's schedule and line item
 * code, the canonical place a reviewer of the published form would look first.
 */
public record TieOutDefinition(
        String id,
        String targetScheduleCode,
        String targetLineItemCode,
        List<LineItemRef> addedComponents,
        List<LineItemRef> subtractedComponents
) {
    public TieOutDefinition(String id, String targetScheduleCode, String targetLineItemCode,
                             List<LineItemRef> addedComponents) {
        this(id, targetScheduleCode, targetLineItemCode, addedComponents, List.of());
    }

    public record LineItemRef(String scheduleCode, String lineItemCode) {
    }
}
