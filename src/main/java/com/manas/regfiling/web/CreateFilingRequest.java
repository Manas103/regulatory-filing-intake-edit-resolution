package com.manas.regfiling.web;

import java.time.LocalDate;
import java.util.List;

public record CreateFilingRequest(String institutionId, String reportType, LocalDate periodEndDate,
                                   List<LineItemDto> lineItems) {

    public record LineItemDto(String scheduleCode, String lineItemCode, String rawValue) {
    }
}
