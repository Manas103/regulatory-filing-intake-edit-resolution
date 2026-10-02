package com.manas.regfiling.workflow;

import com.manas.regfiling.domain.EditFailure;
import com.manas.regfiling.domain.FilingStatus;

import java.util.List;

public record SubmissionResult(FilingStatus status, List<EditFailure> failures) {
}
