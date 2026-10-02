package com.manas.regfiling.workflow;

import com.manas.regfiling.domain.EditFailure;
import com.manas.regfiling.domain.FilingStatus;
import com.manas.regfiling.event.FilingEventStore;
import com.manas.regfiling.replay.FilingReplayProjector;
import com.manas.regfiling.repository.FilingRepository;
import com.manas.regfiling.schema.FilingSchemaCatalog;
import com.manas.regfiling.support.SeededDefect;
import com.manas.regfiling.support.SeededDefectCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reference run behind two claims: "40 of 40 seeded defects caught" and "each failure names
 * the exact schedule and line item". Also folds in the per-filing slice of "every decision
 * reproduced on replay from an append-only log" (claim 5) for these 40 filings, since the data
 * is already in hand here.
 */
@SpringBootTest
class SeededDefectCatalogTest {

    @Autowired
    private FilingWorkflowService workflowService;
    @Autowired
    private FilingRepository filingRepository;
    @Autowired
    private FilingEventStore eventStore;
    @Autowired
    private FilingReplayProjector replayProjector;

    @Test
    void allFortySeededDefectsAreCaughtWithExactLocationAndReplayMatches() throws IOException {
        List<SeededDefect> defects = SeededDefectCatalog.all();
        assertEquals(40, defects.size(), "fixture bug: expected exactly 40 seeded defects");

        List<String> report = new ArrayList<>();
        int caught = 0;
        int namedCorrectly = 0;
        int replayMatched = 0;

        for (int i = 0; i < defects.size(); i++) {
            SeededDefect defect = defects.get(i);
            String institutionId = "SEED-" + i;

            List<LocalDate> priorDates = new ArrayList<>();
            LocalDate currentDate = LocalDate.of(2026, 7, 31);
            if (defect.priorPeriodsNewestFirst().size() == 1) {
                priorDates.add(LocalDate.of(2026, 4, 30));
            } else if (defect.priorPeriodsNewestFirst().size() == 2) {
                priorDates.add(LocalDate.of(2026, 4, 30));
                priorDates.add(LocalDate.of(2026, 1, 31));
            }
            // priorPeriodsNewestFirst is newest-first; file them oldest-first so each becomes a
            // real prior period in the database by the time the current filing is submitted.
            for (int p = priorDates.size() - 1; p >= 0; p--) {
                var prior = workflowService.createFiling(institutionId, FilingSchemaCatalog.REPORT_TYPE,
                        priorDates.get(p), defect.priorPeriodsNewestFirst().get(p));
                workflowService.submit(prior.getId());
            }

            var filing = workflowService.createFiling(institutionId, FilingSchemaCatalog.REPORT_TYPE,
                    currentDate, defect.currentItems());
            SubmissionResult result = workflowService.submit(filing.getId());

            boolean found = false;
            for (EditFailure f : result.failures()) {
                boolean locationNamed = f.getScheduleCode() != null && !f.getScheduleCode().isBlank()
                        && f.getLineItemCode() != null && !f.getLineItemCode().isBlank();
                if (locationNamed) {
                    namedCorrectly++;
                }
                if (f.getRuleId().equals(defect.expectedRule().name())
                        && f.getScheduleCode().equals(defect.expectedSchedule())
                        && f.getLineItemCode().equals(defect.expectedLine())) {
                    found = true;
                }
            }
            if (found) {
                caught++;
            } else {
                report.add(defect.label() + " -> NOT CAUGHT (expected " + defect.expectedRule()
                        + " at " + defect.expectedSchedule() + ":" + defect.expectedLine() + ")");
            }

            FilingStatus replayed = replayProjector.replay(eventStore.findByFilingId(filing.getId()));
            FilingStatus live = filingRepository.findById(filing.getId()).orElseThrow().getStatus();
            if (replayed == live) {
                replayMatched++;
            } else {
                report.add(defect.label() + " -> REPLAY MISMATCH live=" + live + " replayed=" + replayed);
            }
        }

        report.add("");
        report.add("Seeded defects caught with correct location: " + caught + " of " + defects.size());
        report.add("Edit failures naming a non-blank schedule and line item: " + namedCorrectly);
        report.add("Replay matched live status: " + replayMatched + " of " + defects.size());

        Files.createDirectories(Path.of("docs"));
        Files.write(Path.of("docs", "seeded_defect_catalog_output.txt"), report, StandardCharsets.UTF_8);

        assertEquals(40, caught, "see docs/seeded_defect_catalog_output.txt for which defects were not caught");
        assertEquals(40, replayMatched, "see docs/seeded_defect_catalog_output.txt for replay mismatches");
    }
}
