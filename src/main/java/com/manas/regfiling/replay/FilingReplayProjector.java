package com.manas.regfiling.replay;

import com.manas.regfiling.domain.EditSeverity;
import com.manas.regfiling.domain.FilingStatus;
import com.manas.regfiling.event.FilingEvent;
import com.manas.regfiling.event.FilingEventType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconstructs a filing's final status purely by folding its append only event log, with no
 * read of any materialized "current status" column. This is deliberately NOT a replay of the
 * STATUS_CHANGED.statusAfter values recorded along the way (that would just be reading back the
 * same column through a different door). Instead it independently recomputes the workflow
 * decision from the raw facts: did a validity edit ever fire, which quality edits fired, and
 * which of those were later explained. If this projector and the live
 * {@link com.manas.regfiling.workflow.FilingWorkflowService} ever disagree, that is a real bug
 * in one of the two, which is exactly what ReplayConsistencyTest exists to catch.
 */
@Component
public class FilingReplayProjector {

    public FilingStatus replay(List<FilingEvent> events) {
        FilingStatus status = FilingStatus.DRAFT;
        boolean hadValidity = false;
        Set<Long> qualityFailureIds = new HashSet<>();
        Set<Long> explainedFailureIds = new HashSet<>();

        for (FilingEvent event : events) {
            switch (event.getEventType()) {
                case FILING_CREATED -> status = FilingStatus.DRAFT;
                case FILING_SUBMITTED -> status = FilingStatus.SUBMITTED;
                case EDIT_FAILURE_RECORDED -> {
                    if (event.getSeverity() == EditSeverity.VALIDITY) {
                        hadValidity = true;
                    } else if (event.getSeverity() == EditSeverity.QUALITY) {
                        qualityFailureIds.add(event.getEditFailureId());
                    }
                }
                case EXPLANATION_ADDED -> explainedFailureIds.add(event.getEditFailureId());
                case STATUS_CHANGED -> {
                    // Intentionally ignored: the point of this projector is to derive the
                    // status from the underlying facts, not to trust the previously recorded
                    // transition. See class Javadoc.
                }
            }
        }

        if (status == FilingStatus.DRAFT) {
            return status; // never submitted, nothing to decide
        }
        if (hadValidity) {
            return FilingStatus.REJECTED_VALIDITY;
        }
        if (qualityFailureIds.isEmpty()) {
            return FilingStatus.ACCEPTED;
        }
        boolean allExplained = explainedFailureIds.containsAll(qualityFailureIds);
        return allExplained ? FilingStatus.ACCEPTED : FilingStatus.HELD_FOR_EXPLANATION;
    }
}
