package com.manas.regfiling.web;

import com.manas.regfiling.domain.EditFailure;
import com.manas.regfiling.domain.Filing;

import java.util.List;

public record FilingView(Long id, String institutionId, String reportType, String periodEndDate,
                          String status, List<EditFailureView> editFailures) {

    public record EditFailureView(Long id, String ruleId, String severity, String scheduleCode,
                                   String lineItemCode, String message, String explanation) {
        public static EditFailureView of(EditFailure f) {
            return new EditFailureView(f.getId(), f.getRuleId(), f.getSeverity().name(), f.getScheduleCode(),
                    f.getLineItemCode(), f.getMessage(), f.getExplanation());
        }
    }

    public static FilingView of(Filing filing, List<EditFailure> failures) {
        return new FilingView(filing.getId(), filing.getInstitutionId(), filing.getReportType(),
                filing.getPeriodEndDate().toString(), filing.getStatus().name(),
                failures.stream().map(EditFailureView::of).toList());
    }
}
