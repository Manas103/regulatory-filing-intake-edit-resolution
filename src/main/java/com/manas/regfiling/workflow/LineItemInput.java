package com.manas.regfiling.workflow;

/** One submitted (schedule_code, line_item_code, raw_value) triple for a filing being created. */
public record LineItemInput(String scheduleCode, String lineItemCode, String rawValue) {
}
