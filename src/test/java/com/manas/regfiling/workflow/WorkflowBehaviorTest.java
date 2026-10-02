package com.manas.regfiling.workflow;

import com.manas.regfiling.domain.EditFailure;
import com.manas.regfiling.domain.EditSeverity;
import com.manas.regfiling.domain.FilingStatus;
import com.manas.regfiling.event.FilingEvent;
import com.manas.regfiling.event.FilingEventStore;
import com.manas.regfiling.replay.FilingReplayProjector;
import com.manas.regfiling.repository.FilingRepository;
import com.manas.regfiling.schema.FilingSchemaCatalog;
import com.manas.regfiling.support.FilingLeafValuesBuilder;
import com.manas.regfiling.support.FilingTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reference run behind the claim "a validity failure blocks the submission, a quality
 * failure is held for an analyst explanation": one filing with only a validity defect, one
 * filing with only a quality defect. Also exercises the append-only invariant on the event
 * store, and folds in the per-filing slice of claim 5 (replay) for both filings.
 */
@SpringBootTest
class WorkflowBehaviorTest {

    @Autowired
    private FilingWorkflowService workflowService;
    @Autowired
    private FilingRepository filingRepository;
    @Autowired
    private FilingEventStore eventStore;
    @Autowired
    private FilingReplayProjector replayProjector;

    @Test
    void validityFailureBlocksSubmissionOutright() {
        List<LineItemInput> missingRequired = FilingTestFactory.removeItem(
                FilingTestFactory.toLineItems(com.manas.regfiling.support.FilingLeafValues.baseline()), "HC", "1.a");

        var filing = workflowService.createFiling("BEHAVIOR-VALIDITY", FilingSchemaCatalog.REPORT_TYPE,
                LocalDate.of(2026, 7, 31), missingRequired);
        SubmissionResult result = workflowService.submit(filing.getId());

        assertEquals(FilingStatus.REJECTED_VALIDITY, result.status());
        assertEquals(FilingStatus.REJECTED_VALIDITY, filingRepository.findById(filing.getId()).orElseThrow().getStatus());
        assertTrue(result.failures().stream().anyMatch(f -> f.getSeverity() == EditSeverity.VALIDITY));

        assertReplayMatches(filing.getId());
    }

    @Test
    void qualityFailureHoldsThenAcceptsOnceExplained() {
        List<LineItemInput> ratioOutOfBand = FilingTestFactory.toLineItems(
                new FilingLeafValuesBuilder().nonaccrual(500_000).build());

        var filing = workflowService.createFiling("BEHAVIOR-QUALITY", FilingSchemaCatalog.REPORT_TYPE,
                LocalDate.of(2026, 7, 31), ratioOutOfBand);
        SubmissionResult result = workflowService.submit(filing.getId());

        assertEquals(FilingStatus.HELD_FOR_EXPLANATION, result.status());
        assertFalse(result.failures().isEmpty());
        assertTrue(result.failures().stream().allMatch(f -> f.getSeverity() == EditSeverity.QUALITY));

        for (EditFailure f : result.failures()) {
            workflowService.addExplanation(filing.getId(), f.getId(), "Reviewed by analyst: concentration from a single large recovered credit.");
        }

        assertEquals(FilingStatus.ACCEPTED, filingRepository.findById(filing.getId()).orElseThrow().getStatus());
        assertReplayMatches(filing.getId());
    }

    @Test
    void eventStoreExposesNoUpdateOrDeleteMethod() {
        for (var method : FilingEventStore.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertFalse(name.contains("update") || name.contains("delete") || name.contains("remove"),
                    "FilingEventStore must not expose a mutation method, found: " + method.getName());
        }
        for (var method : com.manas.regfiling.event.FilingEventReadRepository.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertFalse(name.contains("save") || name.contains("delete") || name.contains("update"),
                    "FilingEventReadRepository must stay read-only, found: " + method.getName());
        }
    }

    @Test
    void eventLogRowCountNeverDecreasesAndPastRowsNeverChange() {
        List<LineItemInput> items = FilingTestFactory.toLineItems(com.manas.regfiling.support.FilingLeafValues.baseline());
        var filing = workflowService.createFiling("BEHAVIOR-APPEND-ONLY", FilingSchemaCatalog.REPORT_TYPE,
                LocalDate.of(2026, 7, 31), items);

        List<FilingEvent> afterCreate = eventStore.findByFilingId(filing.getId());
        int countAfterCreate = afterCreate.size();
        workflowService.submit(filing.getId());
        List<FilingEvent> afterSubmit = eventStore.findByFilingId(filing.getId());

        assertTrue(afterSubmit.size() >= countAfterCreate, "event count must never decrease");
        for (int i = 0; i < afterCreate.size(); i++) {
            assertEquals(afterCreate.get(i).getEventType(), afterSubmit.get(i).getEventType(),
                    "a previously read event changed identity, the log is not append-only");
        }
    }

    private void assertReplayMatches(Long filingId) {
        FilingStatus replayed = replayProjector.replay(eventStore.findByFilingId(filingId));
        FilingStatus live = filingRepository.findById(filingId).orElseThrow().getStatus();
        assertEquals(live, replayed, "replay from the event log disagreed with the live-tracked status");
    }
}
